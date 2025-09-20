package com.moneyTransfer.application.dto.transaction;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class GetTransactionHistoryRequest {

    @NotBlank
    private final String bankCode;

    @NotBlank
    private final String accountNo;

    @NotNull
    @Min(0)
    private final Integer page;

    @NotNull
    @Min(1)
    private final Integer size;

    private final LocalDateTime startDate;

    private final LocalDateTime endDate;
}