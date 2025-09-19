package com.moneyTransfer.application.dto.transaction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class TransferRequest {

    @NotNull
    private final Long fromAccountId;

    @NotNull
    private final Long toAccountId;

    @NotNull
    @Positive
    private final BigDecimal amount;

    private final String description;
}