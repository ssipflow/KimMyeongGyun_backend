package com.moneyTransfer.application.dto.transaction;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class TransactionHistoryResponse {

    private final List<TransactionResponse> transactions;
    private final PageInfo pageInfo;

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