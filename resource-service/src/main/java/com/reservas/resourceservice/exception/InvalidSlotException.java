package com.reservas.resourceservice.exception;

public class InvalidSlotException extends RuntimeException {

    public InvalidSlotException(String message) {
        super(message);
    }
}
