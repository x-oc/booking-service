package ru.vova.airbnb.service;

import ru.vova.airbnb.controller.dto.BookingRequest;
import ru.vova.airbnb.controller.dto.BookingResponse;
import ru.vova.airbnb.entity.Booking;
import ru.vova.airbnb.entity.BookingStatus;
import ru.vova.airbnb.entity.SupportRequestInitiator;
import ru.vova.airbnb.exception.BookingException;
import ru.vova.airbnb.mapper.BookingMapper;
import ru.vova.airbnb.repository.BookingRepository;
import ru.vova.airbnb.security.jwt.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final NotificationService notificationService;
    private final Random random = new Random();

    @Transactional
    @PreAuthorize("hasRole('GUEST')")
    public BookingResponse createBooking(BookingRequest request, Long guestId) {
        log.info("Creating booking for guest: {}", guestId);

        validateBookingDates(request.getCheckInDate(), request.getCheckOutDate());

        if (bookingRepository.existsOverlappingBooking(
                request.getPropertyId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                Arrays.asList(BookingStatus.AWAITING_PAYMENT, BookingStatus.PAID, BookingStatus.ACTIVE))) {
            throw new BookingException("Property is already booked for selected dates");
        }

        Booking booking = bookingMapper.toEntity(request);
        booking.setGuestId(guestId);
        booking.setStatus(BookingStatus.CREATED);
        booking.setRefundedAmount(BigDecimal.ZERO);
        booking.setSupportRequestInitiator(null);
        booking.setSupportRequestedAt(null);

        Booking savedBooking = bookingRepository.save(booking);

        notificationService.notifyHost(savedBooking.getHostId(),
                "New booking request received for property: " + savedBooking.getPropertyId());

        return bookingMapper.toResponse(savedBooking);
    }

    @Transactional
    @PreAuthorize("hasRole('GUEST')")
    public BookingResponse updateBooking(Long bookingId, BookingRequest request, Long guestId) {
        log.info("Guest {} updating booking: {}", guestId, bookingId);
        validateBookingDates(request.getCheckInDate(), request.getCheckOutDate());

        Booking booking = findBookingById(bookingId);
        if (!booking.getGuestId().equals(guestId)) {
            throw new BookingException("You don't have permission to update this booking");
        }
        validateMutableBeforePayment(booking.getStatus());

        if (bookingRepository.existsOverlappingBookingExcludingId(
                bookingId,
                request.getPropertyId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                Arrays.asList(BookingStatus.AWAITING_PAYMENT, BookingStatus.PAID, BookingStatus.ACTIVE))) {
            throw new BookingException("Property is already booked for selected dates");
        }

        booking.setPropertyId(request.getPropertyId());
        booking.setHostId(request.getHostId());
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setTotalAmount(request.getTotalAmount());
        booking.setStatus(BookingStatus.CREATED);
        booking.setPaymentDeadline(null);
        booking.setRefundedAmount(BigDecimal.ZERO);
        booking.setSupportRequestInitiator(null);
        booking.setSupportRequestedAt(null);

        Booking updatedBooking = bookingRepository.save(booking);
        notificationService.notifyHost(updatedBooking.getHostId(),
                "Booking request was updated by guest and requires your review again.");
        return bookingMapper.toResponse(updatedBooking);
    }

    @Transactional
    @PreAuthorize("hasRole('GUEST')")
    public void deleteBooking(Long bookingId, Long guestId) {
        log.info("Guest {} deleting booking: {}", guestId, bookingId);

        Booking booking = findBookingById(bookingId);
        if (!booking.getGuestId().equals(guestId)) {
            throw new BookingException("You don't have permission to delete this booking");
        }
        validateMutableBeforePayment(booking.getStatus());

        bookingRepository.delete(booking);
        notificationService.notifyHost(booking.getHostId(),
                "Booking request was deleted by guest before payment.");
    }

    @Transactional
    @PreAuthorize("hasRole('HOST')")
    public BookingResponse confirmBooking(Long bookingId, Long hostId) {
        log.info("Host {} confirming booking: {}", hostId, bookingId);

        Booking booking = findBookingById(bookingId);

        if (!booking.getHostId().equals(hostId)) {
            throw new BookingException("You don't have permission to confirm this booking");
        }

        validateTransition(booking.getStatus(), BookingStatus.AWAITING_PAYMENT);

        booking.setStatus(BookingStatus.AWAITING_PAYMENT);
        booking.setPaymentDeadline(LocalDateTime.now().plusHours(24));

        Booking updatedBooking = bookingRepository.save(booking);

        notificationService.notifyGuest(updatedBooking.getGuestId(),
                "Your booking has been confirmed by host. Please complete payment within 24 hours.");

        return bookingMapper.toResponse(updatedBooking);
    }

    @Transactional
    @PreAuthorize("hasRole('HOST')")
    public BookingResponse rejectBooking(Long bookingId, Long hostId) {
        log.info("Host {} rejecting booking: {}", hostId, bookingId);

        Booking booking = findBookingById(bookingId);

        if (!booking.getHostId().equals(hostId)) {
            throw new BookingException("You don't have permission to reject this booking");
        }

        validateTransition(booking.getStatus(), BookingStatus.REJECTED);

        booking.setStatus(BookingStatus.REJECTED);
        Booking updatedBooking = bookingRepository.save(booking);

        notificationService.notifyGuest(updatedBooking.getGuestId(),
                "Your booking request has been rejected by host.");

        return bookingMapper.toResponse(updatedBooking);
    }

    @Transactional
    @PreAuthorize("hasRole('GUEST')")
    public BookingResponse payForBooking(Long bookingId, Long guestId) {
        log.info("Guest {} paying for booking: {}", guestId, bookingId);

        Booking booking = findBookingById(bookingId);

        if (!booking.getGuestId().equals(guestId)) {
            throw new BookingException("You don't have permission to pay for this booking");
        }

        validateTransition(booking.getStatus(), BookingStatus.PAID);

        // Check if payment deadline hasn't expired
        if (booking.getPaymentDeadline() != null &&
                LocalDateTime.now().isAfter(booking.getPaymentDeadline())) {
            booking.setStatus(BookingStatus.CANCELLED_EXPIRED);
            bookingRepository.save(booking);
            throw new BookingException("Payment deadline has expired");
        }

        // Simulate payment failure with 20% probability
        if (random.nextDouble() < 0.2) {
            log.warn("Payment failed for booking {} (guest {}). Payment simulation: 20% failure rate triggered.", 
                    bookingId, guestId);
            
            // Cancel booking due to payment failure
            booking.setStatus(BookingStatus.CANCELLED_EXPIRED);
            bookingRepository.save(booking);
            
            notificationService.notifyGuest(booking.getGuestId(),
                    "Payment failed. Your booking has been cancelled. Please create a new booking request.");
            notificationService.notifyHost(booking.getHostId(),
                    "Booking cancelled due to payment failure.");
            
            throw new BookingException("Payment failed. Booking has been cancelled.");
        }

        booking.setStatus(BookingStatus.PAID);
        Booking updatedBooking = bookingRepository.save(booking);

        // Automatically activate if check-in date is today or in the past
        if (!updatedBooking.getCheckInDate().isAfter(LocalDate.now())) {
            activateBooking(updatedBooking);
        }

        notificationService.notifyHost(updatedBooking.getHostId(),
                "Guest has paid for booking. Payment received.");

        return bookingMapper.toResponse(updatedBooking);
    }

    @Transactional
    public void activateBooking(Booking booking) {
        validateTransition(booking.getStatus(), BookingStatus.ACTIVE);
        booking.setStatus(BookingStatus.ACTIVE);
        bookingRepository.save(booking);

        notificationService.notifyGuest(booking.getGuestId(),
                "Your booking is now active. Enjoy your stay!");
        notificationService.notifyHost(booking.getHostId(),
                "Booking has been activated. Guest can now check in.");
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public BookingResponse forceChangeStatus(Long bookingId, BookingStatus newStatus) {
        log.info("Admin forcing status change for booking: {} to {}", bookingId, newStatus);

        Booking booking = findBookingById(bookingId);
        BookingStatus previousStatus = booking.getStatus();
        validateAdminTransition(previousStatus, newStatus);
        booking.setStatus(newStatus);
        if (newStatus != BookingStatus.AWAITING_PAYMENT) {
            booking.setPaymentDeadline(null);
        }

        Booking updatedBooking = bookingRepository.save(booking);
        notificationService.notifyGuest(updatedBooking.getGuestId(),
                String.format("Booking status was changed by admin from %s to %s.",
                        previousStatus, updatedBooking.getStatus()));
        notificationService.notifyHost(updatedBooking.getHostId(),
                String.format("Booking status was changed by admin from %s to %s.",
                        previousStatus, updatedBooking.getStatus()));
        return bookingMapper.toResponse(updatedBooking);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('GUEST', 'HOST')")
    public BookingResponse requestSupportForPaidBooking(Long bookingId, UserDetailsImpl currentUser) {
        Booking booking = findBookingById(bookingId);
        assertCanRequestSupport(booking, currentUser);

        if (booking.getSupportRequestInitiator() != null) {
            throw new BookingException("Support request is already created for this booking");
        }

        SupportRequestInitiator initiator = hasRole(currentUser, "ROLE_GUEST")
                ? SupportRequestInitiator.GUEST
                : SupportRequestInitiator.HOST;

        booking.setSupportRequestInitiator(initiator);
        booking.setSupportRequestedAt(LocalDateTime.now());
        Booking updatedBooking = bookingRepository.save(booking);

        notificationService.notifyAdmin(
                "Support request for booking " + bookingId + " from " + initiator + "."
        );
        return bookingMapper.toResponse(updatedBooking);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public BookingResponse processSupportRequest(Long bookingId) {
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

        notificationService.notifyGuest(updatedBooking.getGuestId(),
                String.format("Booking was cancelled by admin. Refund amount: %s", refundAmount));
        notificationService.notifyHost(updatedBooking.getHostId(),
                String.format("Booking was cancelled by admin after support request from %s.",
                        updatedBooking.getSupportRequestInitiator()));

        return bookingMapper.toResponse(updatedBooking);
    }

    @Transactional
    public void cancelExpiredPayments() {
        log.info("Checking for expired payments");

        List<Booking> expiredBookings = bookingRepository
                .findByStatusAndPaymentDeadlineBefore(
                        BookingStatus.AWAITING_PAYMENT,
                        LocalDateTime.now()
                );

        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.CANCELLED_EXPIRED);
            bookingRepository.save(booking);

            notificationService.notifyGuest(booking.getGuestId(),
                    "Your booking has been cancelled due to payment timeout.");
            notificationService.notifyHost(booking.getHostId(),
                    "Booking cancelled - guest didn't complete payment within 24 hours.");
        }

        if (!expiredBookings.isEmpty()) {
            log.info("Cancelled {} expired bookings", expiredBookings.size());
        }
    }

    @Transactional
    public void completeStays() {
        log.info("Checking for completed stays");

        List<Booking> completedBookings = bookingRepository
                .findByStatusAndCheckOutDateBefore(
                        BookingStatus.ACTIVE,
                        LocalDate.now()
                );

        for (Booking booking : completedBookings) {
            validateTransition(booking.getStatus(), BookingStatus.COMPLETED);
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);

            notificationService.notifyGuest(booking.getGuestId(),
                    "Your stay has been completed. Thank you for choosing us!");
            notificationService.notifyHost(booking.getHostId(),
                    "Guest stay completed. Payment will be processed soon.");
        }

        if (!completedBookings.isEmpty()) {
            log.info("Completed {} stays", completedBookings.size());
        }
    }

    @Transactional
    public void activateDueBookings() {
        log.info("Activating paid bookings with check-in date today or earlier");

        List<Booking> bookingsToActivate = bookingRepository
                .findByStatusAndCheckInDateBeforeOrEqual(
                        BookingStatus.PAID,
                        LocalDate.now()
                );

        for (Booking booking : bookingsToActivate) {
            activateBooking(booking);
        }

        if (!bookingsToActivate.isEmpty()) {
            log.info("Activated {} bookings", bookingsToActivate.size());
        }
    }

    @PreAuthorize("hasAnyRole('HOST', 'ADMIN')")
    public List<BookingResponse> getHostBookings(Long hostId) {
        return bookingRepository.findByHostId(hostId)
                .stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('GUEST', 'ADMIN')")
    public Page<BookingResponse> getGuestBookings(Long guestId, int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new BookingException("Invalid sort direction. Use ASC or DESC.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return bookingRepository.findByGuestId(guestId, pageable)
                .map(bookingMapper::toResponse);
    }

    @PreAuthorize("hasAnyRole('GUEST', 'HOST', 'ADMIN')")
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

        if (checkIn.isBefore(LocalDate.now())) {
            throw new BookingException("Check-in date cannot be in the past");
        }
    }

    private void validateTransition(BookingStatus current, BookingStatus target) {
        boolean isValid = switch (current) {
            case CREATED -> target == BookingStatus.AWAITING_PAYMENT ||
                    target == BookingStatus.REJECTED;
            case AWAITING_PAYMENT -> target == BookingStatus.PAID ||
                    target == BookingStatus.CANCELLED_EXPIRED;
            case PAID -> target == BookingStatus.ACTIVE ||
                    target == BookingStatus.CANCELLED_BY_ADMIN;
            case ACTIVE -> target == BookingStatus.COMPLETED ||
                    target == BookingStatus.CANCELLED_BY_ADMIN;
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
        if (hasRole(currentUser, "ROLE_GUEST") && !booking.getGuestId().equals(currentUser.getId())) {
            throw new BookingException("You don't have permission to request support for this booking");
        }
        if (hasRole(currentUser, "ROLE_HOST") && !booking.getHostId().equals(currentUser.getId())) {
            throw new BookingException("You don't have permission to request support for this booking");
        }
    }

    private boolean isAdmin(UserDetailsImpl user) {
        return hasRole(user, "ROLE_ADMIN");
    }

    private boolean hasRole(UserDetailsImpl user, String role) {
        return user.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}