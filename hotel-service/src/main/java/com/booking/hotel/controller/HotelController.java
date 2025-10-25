package com.booking.hotel.controller;

import com.booking.hotel.dto.HotelCreateRequest;
import com.booking.hotel.dto.HotelDTO;
import com.booking.hotel.dto.HotelStatisticsDTO;
import com.booking.hotel.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
@Tag(name = "Hotels", description = "Hotel management operations")
@SecurityRequirement(name = "Bearer Authentication")
public class HotelController {

    private final HotelService hotelService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create hotel", description = "Create a new hotel (ADMIN only)")
    public ResponseEntity<HotelDTO> createHotel(@Valid @RequestBody HotelCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hotelService.createHotel(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Get all hotels", description = "Get list of all hotels")
    public ResponseEntity<List<HotelDTO>> getAllHotels() {
        return ResponseEntity.ok(hotelService.getAllHotels());
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get hotel statistics", description = "Get statistics for all hotels (ADMIN only)")
    public ResponseEntity<List<HotelStatisticsDTO>> getAllHotelStatistics() {
        return ResponseEntity.ok(hotelService.getAllHotelStatistics());
    }

    @GetMapping("/{hotelId}/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get hotel statistics by ID", description = "Get statistics for a specific hotel (ADMIN only)")
    public ResponseEntity<HotelStatisticsDTO> getHotelStatistics(@PathVariable Long hotelId) {
        return ResponseEntity.ok(hotelService.getHotelStatistics(hotelId));
    }
}
