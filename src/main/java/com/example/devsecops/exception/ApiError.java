package com.example.devsecops.exception;

import java.util.Map;

public record ApiError(int status, String error, String message, Map<String, String> fieldErrors) {
}
