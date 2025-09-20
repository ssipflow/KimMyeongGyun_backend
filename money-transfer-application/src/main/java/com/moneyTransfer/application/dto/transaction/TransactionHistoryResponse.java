package com.moneyTransfer.application.dto.transaction;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class TransactionHistoryResponse {

    private final AccountInfo accountInfo;
    private final List<TransactionResponse> transactions;
    private final PageInfo pageInfo;

    @Getter
    @RequiredArgsConstructor
    public static class AccountInfo {
        private final String userName;
        private final String email;
        private final BigDecimal balance;
        private final String bankCode;
        private final String accountNo;
    }

    @Getter
    @RequiredArgsConstructor
    public static class PageInfo {
        private final Integer currentPage;
        private final Integer pageSize;
        private final Long totalElements;
        private final Integer totalPages;
        private final Boolean hasNext;
        private final Boolean hasPrevious;
    }
}