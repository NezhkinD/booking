package com.booking.service.client;

import com.booking.service.exception.BookingException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Feign error occurred: method={}, status={}", methodKey, response.status());

        return switch (response.status()) {
            case 404 -> new BookingException("Room not found or not available");
            case 409 -> new BookingException("Room is already booked for the specified dates");
            case 503 -> new BookingException("Hotel service is temporarily unavailable");
            default -> new BookingException("Error communicating with hotel service");
        };
    }
}
