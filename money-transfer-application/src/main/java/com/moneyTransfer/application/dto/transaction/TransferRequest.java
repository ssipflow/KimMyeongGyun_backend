package com.moneyTransfer.application.dto.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class TransferRequest {

    @NotBlank
    private final String fromBankCode;

    @NotBlank
    private final String fromAccountNo;

    @NotBlank
    private final String toBankCode;

    @NotBlank
    private final String toAccountNo;

    @NotNull
    @Positive
    private final BigDecimal amount;

    private final String description;
}