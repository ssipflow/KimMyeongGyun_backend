package com.moneyTransfer.domain.account;

import com.moneyTransfer.common.constant.BusinessConstants;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.common.util.StringNormalizer;
import com.moneyTransfer.common.util.ValidationUtils;
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
        this.balance = BusinessConstants.ZERO_AMOUNT;
        this.status = AccountStatus.ACTIVATE;
        this.version = 0;
        this.createdAt = LocalDateTime.now();
    }

    public static Account create(Long userId, String bankCode, String accountNo) {
        validateAccountData(userId, bankCode, accountNo);

        String accountNoNorm = StringNormalizer.normalizeAccountNo(accountNo);
        return new Account(userId, bankCode, accountNo, accountNoNorm);
    }

    private static void validateAccountData(Long userId, String bankCode, String accountNo) {
        if (userId == null) {
            throw new IllegalArgumentException(ErrorMessages.USER_ID_REQUIRED);
        }
        if (!ValidationUtils.isNotBlank(bankCode)) {
            throw new IllegalArgumentException(ErrorMessages.BANK_CODE_REQUIRED);
        }
        if (!ValidationUtils.isNotBlank(accountNo)) {
            throw new IllegalArgumentException(ErrorMessages.ACCOUNT_NO_REQUIRED);
        }
        if (!ValidationUtils.isValidAccountNo(accountNo)) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_ACCOUNT_NO_FORMAT);
        }
    }


    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BusinessConstants.ZERO_AMOUNT) <= 0) {
            throw new IllegalArgumentException(ErrorMessages.DEPOSIT_AMOUNT_INVALID);
        }
        if (!isActive()) {
            throw new IllegalStateException(ErrorMessages.INACTIVE_ACCOUNT_DEPOSIT);
        }
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BusinessConstants.ZERO_AMOUNT) <= 0) {
            throw new IllegalArgumentException(ErrorMessages.WITHDRAW_AMOUNT_INVALID);
        }
        if (!isActive()) {
            throw new IllegalStateException(ErrorMessages.INACTIVE_ACCOUNT_WITHDRAW);
        }
        if (!canWithdraw(amount)) {
            throw new IllegalArgumentException(ErrorMessages.INSUFFICIENT_BALANCE);
        }
        this.balance = this.balance.subtract(amount);
    }

    public boolean canWithdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BusinessConstants.ZERO_AMOUNT) <= 0) {
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