package com.moneyTransfer.domain.transaction;

import com.moneyTransfer.common.constant.ErrorMessages;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
public class Transaction {
    private Long id;
    private TransactionType transactionType;
    private Long accountId;
    private Long accountToId;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
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
        transaction.transactionType = TransactionType.DEPOSIT;
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
        transaction.transactionType = TransactionType.WITHDRAW;
        transaction.accountId = accountId;
        transaction.amount = amount;
        transaction.fee = BigDecimal.ZERO;
        transaction.description = description != null ? description : "출금";
        transaction.createdAt = LocalDateTime.now();
        transaction.updatedAt = LocalDateTime.now();
        return transaction;
    }

    public static Transaction createTransferSend(Long accountId, Long accountToId,
                                               BigDecimal amount, BigDecimal fee, String description) {
        validateBasicData(amount);
        if (accountId == null) {
            throw new IllegalArgumentException("출금 계좌 ID는 필수입니다");
        }
        if (accountToId == null) {
            throw new IllegalArgumentException("입금 계좌 ID는 필수입니다");
        }

        Transaction transaction = new Transaction();
        transaction.transactionType = TransactionType.TRANSFER_SEND;
        transaction.accountId = accountId;
        transaction.accountToId = accountToId;
        transaction.amount = amount;
        transaction.fee = fee != null ? fee : BigDecimal.ZERO;
        transaction.description = description != null ? description : "이체 출금";
        transaction.createdAt = LocalDateTime.now();
        transaction.updatedAt = LocalDateTime.now();
        return transaction;
    }

    public static Transaction createTransferReceive(Long accountId, Long accountFromId,
                                                  BigDecimal amount, String description) {
        validateBasicData(amount);
        if (accountId == null) {
            throw new IllegalArgumentException("입금 계좌 ID는 필수입니다");
        }
        if (accountFromId == null) {
            throw new IllegalArgumentException("출금 계좌 ID는 필수입니다");
        }

        Transaction transaction = new Transaction();
        transaction.transactionType = TransactionType.TRANSFER_RECEIVE;
        transaction.accountId = accountId;
        transaction.accountToId = accountFromId;
        transaction.amount = amount;
        transaction.fee = BigDecimal.ZERO;
        transaction.description = description != null ? description : "이체 입금";
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
        return TransactionType.DEPOSIT.equals(this.transactionType);
    }

    public boolean isWithdraw() {
        return TransactionType.WITHDRAW.equals(this.transactionType);
    }

    public boolean isTransferDeposit() {
        return TransactionType.TRANSFER_SEND.equals(this.transactionType);
    }

    public boolean isTransferWithdraw() {
        return TransactionType.TRANSFER_RECEIVE.equals(this.transactionType);
    }

    public BigDecimal getTotalDeductionAmount() {
        return amount.add(fee);
    }
}