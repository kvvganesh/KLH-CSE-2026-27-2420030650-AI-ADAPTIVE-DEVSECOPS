package com.example.devsecops.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
