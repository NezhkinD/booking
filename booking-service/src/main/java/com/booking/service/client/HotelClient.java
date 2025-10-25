package com.booking.service.client;

import com.booking.service.dto.ConfirmAvailabilityRequest;
import com.booking.service.dto.ConfirmAvailabilityResponse;
import com.booking.service.dto.ReleaseReservationRequest;
import com.booking.service.dto.RoomDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "hotel-service", configuration = FeignConfig.class, fallback = HotelClientFallback.class)
public interface HotelClient {

    @GetMapping("/api/rooms/recommend")
    List<RoomDTO> getRecommendedRooms(
            @RequestParam Long hotelId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    );

    @PostMapping("/api/rooms/{roomId}/confirm-availability")
    ConfirmAvailabilityResponse confirmAvailability(
            @PathVariable Long roomId,
            @RequestBody ConfirmAvailabilityRequest request
    );

    @PostMapping("/api/rooms/{roomId}/release")
    void releaseReservation(
            @PathVariable Long roomId,
            @RequestBody ReleaseReservationRequest request
    );
}
