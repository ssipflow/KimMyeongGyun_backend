package com.moneyTransfer.domain.account;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
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

    private Account(Long userId, String bankCode, String accountNo, String accountNoNorm) {
        this.userId = userId;
        this.bankCode = bankCode;
        this.accountNo = accountNo;
        this.accountNoNorm = accountNoNorm;
        this.balance = BigDecimal.ZERO;
        this.status = AccountStatus.ACTIVATE;
        this.version = 0;
        this.createdAt = LocalDateTime.now();
    }

    public static Account create(Long userId, String bankCode, String accountNo) {
        validateAccountData(userId, bankCode, accountNo);

        String accountNoNorm = normalizeAccountNo(accountNo);
        return new Account(userId, bankCode, accountNo, accountNoNorm);
    }

    private static void validateAccountData(Long userId, String bankCode, String accountNo) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
        if (bankCode == null || bankCode.trim().isEmpty()) {
            throw new IllegalArgumentException("은행코드는 필수입니다");
        }
        if (accountNo == null || accountNo.trim().isEmpty()) {
            throw new IllegalArgumentException("계좌번호는 필수입니다");
        }
        if (!isValidAccountNo(accountNo)) {
            throw new IllegalArgumentException("유효한 계좌번호 형식이 아닙니다");
        }
    }

    private static boolean isValidAccountNo(String accountNo) {
        String normalized = normalizeAccountNo(accountNo);
        return normalized.matches("^\\d{10,14}$"); // 10-14자리 숫자
    }

    private static String normalizeAccountNo(String accountNo) {
        if (accountNo == null) return null;
        return accountNo.replaceAll("[^0-9]", "");
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
}