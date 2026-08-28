package com.quince.lawyeraiassistant.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(
            String message) {

        super(message);
    }
}
