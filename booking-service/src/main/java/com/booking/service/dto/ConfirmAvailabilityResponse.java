package com.booking.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmAvailabilityResponse {
    private boolean available;
    private String message;
    private Long reservationId;
}
