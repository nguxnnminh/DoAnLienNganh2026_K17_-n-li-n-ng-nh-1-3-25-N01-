package com.shop.clothingstore.exception;

/** Base class for all application-level exceptions. */
public abstract class AppException extends RuntimeException {
    protected AppException(String message) {
        super(message);
    }
    protected AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
