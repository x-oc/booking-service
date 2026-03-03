package ru.vova.airbnb.controller;

import io.swagger.v3.oas.annotations.Operation;
import ru.vova.airbnb.controller.dto.BookingRequest;
import ru.vova.airbnb.controller.dto.BookingResponse;
import ru.vova.airbnb.controller.dto.StatusUpdateRequest;
import ru.vova.airbnb.security.jwt.UserDetailsImpl;
import ru.vova.airbnb.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(
            tags = {"Guest"},
            summary = "Create booking request",
            description = "Role: GUEST. Creates a new booking request."
    )
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        BookingResponse response = bookingService.createBooking(request, currentUser.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(
            tags = {"Guest"},
            summary = "Update booking request",
            description = "Role: GUEST. Updates own booking before payment."
    )
    public ResponseEntity<BookingResponse> updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        BookingResponse response = bookingService.updateBooking(id, request, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            tags = {"Guest"},
            summary = "Delete booking request",
            description = "Role: GUEST. Deletes own booking before payment."
    )
    public ResponseEntity<Void> deleteBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        bookingService.deleteBooking(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pay")
    @Operation(
            tags = {"Guest"},
            summary = "Pay booking",
            description = "Role: GUEST. Pays a confirmed booking."
    )
    public ResponseEntity<BookingResponse> payForBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        BookingResponse response = bookingService.payForBooking(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/confirm")
    @Operation(
            tags = {"Host"},
            summary = "Confirm booking",
            description = "Role: HOST. Confirms request and opens 24h payment window."
    )
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        BookingResponse response = bookingService.confirmBooking(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject")
    @Operation(
            tags = {"Host"},
            summary = "Reject booking",
            description = "Role: HOST. Rejects booking request."
    )
    public ResponseEntity<BookingResponse> rejectBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        BookingResponse response = bookingService.rejectBooking(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/force-status")
    @Operation(
            tags = {"Admin"},
            summary = "Force booking status",
            description = "Role: ADMIN. Forces booking status change."
    )
    public ResponseEntity<BookingResponse> forceChangeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {

        BookingResponse response = bookingService.forceChangeStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/host")
    @Operation(
            tags = {"Host"},
            summary = "Get host bookings",
            description = "Role: HOST. Returns bookings where current user is host."
    )
    public ResponseEntity<List<BookingResponse>> getHostBookings(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        List<BookingResponse> bookings = bookingService.getHostBookings(currentUser.getId());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/guest")
    @Operation(
            tags = {"Guest"},
            summary = "Get guest bookings",
            description = "Role: GUEST. Returns paginated bookings where current user is guest."
    )
    public ResponseEntity<Page<BookingResponse>> getGuestBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        Page<BookingResponse> bookings = bookingService.getGuestBookings(
                currentUser.getId(),
                page,
                size,
                sortBy,
                direction
        );
        return ResponseEntity.ok(bookings);
    }

    @PostMapping("/{id}/support-request")
    @Operation(
            tags = {"Guest", "Host"},
            summary = "Request admin support for paid booking",
            description = "Roles: GUEST/HOST. Creates support request for paid booking."
    )
    public ResponseEntity<BookingResponse> requestSupport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        BookingResponse response = bookingService.requestSupportForPaidBooking(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/support-request/process")
    @Operation(
            tags = {"Admin"},
            summary = "Process support request",
            description = "Role: ADMIN. Cancels booking and refunds 50% or 100% depending on request initiator."
    )
    public ResponseEntity<BookingResponse> processSupportRequest(@PathVariable Long id) {
        BookingResponse response = bookingService.processSupportRequest(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            tags = {"Guest", "Host", "Admin"},
            summary = "Get booking by id",
            description = "Roles: GUEST/HOST/ADMIN. Admin sees any booking, host/guest only their own."
    )
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        BookingResponse response = bookingService.getBookingById(id, currentUser);
        return ResponseEntity.ok(response);
    }
}