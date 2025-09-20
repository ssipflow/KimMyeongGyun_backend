package com.moneyTransfer.application.dto.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class DepositRequest {

    @NotBlank
    private final String bankCode;

    @NotBlank
    private final String accountNo;

    @NotNull
    @Positive
    private final BigDecimal amount;

    private final String description;
}