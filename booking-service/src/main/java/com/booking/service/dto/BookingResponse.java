package com.booking.service.dto;

import com.booking.service.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Booking.BookingStatus status;
    private LocalDateTime createdAt;
}
