package com.booking.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmAvailabilityRequest {
    private String requestId;
    private Long bookingId;
    private LocalDate startDate;
    private LocalDate endDate;
}
