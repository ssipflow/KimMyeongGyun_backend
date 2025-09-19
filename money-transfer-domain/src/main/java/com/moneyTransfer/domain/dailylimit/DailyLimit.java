package com.moneyTransfer.domain.dailylimit;

import com.moneyTransfer.common.constant.BusinessConstants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
public class DailyLimit {
    private Long id;
    private Long accountId;
    private LocalDate limitDate;
    private BigDecimal withdrawUsed = BusinessConstants.ZERO_AMOUNT;
    private BigDecimal transferUsed = BusinessConstants.ZERO_AMOUNT;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

    public static DailyLimit createNew(Long accountId, LocalDate limitDate) {
        if (accountId == null) {
            throw new IllegalArgumentException("계좌 ID는 필수입니다");
        }
        if (limitDate == null) {
            throw new IllegalArgumentException("날짜는 필수입니다");
        }

        DailyLimit dailyLimit = new DailyLimit();
        dailyLimit.accountId = accountId;
        dailyLimit.limitDate = limitDate;
        dailyLimit.withdrawUsed = BusinessConstants.ZERO_AMOUNT;
        dailyLimit.transferUsed = BusinessConstants.ZERO_AMOUNT;
        dailyLimit.createdAt = LocalDateTime.now();
        dailyLimit.updatedAt = LocalDateTime.now();
        return dailyLimit;
    }

    public DailyLimit addWithdrawUsed(BigDecimal amount) {
        if (amount == null || amount.compareTo(BusinessConstants.ZERO_AMOUNT) <= 0) {
            throw new IllegalArgumentException("출금 금액은 0보다 커야 합니다");
        }

        this.withdrawUsed = this.withdrawUsed.add(amount);
        this.updatedAt = LocalDateTime.now();
        return this;
    }

    public DailyLimit addTransferUsed(BigDecimal amount) {
        if (amount == null || amount.compareTo(BusinessConstants.ZERO_AMOUNT) <= 0) {
            throw new IllegalArgumentException("이체 금액은 0보다 커야 합니다");
        }

        this.transferUsed = this.transferUsed.add(amount);
        this.updatedAt = LocalDateTime.now();
        return this;
    }
}