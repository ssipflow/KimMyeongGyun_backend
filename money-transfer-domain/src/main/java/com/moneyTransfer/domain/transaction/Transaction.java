package com.moneyTransfer.domain.transaction;

import com.moneyTransfer.common.constant.ErrorMessages;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
public class Transaction {
    private Long id;
    private TransactionType type;
    private Long accountId;
    private String accountToNo;
    private BigDecimal amount;
    private BigDecimal fee = BigDecimal.ZERO;
    private String description;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

    public static Transaction createDeposit(Long accountId, BigDecimal amount, String description) {
        validateBasicData(amount);
        if (accountId == null) {
            throw new IllegalArgumentException("입금 계좌번호는 필수입니다");
        }

        Transaction transaction = new Transaction();
        transaction.type = TransactionType.DEPOSIT;
        transaction.accountId = accountId;
        transaction.amount = amount;
        transaction.fee = BigDecimal.ZERO;
        transaction.description = description != null ? description : "입금";
        transaction.createdAt = LocalDateTime.now();
        transaction.updatedAt = LocalDateTime.now();
        return transaction;
    }

    public static Transaction createWithdraw(Long accountId, BigDecimal amount, String description) {
        validateBasicData(amount);
        if (accountId == null) {
            throw new IllegalArgumentException("출금 계좌번호는 필수입니다");
        }

        Transaction transaction = new Transaction();
        transaction.type = TransactionType.WITHDRAW;
        transaction.accountId = accountId;
        transaction.amount = amount;
        transaction.fee = BigDecimal.ZERO;
        transaction.description = description != null ? description : "출금";
        transaction.createdAt = LocalDateTime.now();
        transaction.updatedAt = LocalDateTime.now();
        return transaction;
    }

    public static Transaction createTransfer(Long accountId, String accountToNo,
                                           BigDecimal amount, BigDecimal fee, String description) {
        validateBasicData(amount);
        if (accountId == null) {
            throw new IllegalArgumentException("출금 계좌번호는 필수입니다");
        }
        if (accountToNo == null || accountToNo.trim().isEmpty()) {
            throw new IllegalArgumentException("수취 계좌번호는 필수입니다");
        }

        Transaction transaction = new Transaction();
        transaction.type = TransactionType.TRANSFER;
        transaction.accountId = accountId;
        transaction.accountToNo = accountToNo;
        transaction.amount = amount;
        transaction.fee = fee != null ? fee : BigDecimal.ZERO;
        transaction.description = description != null ? description : "계좌이체";
        transaction.createdAt = LocalDateTime.now();
        transaction.updatedAt = LocalDateTime.now();
        return transaction;
    }

    private static void validateBasicData(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_AMOUNT);
        }
    }

    public boolean isDeposit() {
        return TransactionType.DEPOSIT.equals(this.type);
    }

    public boolean isWithdraw() {
        return TransactionType.WITHDRAW.equals(this.type);
    }

    public boolean isTransfer() {
        return TransactionType.TRANSFER.equals(this.type);
    }

    public BigDecimal getTotalDeductionAmount() {
        return amount.add(fee);
    }
}