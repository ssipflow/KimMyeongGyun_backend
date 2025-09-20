package com.moneyTransfer.api.controller;

import com.moneyTransfer.api.dto.request.DepositApiRequest;
import com.moneyTransfer.api.dto.request.TransferApiRequest;
import com.moneyTransfer.api.dto.request.WithdrawApiRequest;
import com.moneyTransfer.api.dto.response.TransactionApiResponse;
import com.moneyTransfer.api.dto.response.TransactionHistoryApiResponse;
import com.moneyTransfer.api.mapper.TransactionDtoMapper;
import com.moneyTransfer.application.dto.transaction.*;
import com.moneyTransfer.application.usecase.transaction.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@Tag(name = "거래", description = "입금, 출금, 이체 및 거래내역 조회 API")
public class TransactionController {

    private final DepositUseCase depositUseCase;
    private final WithdrawUseCase withdrawUseCase;
    private final TransferUseCase transferUseCase;
    private final GetTransactionHistoryUseCase getTransactionHistoryUseCase;
    private final TransactionDtoMapper transactionDtoMapper;

    public TransactionController(
            DepositUseCase depositUseCase,
            WithdrawUseCase withdrawUseCase,
            TransferUseCase transferUseCase,
            GetTransactionHistoryUseCase getTransactionHistoryUseCase,
            TransactionDtoMapper transactionDtoMapper) {
        this.depositUseCase = depositUseCase;
        this.withdrawUseCase = withdrawUseCase;
        this.transferUseCase = transferUseCase;
        this.getTransactionHistoryUseCase = getTransactionHistoryUseCase;
        this.transactionDtoMapper = transactionDtoMapper;
    }

    @PostMapping("/transactions/deposit")
    @Operation(summary = "입금", description = "특정 계좌에 금액을 입금합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "입금 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<TransactionApiResponse> deposit(@Valid @RequestBody DepositApiRequest apiRequest) {
        DepositRequest applicationRequest = transactionDtoMapper.toApplicationRequest(apiRequest);
        TransactionResponse applicationResponse = depositUseCase.execute(applicationRequest);
        TransactionApiResponse apiResponse = transactionDtoMapper.toApiResponse(applicationResponse);

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PostMapping("/transactions/withdraw")
    @Operation(summary = "출금", description = "특정 계좌에서 금액을 출금합니다. 일일 한도 1,000,000원 적용.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "출금 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (잔액 부족, 한도 초과 등)"),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<TransactionApiResponse> withdraw(@Valid @RequestBody WithdrawApiRequest apiRequest) {
        WithdrawRequest applicationRequest = transactionDtoMapper.toApplicationRequest(apiRequest);
        TransactionResponse applicationResponse = withdrawUseCase.execute(applicationRequest);
        TransactionApiResponse apiResponse = transactionDtoMapper.toApiResponse(applicationResponse);

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PostMapping("/transactions/transfer")
    @Operation(summary = "이체", description = "계좌 간 금액을 이체합니다. 이체 금액의 1% 수수료, 일일 한도 3,000,000원 적용.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "이체 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (잔액 부족, 한도 초과, 동일 계좌 이체 등)"),
            @ApiResponse(responseCode = "404", description = "송금 또는 수취 계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<TransactionApiResponse> transfer(@Valid @RequestBody TransferApiRequest apiRequest) {
        TransferRequest applicationRequest = transactionDtoMapper.toApplicationRequest(apiRequest);
        TransactionResponse applicationResponse = transferUseCase.execute(applicationRequest);
        TransactionApiResponse apiResponse = transactionDtoMapper.toApiResponse(applicationResponse);

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @GetMapping("/transactions/account/{bankCode}/{accountNo}")
    @Operation(summary = "거래내역 조회", description = "특정 계좌의 거래내역을 조회합니다. 최신순으로 정렬됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<TransactionHistoryApiResponse> getTransactionHistory(
            @Parameter(description = "은행 코드", required = true, example = "001")
            @PathVariable String bankCode,
            @Parameter(description = "계좌 번호", required = true, example = "123-456-789")
            @PathVariable String accountNo,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "조회 시작 일시", example = "2024-01-01T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @Parameter(description = "조회 종료 일시", example = "2024-12-31T23:59:59")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate) {

        GetTransactionHistoryRequest applicationRequest = transactionDtoMapper.toApplicationRequest(
                bankCode, accountNo, page, size, startDate, endDate);
        TransactionHistoryResponse applicationResponse = getTransactionHistoryUseCase.execute(applicationRequest);
        TransactionHistoryApiResponse apiResponse = transactionDtoMapper.toApiResponse(applicationResponse);

        return ResponseEntity.ok(apiResponse);
    }
}