package com.moneyTransfer.domain.dailylimit;

import com.moneyTransfer.common.constant.BusinessConstants;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DailyLimit {
    private Long id;
    private Long accountId;
    private LocalDate limitDate;
    private BigDecimal withdrawUsed = BusinessConstants.ZERO_AMOUNT;
    private BigDecimal transferUsed = BusinessConstants.ZERO_AMOUNT;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

    public static DailyLimit create(Long accountId, LocalDate limitDate) {
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

    public void addWithdrawUsage(BigDecimal amount) {
        if (amount == null || amount.compareTo(BusinessConstants.ZERO_AMOUNT) <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다");
        }

        BigDecimal newWithdrawUsed = this.withdrawUsed.add(amount);
        if (newWithdrawUsed.compareTo(BusinessConstants.DAILY_WITHDRAW_LIMIT) > 0) {
            throw new IllegalStateException(
                String.format("일일 출금 한도를 초과합니다. 한도: %s, 사용 예정: %s",
                    BusinessConstants.DAILY_WITHDRAW_LIMIT, newWithdrawUsed)
            );
        }

        this.withdrawUsed = newWithdrawUsed;
        this.updatedAt = LocalDateTime.now();
    }

    public void addTransferUsage(BigDecimal amount) {
        if (amount == null || amount.compareTo(BusinessConstants.ZERO_AMOUNT) <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다");
        }

        BigDecimal newTransferUsed = this.transferUsed.add(amount);
        if (newTransferUsed.compareTo(BusinessConstants.DAILY_TRANSFER_LIMIT) > 0) {
            throw new IllegalStateException(
                String.format("일일 이체 한도를 초과합니다. 한도: %s, 사용 예정: %s",
                    BusinessConstants.DAILY_TRANSFER_LIMIT, newTransferUsed)
            );
        }

        this.transferUsed = newTransferUsed;
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal getRemainingWithdrawAmount() {
        return BusinessConstants.DAILY_WITHDRAW_LIMIT.subtract(this.withdrawUsed);
    }

    public BigDecimal getRemainingTransferAmount() {
        return BusinessConstants.DAILY_TRANSFER_LIMIT.subtract(this.transferUsed);
    }

    public boolean canWithdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BusinessConstants.ZERO_AMOUNT) <= 0) {
            return false;
        }
        return this.withdrawUsed.add(amount).compareTo(BusinessConstants.DAILY_WITHDRAW_LIMIT) <= 0;
    }

    public boolean canTransfer(BigDecimal amount) {
        if (amount == null || amount.compareTo(BusinessConstants.ZERO_AMOUNT) <= 0) {
            return false;
        }
        return this.transferUsed.add(amount).compareTo(BusinessConstants.DAILY_TRANSFER_LIMIT) <= 0;
    }

    public boolean isWithdrawExceeded() {
        return this.withdrawUsed.compareTo(BusinessConstants.DAILY_WITHDRAW_LIMIT) >= 0;
    }

    public boolean isTransferExceeded() {
        return this.transferUsed.compareTo(BusinessConstants.DAILY_TRANSFER_LIMIT) >= 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public LocalDate getLimitDate() { return limitDate; }
    public void setLimitDate(LocalDate limitDate) { this.limitDate = limitDate; }

    public BigDecimal getWithdrawUsed() { return withdrawUsed; }
    public void setWithdrawUsed(BigDecimal withdrawUsed) { this.withdrawUsed = withdrawUsed; }

    public BigDecimal getTransferUsed() { return transferUsed; }
    public void setTransferUsed(BigDecimal transferUsed) { this.transferUsed = transferUsed; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}