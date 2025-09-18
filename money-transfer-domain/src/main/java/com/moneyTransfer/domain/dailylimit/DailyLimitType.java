package com.moneyTransfer.domain.dailylimit;

import java.math.BigDecimal;

public enum DailyLimitType {
    WITHDRAW(new BigDecimal("1000000")), // 출금 일일 한도: 100만원
    TRANSFER(new BigDecimal("3000000"));  // 이체 일일 한도: 300만원

    private final BigDecimal limit;

    DailyLimitType(BigDecimal limit) {
        this.limit = limit;
    }

    public BigDecimal getLimit() {
        return limit;
    }
}