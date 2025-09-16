package com.moneyTransfer.domain.account;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Account {
    private Long id;
    private Long userId;
    private String bankCode;
    private String accountNo;
    private String accountNoNorm;
    private BigDecimal balance;
    private AccountStatus status;
    private LocalDateTime deactivatedAt;
    private Integer version;
    private LocalDateTime createdAt;

    public Account() {}

    public Account(Long userId, String bankCode, String accountNo, String accountNoNorm) {
        this.userId = userId;
        this.bankCode = bankCode;
        this.accountNo = accountNo;
        this.accountNoNorm = accountNoNorm;
        this.balance = BigDecimal.ZERO;
        this.status = AccountStatus.ACTIVATE;
        this.version = 0;
        this.createdAt = LocalDateTime.now();
    }

    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("입금 금액은 0보다 커야 합니다");
        }
        if (!isActive()) {
            throw new IllegalStateException("비활성 계좌에는 입금할 수 없습니다");
        }
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("출금 금액은 0보다 커야 합니다");
        }
        if (!isActive()) {
            throw new IllegalStateException("비활성 계좌에서는 출금할 수 없습니다");
        }
        if (!canWithdraw(amount)) {
            throw new IllegalArgumentException("잔액이 부족합니다");
        }
        this.balance = this.balance.subtract(amount);
    }

    public boolean canWithdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return this.balance.compareTo(amount) >= 0;
    }

    public void deactivate() {
        this.status = AccountStatus.DEACTIVATE;
        this.deactivatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return AccountStatus.ACTIVATE.equals(this.status);
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getBankCode() { return bankCode; }
    public String getAccountNo() { return accountNo; }
    public String getAccountNoNorm() { return accountNoNorm; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public LocalDateTime getDeactivatedAt() { return deactivatedAt; }
    public Integer getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters (for JPA mapping)
    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public void setAccountNoNorm(String accountNoNorm) { this.accountNoNorm = accountNoNorm; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public void setDeactivatedAt(LocalDateTime deactivatedAt) { this.deactivatedAt = deactivatedAt; }
    public void setVersion(Integer version) { this.version = version; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}