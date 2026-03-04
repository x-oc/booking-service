package ru.vova.airbnb.controller;

import io.swagger.v3.oas.annotations.Operation;
import ru.vova.airbnb.controller.dto.BookingRequest;
import ru.vova.airbnb.controller.dto.BookingResponse;
import ru.vova.airbnb.controller.dto.StatusUpdateRequest;
import ru.vova.airbnb.entity.BookingStatus;
import ru.vova.airbnb.security.jwt.UserDetailsImpl;
import ru.vova.airbnb.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            tags = {"Guest"},
            summary = "Create booking request",
            description = "Role: GUEST. Creates a new booking request."
    )
    public BookingResponse createBooking(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return bookingService.createBooking(request, currentUser.getId());
    }

    @PutMapping("/{id}")
    @Operation(
            tags = {"Guest"},
            summary = "Update booking request",
            description = "Role: GUEST. Updates own booking before payment."
    )
    public BookingResponse updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return bookingService.updateBooking(id, request, currentUser.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            tags = {"Guest"},
            summary = "Delete booking request",
            description = "Role: GUEST. Deletes own booking before payment."
    )
    public void deleteBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        bookingService.deleteBooking(id, currentUser.getId());
    }

    @PostMapping("/{id}/pay")
    @Operation(
            tags = {"Guest"},
            summary = "Pay booking",
            description = "Role: GUEST. Pays a confirmed booking."
    )
    public BookingResponse payForBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return bookingService.payForBooking(id, currentUser.getId());
    }

    @PatchMapping("/{id}/confirm")
    @Operation(
            tags = {"Host"},
            summary = "Confirm booking",
            description = "Role: HOST. Confirms request and opens 24h payment window."
    )
    public BookingResponse confirmBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return bookingService.confirmBooking(id, currentUser.getId());
    }

    @PatchMapping("/{id}/reject")
    @Operation(
            tags = {"Host"},
            summary = "Reject booking",
            description = "Role: HOST. Rejects booking request."
    )
    public BookingResponse rejectBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return bookingService.rejectBooking(id, currentUser.getId());
    }

    @PatchMapping("/{id}/force-status")
    @Operation(
            tags = {"Admin"},
            summary = "Force booking status",
            description = "Role: ADMIN. Forces booking status change."
    )
    public BookingResponse forceChangeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return bookingService.forceChangeStatus(id, request.getStatus());
    }

    @GetMapping("/host")
    @Operation(
            tags = {"Host"},
            summary = "Get host bookings",
            description = "Role: HOST. Returns paginated host bookings with optional filters."
    )
    public Page<BookingResponse> getHostBookings(
            @RequestParam(required = false) Long guestId,
            @RequestParam(required = false) String guestEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return bookingService.getHostBookings(
                guestId,
                guestEmail,
                date,
                status,
                page,
                size,
                sortBy,
                direction,
                currentUser
        );
    }

    @GetMapping("/guest")
    @Operation(
            tags = {"Guest"},
            summary = "Get guest bookings",
            description = "Role: GUEST. Returns paginated guest bookings with optional filters."
    )
    public Page<BookingResponse> getGuestBookings(
            @RequestParam(required = false) Long hostId,
            @RequestParam(required = false) String hostEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return bookingService.getGuestBookings(
                hostId,
                hostEmail,
                date,
                status,
                page,
                size,
                sortBy,
                direction,
                currentUser
        );
    }

    @PostMapping("/{id}/support-request")
    @Operation(
            tags = {"Guest", "Host"},
            summary = "Request admin support for paid booking",
            description = "Roles: GUEST/HOST. Creates support request for paid booking."
    )
    public BookingResponse requestSupport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return bookingService.requestSupportForPaidBooking(id, currentUser);
    }

    @PostMapping("/{id}/support-request/process")
    @Operation(
            tags = {"Admin"},
            summary = "Process support request",
            description = "Role: ADMIN. Cancels booking and refunds 50% or 100% depending on request initiator."
    )
    public BookingResponse processSupportRequest(@PathVariable Long id) {
        return bookingService.processSupportRequest(id);
    }

    @GetMapping("/{id}")
    @Operation(
            tags = {"Guest", "Host", "Admin"},
            summary = "Get booking by id",
            description = "Roles: GUEST/HOST/ADMIN. Admin sees any booking, host/guest only their own."
    )
    public BookingResponse getBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return bookingService.getBookingById(id, currentUser);
    }
}