package ru.vova.airbnb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.vova.airbnb.controller.dto.BookingRequest;
import ru.vova.airbnb.controller.dto.BookingResponse;
import ru.vova.airbnb.entity.Booking;
import ru.vova.airbnb.entity.BookingStatus;
import ru.vova.airbnb.entity.Property;
import ru.vova.airbnb.entity.SupportRequestInitiator;
import ru.vova.airbnb.entity.User;
import ru.vova.airbnb.events.BookingNotificationEvent;
import ru.vova.airbnb.exception.BookingException;
import ru.vova.airbnb.mapper.BookingMapper;
import ru.vova.airbnb.repository.BookingRepository;
import ru.vova.airbnb.repository.UserRepository;
import ru.vova.airbnb.security.jwt.UserDetailsImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PropertyService propertyService;
    private final BookingMapper bookingMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock moscowClock;

    private final Random random = new Random();

    @PreAuthorize("hasAuthority('BOOKING_CREATE')")
    public BookingResponse createBooking(BookingRequest request, Long guestId) {
        return inTx(() -> {
            log.info("Creating booking for guest: {}", guestId);

            validateBookingDates(request.getCheckInDate(), request.getCheckOutDate());

            Property property = propertyService.lockPropertyForUpdate(request.getPropertyId());
            validatePropertyActive(property);
            validateHostOwnsProperty(property, request.getHostId());

            if (bookingRepository.existsOverlappingBooking(
                    request.getPropertyId(),
                    request.getCheckInDate(),
                    request.getCheckOutDate(),
                    Arrays.asList(BookingStatus.CREATED, BookingStatus.AWAITING_PAYMENT, BookingStatus.PAID, BookingStatus.ACTIVE))) {
                throw new BookingException("Property is already booked for selected dates");
            }

            Booking booking = bookingMapper.toEntity(request);
            booking.setGuestId(guestId);
            booking.setHostId(property.getHostId());
            booking.setStatus(BookingStatus.CREATED);
            booking.setRefundedAmount(BigDecimal.ZERO);
            booking.setSupportRequestInitiator(null);
            booking.setSupportRequestedAt(null);

            Booking savedBooking = bookingRepository.save(booking);
            applicationEventPublisher.publishEvent(
                    BookingNotificationEvent.host(
                            savedBooking.getHostId(),
                            "New booking request received for property: " + savedBooking.getPropertyId()
                    )
            );

            return bookingMapper.toResponse(savedBooking);
        });
    }

    @PreAuthorize("hasAuthority('BOOKING_UPDATE')")
    public BookingResponse updateBooking(Long bookingId, BookingRequest request, Long guestId) {
        return inTx(() -> {
            log.info("Guest {} updating booking: {}", guestId, bookingId);
            validateBookingDates(request.getCheckInDate(), request.getCheckOutDate());

            Booking booking = findBookingById(bookingId);
            if (!booking.getGuestId().equals(guestId)) {
                throw new BookingException("You don't have permission to update this booking");
            }
            validateMutableBeforePayment(booking.getStatus());

            Property lockedTargetProperty = lockPropertiesForUpdate(booking.getPropertyId(), request.getPropertyId())
                    .stream()
                    .filter(property -> property.getId().equals(request.getPropertyId()))
                    .findFirst()
                    .orElseThrow(() -> new BookingException("Property not found with id: " + request.getPropertyId()));
            validatePropertyActive(lockedTargetProperty);
            validateHostOwnsProperty(lockedTargetProperty, request.getHostId());

            if (bookingRepository.existsOverlappingBookingExcludingId(
                    bookingId,
                    request.getPropertyId(),
                    request.getCheckInDate(),
                    request.getCheckOutDate(),
                    Arrays.asList(BookingStatus.CREATED, BookingStatus.AWAITING_PAYMENT, BookingStatus.PAID, BookingStatus.ACTIVE))) {
                throw new BookingException("Property is already booked for selected dates");
            }

            booking.setPropertyId(request.getPropertyId());
            booking.setHostId(lockedTargetProperty.getHostId());
            booking.setCheckInDate(request.getCheckInDate());
            booking.setCheckOutDate(request.getCheckOutDate());
            booking.setTotalAmount(request.getTotalAmount());
            booking.setStatus(BookingStatus.CREATED);
            booking.setPaymentDeadline(null);
            booking.setRefundedAmount(BigDecimal.ZERO);
            booking.setSupportRequestInitiator(null);
            booking.setSupportRequestedAt(null);

            Booking updatedBooking = bookingRepository.save(booking);
            applicationEventPublisher.publishEvent(
                    BookingNotificationEvent.host(
                            updatedBooking.getHostId(),
                            "Booking request was updated by guest and requires your review again."
                    )
            );
            return bookingMapper.toResponse(updatedBooking);
        });
    }

    @PreAuthorize("hasAuthority('BOOKING_DELETE')")
    public void deleteBooking(Long bookingId, Long guestId) {
        inTxVoid(() -> {
            log.info("Guest {} deleting booking: {}", guestId, bookingId);

            Booking booking = findBookingById(bookingId);
            if (!booking.getGuestId().equals(guestId)) {
                throw new BookingException("You don't have permission to delete this booking");
            }
            validateMutableBeforePayment(booking.getStatus());

            bookingRepository.delete(booking);
            applicationEventPublisher.publishEvent(
                    BookingNotificationEvent.host(
                            booking.getHostId(),
                            "Booking request was deleted by guest before payment."
                    )
            );
        });
    }

    @PreAuthorize("hasAuthority('BOOKING_CONFIRM')")
    public BookingResponse confirmBooking(Long bookingId, Long hostId) {
        return inTx(() -> {
            log.info("Host {} confirming booking: {}", hostId, bookingId);

            Booking booking = findBookingById(bookingId);
            propertyService.lockPropertyForUpdate(booking.getPropertyId());

            if (!booking.getHostId().equals(hostId)) {
                throw new BookingException("You don't have permission to confirm this booking");
            }

            if (bookingRepository.existsOverlappingBookingExcludingId(
                    booking.getId(),
                    booking.getPropertyId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    Arrays.asList(BookingStatus.AWAITING_PAYMENT, BookingStatus.PAID, BookingStatus.ACTIVE))) {
                throw new BookingException("Property is already booked for selected dates");
            }

            validateTransition(booking.getStatus(), BookingStatus.AWAITING_PAYMENT);

            booking.setStatus(BookingStatus.AWAITING_PAYMENT);
            booking.setPaymentDeadline(LocalDateTime.now(moscowClock).plusHours(24));

            Booking updatedBooking = bookingRepository.save(booking);
            applicationEventPublisher.publishEvent(
                    BookingNotificationEvent.guest(
                            updatedBooking.getGuestId(),
                            "Your booking has been confirmed by host. Please complete payment within 24 hours."
                    )
            );

            return bookingMapper.toResponse(updatedBooking);
        });
    }

    @PreAuthorize("hasAuthority('BOOKING_REJECT')")
    public BookingResponse rejectBooking(Long bookingId, Long hostId) {
        return inTx(() -> {
            log.info("Host {} rejecting booking: {}", hostId, bookingId);

            Booking booking = findBookingById(bookingId);

            if (!booking.getHostId().equals(hostId)) {
                throw new BookingException("You don't have permission to reject this booking");
            }

            validateTransition(booking.getStatus(), BookingStatus.REJECTED);

            booking.setStatus(BookingStatus.REJECTED);
            Booking updatedBooking = bookingRepository.save(booking);
            applicationEventPublisher.publishEvent(
                    BookingNotificationEvent.guest(
                            updatedBooking.getGuestId(),
                            "Your booking request has been rejected by host."
                    )
            );

            return bookingMapper.toResponse(updatedBooking);
        });
    }

    @PreAuthorize("hasAuthority('BOOKING_PAY')")
    public BookingResponse payForBooking(Long bookingId, Long guestId) {
        return inTx(() -> {
            log.info("Guest {} paying for booking: {}", guestId, bookingId);

            Booking booking = findBookingById(bookingId);

            if (!booking.getGuestId().equals(guestId)) {
                throw new BookingException("You don't have permission to pay for this booking");
            }

            validateTransition(booking.getStatus(), BookingStatus.PAID);

            if (booking.getPaymentDeadline() != null
                    && LocalDateTime.now(moscowClock).isAfter(booking.getPaymentDeadline())) {
                booking.setStatus(BookingStatus.CANCELLED_EXPIRED);
                bookingRepository.save(booking);
                throw new BookingException("Payment deadline has expired");
            }

            if (random.nextDouble() < 0.2) {
                log.warn("Payment failed for booking {} (guest {}). Payment simulation: 20% failure rate triggered.",
                        bookingId, guestId);

                booking.setStatus(BookingStatus.CANCELLED_EXPIRED);
                bookingRepository.save(booking);
                applicationEventPublisher.publishEvent(
                        BookingNotificationEvent.guest(
                                booking.getGuestId(),
                                "Payment failed. Your booking has been cancelled. Please create a new booking request."
                        )
                );
                applicationEventPublisher.publishEvent(
                        BookingNotificationEvent.host(
                                booking.getHostId(),
                                "Booking cancelled due to payment failure."
                        )
                );
                throw new BookingException("Payment failed. Booking has been cancelled.");
            }

            booking.setStatus(BookingStatus.PAID);
            Booking updatedBooking = bookingRepository.save(booking);

            if (!updatedBooking.getCheckInDate().isAfter(LocalDate.now(moscowClock))) {
                activateBooking(updatedBooking);
            }
            applicationEventPublisher.publishEvent(
                    BookingNotificationEvent.host(
                            updatedBooking.getHostId(),
                            "Guest has paid for booking. Payment received."
                    )
            );

            return bookingMapper.toResponse(updatedBooking);
        });
    }

    @PreAuthorize("hasAuthority('BOOKING_FORCE_STATUS')")
    public BookingResponse forceChangeStatus(Long bookingId, BookingStatus newStatus) {
        return inTx(() -> {
            log.info("Admin forcing status change for booking: {} to {}", bookingId, newStatus);

            Booking booking = findBookingById(bookingId);
            BookingStatus previousStatus = booking.getStatus();
            validateAdminTransition(previousStatus, newStatus);
            booking.setStatus(newStatus);
            if (newStatus != BookingStatus.AWAITING_PAYMENT) {
                booking.setPaymentDeadline(null);
            }

            Booking updatedBooking = bookingRepository.save(booking);
            applicationEventPublisher.publishEvent(
                    BookingNotificationEvent.guest(
                            updatedBooking.getGuestId(),
                            String.format("Booking status was changed by admin from %s to %s.",
                                    previousStatus, updatedBooking.getStatus())
                    )
            );
            applicationEventPublisher.publishEvent(
                    BookingNotificationEvent.host(
                            updatedBooking.getHostId(),
                            String.format("Booking status was changed by admin from %s to %s.",
                                    previousStatus, updatedBooking.getStatus())
                    )
            );
            return bookingMapper.toResponse(updatedBooking);
        });
    }

    @PreAuthorize("hasAuthority('BOOKING_SUPPORT_REQUEST')")
    public BookingResponse requestSupportForPaidBooking(Long bookingId, UserDetailsImpl currentUser) {
        return inTx(() -> {
            Booking booking = findBookingById(bookingId);
            assertCanRequestSupport(booking, currentUser);

            if (booking.getSupportRequestInitiator() != null) {
                throw new BookingException("Support request is already created for this booking");
            }

            SupportRequestInitiator initiator = currentUser.getRole().equals("GUEST")
                    ? SupportRequestInitiator.GUEST
                    : SupportRequestInitiator.HOST;

            booking.setSupportRequestInitiator(initiator);
            booking.setSupportRequestedAt(LocalDateTime.now(moscowClock));
            Booking updatedBooking = bookingRepository.save(booking);
            applicationEventPublisher.publishEvent(
                    BookingNotificationEvent.admin(
                            "Support request for booking " + bookingId + " from " + initiator + "."
                    )
            );
            return bookingMapper.toResponse(updatedBooking);
        });
    }

    @PreAuthorize("hasAuthority('BOOKING_SUPPORT_PROCESS')")
    public BookingResponse processSupportRequest(Long bookingId) {
        return inTx(() -> {
            Booking booking = findBookingById(bookingId);

            if (booking.getSupportRequestInitiator() == null) {
                throw new BookingException("Support request for this booking was not found");
            }
            if (booking.getStatus() != BookingStatus.PAID && booking.getStatus() != BookingStatus.ACTIVE) {
                throw new BookingException("Support request can be processed only for paid or active bookings");
            }

            BigDecimal refundRate = booking.getSupportRequestInitiator() == SupportRequestInitiator.GUEST
                    ? new BigDecimal("0.50")
                    : BigDecimal.ONE;
            BigDecimal refundAmount = booking.getTotalAmount()
                    .multiply(refundRate)
                    .setScale(2, RoundingMode.HALF_UP);

            booking.setRefundedAmount(refundAmount);
            booking.setStatus(BookingStatus.CANCELLED_BY_ADMIN);
            booking.setPaymentDeadline(null);
            Booking updatedBooking = bookingRepository.save(booking);
            applicationEventPublisher.publishEvent(
                    BookingNotificationEvent.guest(
                            updatedBooking.getGuestId(),
                            String.format("Booking was cancelled by admin. Refund amount: %s", refundAmount)
                    )
            );
            applicationEventPublisher.publishEvent(
                    BookingNotificationEvent.host(
                            updatedBooking.getHostId(),
                            String.format("Booking was cancelled by admin after support request from %s.",
                                    updatedBooking.getSupportRequestInitiator())
                    )
            );

            return bookingMapper.toResponse(updatedBooking);
        });
    }

    public void cancelExpiredPayments() {
        inTxVoid(() -> {
            log.info("Checking for expired payments");

            List<Booking> expiredBookings = bookingRepository
                    .findByStatusAndPaymentDeadlineBefore(
                            BookingStatus.AWAITING_PAYMENT,
                            LocalDateTime.now(moscowClock)
                    );

            for (Booking booking : expiredBookings) {
                booking.setStatus(BookingStatus.CANCELLED_EXPIRED);
                bookingRepository.save(booking);
                applicationEventPublisher.publishEvent(
                        BookingNotificationEvent.guest(
                                booking.getGuestId(),
                                "Your booking has been cancelled due to payment timeout."
                        )
                );
                applicationEventPublisher.publishEvent(
                        BookingNotificationEvent.host(
                                booking.getHostId(),
                                "Booking cancelled - guest didn't complete payment within 24 hours."
                        )
                );
            }

            if (!expiredBookings.isEmpty()) {
                log.info("Cancelled {} expired bookings", expiredBookings.size());
            }
        });
    }

    public void completeStays() {
        inTxVoid(() -> {
            log.info("Checking for completed stays");

            List<Booking> completedBookings = bookingRepository
                    .findByStatusAndCheckOutDateBefore(
                            BookingStatus.ACTIVE,
                            LocalDate.now(moscowClock)
                    );

            for (Booking booking : completedBookings) {
                validateTransition(booking.getStatus(), BookingStatus.COMPLETED);
                booking.setStatus(BookingStatus.COMPLETED);
                bookingRepository.save(booking);
                applicationEventPublisher.publishEvent(
                        BookingNotificationEvent.guest(
                                booking.getGuestId(),
                                "Your stay has been completed. Thank you for choosing us!"
                        )
                );
                applicationEventPublisher.publishEvent(
                        BookingNotificationEvent.host(
                                booking.getHostId(),
                                "Guest stay completed. Payment will be processed soon."
                        )
                );
            }

            if (!completedBookings.isEmpty()) {
                log.info("Completed {} stays", completedBookings.size());
            }
        });
    }

    public void activateDueBookings() {
        inTxVoid(() -> {
            log.info("Activating paid bookings with check-in date today or earlier");

            List<Booking> bookingsToActivate = bookingRepository
                    .findByStatusAndCheckInDateBeforeOrEqual(
                            BookingStatus.PAID,
                            LocalDate.now(moscowClock)
                    );

            for (Booking booking : bookingsToActivate) {
                activateBooking(booking);
            }

            if (!bookingsToActivate.isEmpty()) {
                log.info("Activated {} bookings", bookingsToActivate.size());
            }
        });
    }

    @PreAuthorize("hasAuthority('BOOKING_LIST_HOST')")
    public Page<BookingResponse> getHostBookings(Long guestId,
                                                 String guestEmail,
                                                 LocalDate date,
                                                 BookingStatus status,
                                                 int page,
                                                 int size,
                                                 String sortBy,
                                                 String direction,
                                                 UserDetailsImpl currentUser) {
        Long effectiveHostId = currentUser.getId();

        Long effectiveGuestId = resolveFilterUserId(guestId, guestEmail, "Guest");
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        String statusStr = status != null ? status.name() : null;
        String dateStr = date != null ? date.toString() : null;
        return bookingRepository.findHostBookingsWithFilters(effectiveHostId, effectiveGuestId, dateStr, statusStr, pageable)
                .map(bookingMapper::toResponse);
    }

    @PreAuthorize("hasAuthority('BOOKING_LIST_GUEST')")
    public Page<BookingResponse> getGuestBookings(Long hostId,
                                                  String hostEmail,
                                                  LocalDate date,
                                                  BookingStatus status,
                                                  int page,
                                                  int size,
                                                  String sortBy,
                                                  String direction,
                                                  UserDetailsImpl currentUser) {
        Long effectiveGuestId = currentUser.getId();

        Long effectiveHostId = resolveFilterUserId(hostId, hostEmail, "Host");
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        String statusStr = status != null ? status.name() : null;
        String dateStr = date != null ? date.toString() : null;
        return bookingRepository.findGuestBookingsWithFilters(effectiveGuestId, effectiveHostId, dateStr, statusStr, pageable)
                .map(bookingMapper::toResponse);
    }

    @PreAuthorize("hasAnyAuthority('BOOKING_VIEW_OWN', 'BOOKING_VIEW_ANY')")
    public BookingResponse getBookingById(Long bookingId, UserDetailsImpl currentUser) {
        Booking booking = findBookingById(bookingId);
        if (!isAdmin(currentUser)
                && !booking.getGuestId().equals(currentUser.getId())
                && !booking.getHostId().equals(currentUser.getId())) {
            throw new BookingException("You don't have permission to view this booking");
        }
        return bookingMapper.toResponse(booking);
    }

    private Booking findBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found with id: " + bookingId));
    }

    private void validateBookingDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new BookingException("Check-out date must be after check-in date");
        }

        if (checkIn.isBefore(LocalDate.now(moscowClock))) {
            throw new BookingException("Check-in date cannot be in the past");
        }
    }

    private void validateTransition(BookingStatus current, BookingStatus target) {
        boolean isValid = switch (current) {
            case CREATED -> target == BookingStatus.AWAITING_PAYMENT || target == BookingStatus.REJECTED;
            case AWAITING_PAYMENT -> target == BookingStatus.PAID || target == BookingStatus.CANCELLED_EXPIRED;
            case PAID -> target == BookingStatus.ACTIVE || target == BookingStatus.CANCELLED_BY_ADMIN;
            case ACTIVE -> target == BookingStatus.COMPLETED || target == BookingStatus.CANCELLED_BY_ADMIN;
            case COMPLETED, CANCELLED_EXPIRED, REJECTED, FORCED_COMPLETED, CANCELLED_BY_ADMIN -> false;
        };

        if (!isValid && target != BookingStatus.FORCED_COMPLETED) {
            throw new BookingException(
                    String.format("Invalid status transition from %s to %s", current, target)
            );
        }
    }

    private void validateAdminTransition(BookingStatus current, BookingStatus target) {
        if (target == BookingStatus.FORCED_COMPLETED || target == BookingStatus.CANCELLED_EXPIRED) {
            return;
        }
        validateTransition(current, target);
    }

    private void validateMutableBeforePayment(BookingStatus status) {
        if (status == BookingStatus.PAID
                || status == BookingStatus.ACTIVE
                || status == BookingStatus.COMPLETED
                || status == BookingStatus.FORCED_COMPLETED
                || status == BookingStatus.CANCELLED_BY_ADMIN) {
            throw new BookingException("Booking can be changed only before payment");
        }
    }

    private void assertCanRequestSupport(Booking booking, UserDetailsImpl currentUser) {
        if (booking.getStatus() != BookingStatus.PAID && booking.getStatus() != BookingStatus.ACTIVE) {
            throw new BookingException("Support request is available only for paid bookings");
        }
        if (currentUser.getRole().equals("GUEST") && !booking.getGuestId().equals(currentUser.getId())) {
            throw new BookingException("You don't have permission to request support for this booking");
        }
        if (currentUser.getRole().equals("HOST") && !booking.getHostId().equals(currentUser.getId())) {
            throw new BookingException("You don't have permission to request support for this booking");
        }
    }

    private boolean isAdmin(UserDetailsImpl user) {
        return user.getRole().equals("ADMIN");
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new BookingException("Invalid sort direction. Use ASC or DESC.");
        }
        String dbColumnName = mapFieldToColumn(sortBy);
        return PageRequest.of(page, size, Sort.by(sortDirection, dbColumnName));
    }

    private String mapFieldToColumn(String fieldName) {
        return switch (fieldName) {
            case "createdAt" -> "created_at";
            case "updatedAt" -> "updated_at";
            case "guestId" -> "guest_id";
            case "hostId" -> "host_id";
            case "propertyId" -> "property_id";
            case "status" -> "status";
            case "checkInDate" -> "check_in_date";
            case "checkOutDate" -> "check_out_date";
            case "paymentDeadline" -> "payment_deadline";
            case "totalAmount" -> "total_amount";
            case "refundedAmount" -> "refunded_amount";
            case "supportRequestInitiator" -> "support_request_initiator";
            case "supportRequestedAt" -> "support_requested_at";
            default -> throw new BookingException("Unsupported sortBy field: " + fieldName);
        };
    }

    private Long resolveFilterUserId(Long userId, String email, String roleLabel) {
        if (email == null || email.isBlank()) {
            return userId;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BookingException(roleLabel + " with email not found: " + email));

        if (userId != null && !userId.equals(user.getId())) {
            throw new BookingException(roleLabel + " id and email refer to different users");
        }
        return user.getId();
    }

    private void validatePropertyActive(Property property) {
        if (Boolean.FALSE.equals(property.getActive())) {
            throw new BookingException("Property is inactive and cannot be booked");
        }
    }

    private void validateHostOwnsProperty(Property property, Long hostIdFromRequest) {
        if (!property.getHostId().equals(hostIdFromRequest)) {
            throw new BookingException("Host id in request does not match property owner");
        }
    }

    private List<Property> lockPropertiesForUpdate(Long firstPropertyId, Long secondPropertyId) {
        if (firstPropertyId.equals(secondPropertyId)) {
            return List.of(propertyService.lockPropertyForUpdate(firstPropertyId));
        }
        Long low = Math.min(firstPropertyId, secondPropertyId);
        Long high = Math.max(firstPropertyId, secondPropertyId);
        Property first = propertyService.lockPropertyForUpdate(low);
        Property second = propertyService.lockPropertyForUpdate(high);
        return List.of(first, second);
    }

    private void activateBooking(Booking booking) {
        validateTransition(booking.getStatus(), BookingStatus.ACTIVE);
        booking.setStatus(BookingStatus.ACTIVE);
        bookingRepository.save(booking);
        applicationEventPublisher.publishEvent(
                BookingNotificationEvent.guest(
                        booking.getGuestId(),
                        "Your booking is now active. Enjoy your stay!"
                )
        );
        applicationEventPublisher.publishEvent(
                BookingNotificationEvent.host(
                        booking.getHostId(),
                        "Booking has been activated. Guest can now check in."
                )
        );
    }

    private <T> T inTx(Supplier<T> action) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> action.get()));
    }

    private void inTxVoid(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }
}
