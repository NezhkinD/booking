package com.booking.service.client;

import com.booking.service.dto.ConfirmAvailabilityRequest;
import com.booking.service.dto.ConfirmAvailabilityResponse;
import com.booking.service.dto.ReleaseReservationRequest;
import com.booking.service.dto.RoomDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class HotelClientFallback implements HotelClient {

    @Override
    public List<RoomDTO> getRecommendedRooms(Long hotelId, LocalDate startDate, LocalDate endDate) {
        log.error("Circuit breaker activated for getRecommendedRooms: hotelId={}, dates={} to {}",
                hotelId, startDate, endDate);
        return Collections.emptyList();
    }

    @Override
    public ConfirmAvailabilityResponse confirmAvailability(Long roomId, ConfirmAvailabilityRequest request) {
        log.error("Circuit breaker activated for confirmAvailability: roomId={}, bookingId={}",
                roomId, request.getBookingId());
        return ConfirmAvailabilityResponse.builder()
                .available(false)
                .message("Hotel service is temporarily unavailable. Please try again later.")
                .build();
    }

    @Override
    public void releaseReservation(Long roomId, ReleaseReservationRequest request) {
        log.error("Circuit breaker activated for releaseReservation: roomId={}, bookingId={}",
                roomId, request.getBookingId());
    }
}
