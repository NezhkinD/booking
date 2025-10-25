package com.booking.service.controller;

import com.booking.service.dto.BookingRequest;
import com.booking.service.dto.BookingResponse;
import com.booking.service.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management operations")
@SecurityRequirement(name = "Bearer Authentication")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/booking")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Create booking", description = "Create a new room booking with optional auto-select")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request, username));
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Get user bookings", description = "Get all bookings for the authenticated user with optional pagination")
    public ResponseEntity<?> getUserBookings(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        String username = authentication.getName();

        if (page != null && size != null) {
            return ResponseEntity.ok(bookingService.getUserBookingsPaginated(username, page, size));
        }

        return ResponseEntity.ok(bookingService.getUserBookings(username));
    }

    @GetMapping("/booking/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Get booking by ID", description = "Get a specific booking by ID")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(bookingService.getBookingById(id, username));
    }

    @DeleteMapping("/booking/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Cancel booking", description = "Cancel a booking")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        bookingService.cancelBooking(id, username);
        return ResponseEntity.noContent().build();
    }
}
