package ru.vova.airbnb.controller;

import ru.vova.airbnb.controller.dto.BookingRequest;
import ru.vova.airbnb.controller.dto.BookingResponse;
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
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        BookingResponse response = bookingService.createBooking(request, currentUser.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<BookingResponse> payForBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        BookingResponse response = bookingService.payForBooking(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        BookingResponse response = bookingService.confirmBooking(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<BookingResponse> rejectBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        BookingResponse response = bookingService.rejectBooking(id, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/force-status")
    public ResponseEntity<BookingResponse> forceChangeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {

        BookingResponse response = bookingService.forceChangeStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/host")
    public ResponseEntity<List<BookingResponse>> getHostBookings(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        List<BookingResponse> bookings = bookingService.getHostBookings(currentUser.getId());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/guest")
    public ResponseEntity<List<BookingResponse>> getGuestBookings(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        List<BookingResponse> bookings = bookingService.getGuestBookings(currentUser.getId());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id) {
        BookingResponse response = bookingService.getBookingById(id);
        return ResponseEntity.ok(response);
    }
}