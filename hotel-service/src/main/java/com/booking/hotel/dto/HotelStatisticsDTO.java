package com.booking.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelStatisticsDTO {
    private Long hotelId;
    private String hotelName;
    private Long totalRooms;
    private Long availableRooms;
    private Long unavailableRooms;
    private Double averageTimesBooked;
    private Long totalReservations;
    private Double occupancyRate;
}
