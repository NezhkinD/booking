package com.booking.hotel.exception;

public class DuplicateRoomNumberException extends RuntimeException {

    public DuplicateRoomNumberException(String message) {
        super(message);
    }
}
