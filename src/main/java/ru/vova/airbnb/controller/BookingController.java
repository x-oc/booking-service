package ru.vova.airbnb.controller;

import io.swagger.v3.oas.annotations.Operation;
import ru.vova.airbnb.controller.dto.BookingRequest;
import ru.vova.airbnb.controller.dto.BookingResponse;
import ru.vova.airbnb.controller.dto.BookingStatisticsResponse;
import ru.vova.airbnb.controller.dto.StatusUpdateRequest;
import ru.vova.airbnb.security.jwt.UserDetailsImpl;
import ru.vova.airbnb.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
            description = "Role: GUEST. Returns bookings where current user is guest."
    )
    public ResponseEntity<List<BookingResponse>> getGuestBookings(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        List<BookingResponse> bookings = bookingService.getGuestBookings(currentUser.getId());
        return ResponseEntity.ok(bookings);
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

    @GetMapping("/admin/statistics")
    @Operation(
            tags = {"Admin"},
            summary = "Get booking statistics",
            description = "Role: ADMIN. Returns aggregated statistics by booking status."
    )
    public ResponseEntity<BookingStatisticsResponse> getBookingStatistics() {
        return ResponseEntity.ok(bookingService.getBookingStatistics());
    }
}