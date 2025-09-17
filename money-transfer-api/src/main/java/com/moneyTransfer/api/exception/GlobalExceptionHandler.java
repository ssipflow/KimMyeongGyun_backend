package com.moneyTransfer.api.exception;

import com.moneyTransfer.api.dto.ErrorResponse;
import com.moneyTransfer.common.constant.ErrorMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());

        String message = e.getMessage();
        HttpStatus status;

        // 에러 메시지에 따른 적절한 HTTP 상태 코드 결정
        if (isNotFoundError(message)) {
            status = HttpStatus.NOT_FOUND;
        } else if (isDuplicateError(message)) {
            status = HttpStatus.CONFLICT;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }

        ErrorResponse errorResponse = ErrorResponse.of(status.value(), status.getReasonPhrase(), message);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        log.warn("IllegalStateException: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.badRequest(e.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        log.warn("Validation error: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String message = "입력 데이터 검증 실패: " + errors.toString();
        ErrorResponse errorResponse = ErrorResponse.badRequest(message);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);

        ErrorResponse errorResponse = ErrorResponse.internalServerError("서버 내부 오류가 발생했습니다");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private boolean isNotFoundError(String message) {
        return message.equals(ErrorMessages.USER_NOT_FOUND) ||
               message.equals(ErrorMessages.ACCOUNT_NOT_FOUND);
    }

    private boolean isDuplicateError(String message) {
        return message.equals(ErrorMessages.DUPLICATE_EMAIL) ||
               message.equals(ErrorMessages.DUPLICATE_ID_CARD) ||
               message.equals(ErrorMessages.DUPLICATE_ACCOUNT_NO);
    }
}