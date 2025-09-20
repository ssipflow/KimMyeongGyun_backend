package com.moneyTransfer.application.dto.transaction;

import com.moneyTransfer.domain.transaction.TransactionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class TransactionResponse {

    private final Long transactionId;
    private final AccountInfo accountInfo;
    private final AccountInfo relatedAccountInfo;
    private final TransactionType transactionType;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String description;
    private final LocalDateTime createdAt;
    private final BigDecimal fee;

    @Getter
    @RequiredArgsConstructor
    public static class AccountInfo {
        private final String bankCode;
        private final String accountNo;
    }
}