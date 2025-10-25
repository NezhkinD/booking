package com.booking.hotel.controller;

import com.booking.hotel.dto.ConfirmAvailabilityRequest;
import com.booking.hotel.dto.ConfirmAvailabilityResponse;
import com.booking.hotel.dto.ReleaseReservationRequest;
import com.booking.hotel.service.RoomReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Room Reservations", description = "Internal operations for room reservation management")
public class RoomReservationController {

    private final RoomReservationService reservationService;

    @PostMapping("/{roomId}/confirm-availability")
    @Operation(summary = "Confirm room availability", description = "Internal endpoint to confirm and reserve room availability")
    public ResponseEntity<ConfirmAvailabilityResponse> confirmAvailability(
            @PathVariable Long roomId,
            @Valid @RequestBody ConfirmAvailabilityRequest request) {
        return ResponseEntity.ok(reservationService.confirmAvailability(roomId, request));
    }

    @PostMapping("/{roomId}/release")
    @Operation(summary = "Release reservation", description = "Internal endpoint to release a room reservation")
    public ResponseEntity<Void> releaseReservation(
            @PathVariable Long roomId,
            @Valid @RequestBody ReleaseReservationRequest request) {
        reservationService.releaseReservation(roomId, request);
        return ResponseEntity.noContent().build();
    }
}
