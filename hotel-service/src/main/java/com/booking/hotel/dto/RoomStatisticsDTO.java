package com.booking.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomStatisticsDTO {
    private Long roomId;
    private String roomNumber;
    private Long hotelId;
    private String hotelName;
    private Integer timesBooked;
    private Boolean available;
    private Long activeReservations;
}
