package com.moneyTransfer.api.controller;

import com.moneyTransfer.api.dto.request.CreateAccountApiRequest;
import com.moneyTransfer.api.dto.request.DeleteAccountApiRequest;
import com.moneyTransfer.api.dto.response.AccountApiResponse;
import com.moneyTransfer.api.mapper.AccountDtoMapper;
import com.moneyTransfer.application.dto.account.AccountResponse;
import com.moneyTransfer.application.dto.account.CreateAccountRequest;
import com.moneyTransfer.application.usecase.account.*;
import com.moneyTransfer.common.constant.ErrorMessages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/accounts")
@Tag(name = "계좌 관리", description = "계좌 등록 및 삭제 API")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;
    private final AccountDtoMapper accountDtoMapper;

    public AccountController(
            CreateAccountUseCase createAccountUseCase,
            DeleteAccountUseCase deleteAccountUseCase,
            AccountDtoMapper accountDtoMapper) {
        this.createAccountUseCase = createAccountUseCase;
        this.deleteAccountUseCase = deleteAccountUseCase;
        this.accountDtoMapper = accountDtoMapper;
    }

    @PostMapping
    @Operation(summary = "계좌 등록", description = "새로운 계좌를 시스템에 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "계좌 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (중복 계좌, 유효성 검증 실패 등)"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<AccountApiResponse> createAccount(@Valid @RequestBody CreateAccountApiRequest apiRequest) {
        CreateAccountRequest applicationRequest = accountDtoMapper.toApplicationRequest(apiRequest);
        AccountResponse applicationResponse = createAccountUseCase.execute(applicationRequest);
        AccountApiResponse apiResponse = accountDtoMapper.toApiResponse(applicationResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @DeleteMapping
    @Operation(summary = "계좌 삭제", description = "기존 계좌를 시스템에서 삭제합니다. 잔액이 0원인 계좌만 삭제 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "계좌 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (잔액이 있는 계좌, 이미 삭제된 계좌 등)"),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> deleteAccount(@Valid @RequestBody DeleteAccountApiRequest apiRequest) {
        deleteAccountUseCase.execute(apiRequest.getBankCode(), apiRequest.getAccountNo());
        return ResponseEntity.noContent().build();
    }
}