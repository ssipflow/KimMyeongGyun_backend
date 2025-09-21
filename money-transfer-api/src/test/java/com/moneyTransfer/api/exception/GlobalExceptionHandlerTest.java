package com.moneyTransfer.api.exception;

import com.moneyTransfer.api.dto.ErrorResponse;
import com.moneyTransfer.common.constant.ErrorMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @Mock
    private BindingResult bindingResult;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("IllegalArgumentException 처리 - NOT_FOUND 에러")
    void handleIllegalArgumentExceptionNotFound() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException(ErrorMessages.USER_NOT_FOUND);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorMessages.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("IllegalArgumentException 처리 - ACCOUNT_NOT_FOUND 에러")
    void handleIllegalArgumentExceptionAccountNotFound() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorMessages.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("IllegalArgumentException 처리 - CONFLICT 에러")
    void handleIllegalArgumentExceptionConflict() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException(ErrorMessages.DUPLICATE_EMAIL);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorMessages.DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("IllegalArgumentException 처리 - DUPLICATE_ID_CARD 에러")
    void handleIllegalArgumentExceptionDuplicateIdCard() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException(ErrorMessages.DUPLICATE_ID_CARD);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorMessages.DUPLICATE_ID_CARD);
    }

    @Test
    @DisplayName("IllegalArgumentException 처리 - DUPLICATE_ACCOUNT_NO 에러")
    void handleIllegalArgumentExceptionDuplicateAccountNo() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException(ErrorMessages.DUPLICATE_ACCOUNT_NO);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorMessages.DUPLICATE_ACCOUNT_NO);
    }

    @Test
    @DisplayName("IllegalArgumentException 처리 - BAD_REQUEST 에러")
    void handleIllegalArgumentExceptionBadRequest() {
        // given
        String customErrorMessage = "잘못된 요청입니다";
        IllegalArgumentException exception = new IllegalArgumentException(customErrorMessage);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo(customErrorMessage);
    }

    @Test
    @DisplayName("IllegalStateException 처리")
    void handleIllegalStateException() {
        // given
        String errorMessage = "잘못된 상태입니다";
        IllegalStateException exception = new IllegalStateException(errorMessage);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalStateException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("MethodArgumentNotValidException 처리")
    void handleMethodArgumentNotValidException() {
        // given
        FieldError fieldError1 = new FieldError("createAccountRequest", "bankCode", "은행 코드는 필수입니다");
        FieldError fieldError2 = new FieldError("createAccountRequest", "accountNo", "계좌번호는 필수입니다");

        given(methodArgumentNotValidException.getBindingResult()).willReturn(bindingResult);
        given(bindingResult.getAllErrors()).willReturn(List.of(fieldError1, fieldError2));

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationExceptions(methodArgumentNotValidException);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).contains("입력 데이터 검증 실패");
        assertThat(response.getBody().getMessage()).contains("bankCode");
        assertThat(response.getBody().getMessage()).contains("accountNo");
    }

    @Test
    @DisplayName("단일 필드 검증 실패 처리")
    void handleMethodArgumentNotValidExceptionSingleField() {
        // given
        FieldError fieldError = new FieldError("depositRequest", "amount", "금액은 0보다 커야 합니다");

        given(methodArgumentNotValidException.getBindingResult()).willReturn(bindingResult);
        given(bindingResult.getAllErrors()).willReturn(List.of(fieldError));

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationExceptions(methodArgumentNotValidException);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("입력 데이터 검증 실패");
        assertThat(response.getBody().getMessage()).contains("amount");
        assertThat(response.getBody().getMessage()).contains("금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("일반 Exception 처리")
    void handleGenericException() {
        // given
        Exception exception = new RuntimeException("예상치 못한 오류");

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("서버 내부 오류가 발생했습니다");
    }

    @Test
    @DisplayName("NullPointerException 처리")
    void handleNullPointerException() {
        // given
        NullPointerException exception = new NullPointerException("Null pointer error");

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("서버 내부 오류가 발생했습니다");
    }

    @Test
    @DisplayName("빈 에러 메시지 처리")
    void handleExceptionWithEmptyMessage() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("");

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("");
    }

    @Test
    @DisplayName("null 에러 메시지 처리")
    void handleExceptionWithNullMessage() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException((String) null);

        // when & then
        // GlobalExceptionHandler에서 null 메시지로 인한 NPE가 발생할 수 있음을 확인
        assertThatThrownBy(() -> globalExceptionHandler.handleIllegalArgumentException(exception))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("모든 NOT_FOUND 에러 케이스 검증")
    void verifyAllNotFoundErrorCases() {
        // USER_NOT_FOUND
        IllegalArgumentException userNotFound = new IllegalArgumentException(ErrorMessages.USER_NOT_FOUND);
        ResponseEntity<ErrorResponse> userResponse = globalExceptionHandler.handleIllegalArgumentException(userNotFound);
        assertThat(userResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // ACCOUNT_NOT_FOUND
        IllegalArgumentException accountNotFound = new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND);
        ResponseEntity<ErrorResponse> accountResponse = globalExceptionHandler.handleIllegalArgumentException(accountNotFound);
        assertThat(accountResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("모든 CONFLICT 에러 케이스 검증")
    void verifyAllConflictErrorCases() {
        // DUPLICATE_EMAIL
        IllegalArgumentException duplicateEmail = new IllegalArgumentException(ErrorMessages.DUPLICATE_EMAIL);
        ResponseEntity<ErrorResponse> emailResponse = globalExceptionHandler.handleIllegalArgumentException(duplicateEmail);
        assertThat(emailResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // DUPLICATE_ID_CARD
        IllegalArgumentException duplicateIdCard = new IllegalArgumentException(ErrorMessages.DUPLICATE_ID_CARD);
        ResponseEntity<ErrorResponse> idCardResponse = globalExceptionHandler.handleIllegalArgumentException(duplicateIdCard);
        assertThat(idCardResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // DUPLICATE_ACCOUNT_NO
        IllegalArgumentException duplicateAccountNo = new IllegalArgumentException(ErrorMessages.DUPLICATE_ACCOUNT_NO);
        ResponseEntity<ErrorResponse> accountNoResponse = globalExceptionHandler.handleIllegalArgumentException(duplicateAccountNo);
        assertThat(accountNoResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}