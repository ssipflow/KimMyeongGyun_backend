package com.moneyTransfer.api.controller;

import com.moneyTransfer.api.dto.request.CreateAccountApiRequest;
import com.moneyTransfer.api.dto.response.AccountApiResponse;
import com.moneyTransfer.api.mapper.AccountDtoMapper;
import com.moneyTransfer.application.dto.account.AccountResponse;
import com.moneyTransfer.application.dto.account.CreateAccountRequest;
import com.moneyTransfer.application.usecase.account.*;
import com.moneyTransfer.common.constant.ErrorMessages;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;
    private final GetAccountsByUserUseCase getAccountsByUserUseCase;
    private final GetAccountByBankCodeAndAccountNoUseCase getAccountByBankCodeAndAccountNoUseCase;
    private final AccountDtoMapper accountDtoMapper;

    public AccountController(
            CreateAccountUseCase createAccountUseCase,
            DeleteAccountUseCase deleteAccountUseCase,
            GetAccountUseCase getAccountUseCase,
            GetAccountsByUserUseCase getAccountsByUserUseCase,
            GetAccountByBankCodeAndAccountNoUseCase getAccountByBankCodeAndAccountNoUseCase,
            AccountDtoMapper accountDtoMapper) {
        this.createAccountUseCase = createAccountUseCase;
        this.deleteAccountUseCase = deleteAccountUseCase;
        this.getAccountUseCase = getAccountUseCase;
        this.getAccountsByUserUseCase = getAccountsByUserUseCase;
        this.getAccountByBankCodeAndAccountNoUseCase = getAccountByBankCodeAndAccountNoUseCase;
        this.accountDtoMapper = accountDtoMapper;
    }

    @PostMapping
    public ResponseEntity<AccountApiResponse> createAccount(@Valid @RequestBody CreateAccountApiRequest apiRequest) {
        CreateAccountRequest applicationRequest = accountDtoMapper.toApplicationRequest(apiRequest);
        AccountResponse applicationResponse = createAccountUseCase.execute(applicationRequest);
        AccountApiResponse apiResponse = accountDtoMapper.toApiResponse(applicationResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long accountId) {
        deleteAccountUseCase.execute(accountId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountApiResponse> getAccount(@PathVariable Long accountId) {
        Optional<AccountResponse> applicationResponse = getAccountUseCase.execute(accountId);
        return applicationResponse.map(response -> {
            AccountApiResponse apiResponse = accountDtoMapper.toApiResponse(response);
            return ResponseEntity.ok().body(apiResponse);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<AccountApiResponse>> getAccounts(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String bankCode,
            @RequestParam(required = false) String accountNo) {

        // 사용자별 계좌 목록 조회
        if (userId != null) {
            List<AccountResponse> applicationResponses = getAccountsByUserUseCase.execute(userId);
            List<AccountApiResponse> apiResponses = accountDtoMapper.toApiResponseList(applicationResponses);
            return ResponseEntity.ok(apiResponses);
        }

        // 은행코드 + 계좌번호로 계좌 조회
        if (bankCode != null && accountNo != null) {
            Optional<AccountResponse> applicationResponse = getAccountByBankCodeAndAccountNoUseCase.execute(bankCode, accountNo);
            return applicationResponse.map(response -> {
                AccountApiResponse apiResponse = accountDtoMapper.toApiResponse(response);
                return ResponseEntity.ok(List.of(apiResponse));
            }).orElse(ResponseEntity.ok(List.of()));
        }

        throw new IllegalArgumentException(ErrorMessages.MISSING_QUERY_PARAMETERS);
    }
}