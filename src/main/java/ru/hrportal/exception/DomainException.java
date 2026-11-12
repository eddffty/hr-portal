package ru.hrportal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DomainException extends ResponseStatusException {
    public DomainException(HttpStatus status, String message) {
        super(status, message);
    }

    public static void check(boolean condition, String message) {
        if (!condition) throw new DomainException(HttpStatus.CONFLICT, message);
    }
}
