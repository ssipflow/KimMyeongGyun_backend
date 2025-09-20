package com.moneyTransfer.domain.dailylimit;

import com.moneyTransfer.common.constant.BusinessConstants;
import com.moneyTransfer.common.constant.ErrorMessages;
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
            throw new IllegalArgumentException(ErrorMessages.ACCOUNT_ID_REQUIRED);
        }
        if (limitDate == null) {
            throw new IllegalArgumentException(ErrorMessages.LIMIT_DATE_REQUIRED);
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
            throw new IllegalArgumentException(ErrorMessages.WITHDRAW_AMOUNT_MUST_BE_POSITIVE);
        }

        this.withdrawUsed = this.withdrawUsed.add(amount);
        this.updatedAt = LocalDateTime.now();
        return this;
    }

    public DailyLimit addTransferUsed(BigDecimal amount) {
        if (amount == null || amount.compareTo(BusinessConstants.ZERO_AMOUNT) <= 0) {
            throw new IllegalArgumentException(ErrorMessages.TRANSFER_AMOUNT_MUST_BE_POSITIVE);
        }

        this.transferUsed = this.transferUsed.add(amount);
        this.updatedAt = LocalDateTime.now();
        return this;
    }
}