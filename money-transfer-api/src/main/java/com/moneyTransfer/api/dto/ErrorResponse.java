package com.moneyTransfer.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ErrorResponse {
    private final int status;
    private final String error;
    private final String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message);
    }

    public static ErrorResponse badRequest(String message) {
        return new ErrorResponse(400, "Bad Request", message);
    }

    public static ErrorResponse notFound(String message) {
        return new ErrorResponse(404, "Not Found", message);
    }

    public static ErrorResponse conflict(String message) {
        return new ErrorResponse(409, "Conflict", message);
    }

    public static ErrorResponse internalServerError(String message) {
        return new ErrorResponse(500, "Internal Server Error", message);
    }
}