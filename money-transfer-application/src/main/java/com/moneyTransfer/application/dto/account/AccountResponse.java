package com.moneyTransfer.application.dto.account;

import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountResponse {
    private Long id;
    private Long userId;
    private String bankCode;
    private String accountNo;
    private BigDecimal balance;
    private AccountStatus status;
    private LocalDateTime createdAt;

    public AccountResponse() {}

    public AccountResponse(Account account) {
        this.id = account.getId();
        this.userId = account.getUserId();
        this.bankCode = account.getBankCode();
        this.accountNo = account.getAccountNo();
        this.balance = account.getBalance();
        this.status = account.getStatus();
        this.createdAt = account.getCreatedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}