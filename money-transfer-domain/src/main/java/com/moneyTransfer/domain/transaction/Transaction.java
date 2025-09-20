package com.moneyTransfer.domain.transaction;

import com.moneyTransfer.common.constant.BusinessConstants;
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
    private Long relatedAccountId;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private BigDecimal fee = BusinessConstants.ZERO_AMOUNT;
    private String description;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

    public static Transaction createDeposit(Long accountId, BigDecimal amount, String description) {
        validateBasicData(amount);
        if (accountId == null) {
            throw new IllegalArgumentException(ErrorMessages.DEPOSIT_ACCOUNT_ID_REQUIRED);
        }

        Transaction transaction = new Transaction();
        transaction.transactionType = TransactionType.DEPOSIT;
        transaction.accountId = accountId;  // 입금 계좌가 주체
        transaction.amount = amount;
        transaction.fee = BusinessConstants.ZERO_AMOUNT;
        transaction.description = description != null ? description : BusinessConstants.DEFAULT_DEPOSIT_DESCRIPTION;
        transaction.createdAt = LocalDateTime.now();
        transaction.updatedAt = LocalDateTime.now();
        return transaction;
    }

    public static Transaction createWithdraw(Long accountId, BigDecimal amount, String description) {
        validateBasicData(amount);
        if (accountId == null) {
            throw new IllegalArgumentException(ErrorMessages.WITHDRAW_ACCOUNT_ID_REQUIRED);
        }

        Transaction transaction = new Transaction();
        transaction.transactionType = TransactionType.WITHDRAW;
        transaction.accountId = accountId;  // 출금 계좌가 주체
        transaction.amount = amount;
        transaction.fee = BusinessConstants.ZERO_AMOUNT;
        transaction.description = description != null ? description : BusinessConstants.DEFAULT_WITHDRAW_DESCRIPTION;
        transaction.createdAt = LocalDateTime.now();
        transaction.updatedAt = LocalDateTime.now();
        return transaction;
    }

    public static Transaction createTransferSend(Long fromAccountId, Long toAccountId,
                                               BigDecimal amount, BigDecimal fee, String description) {
        validateBasicData(amount);
        if (fromAccountId == null) {
            throw new IllegalArgumentException(ErrorMessages.FROM_ACCOUNT_ID_REQUIRED);
        }
        if (toAccountId == null) {
            throw new IllegalArgumentException(ErrorMessages.TO_ACCOUNT_ID_REQUIRED);
        }

        Transaction transaction = new Transaction();
        transaction.transactionType = TransactionType.TRANSFER_SEND;
        transaction.accountId = fromAccountId; // 출금 계좌가 주체
        transaction.relatedAccountId = toAccountId; // 입금 계좌가 연관
        transaction.amount = amount;
        transaction.fee = fee != null ? fee : BusinessConstants.ZERO_AMOUNT;
        transaction.description = description != null ? description : BusinessConstants.DEFAULT_TRANSFER_SEND_DESCRIPTION;
        transaction.createdAt = LocalDateTime.now();
        transaction.updatedAt = LocalDateTime.now();
        return transaction;
    }

    public static Transaction createTransferReceive(Long toAccountId, Long fromAccountId,
                                                  BigDecimal amount, String description) {
        validateBasicData(amount);
        if (toAccountId == null) {
            throw new IllegalArgumentException(ErrorMessages.TO_ACCOUNT_ID_REQUIRED);
        }
        if (fromAccountId == null) {
            throw new IllegalArgumentException(ErrorMessages.FROM_ACCOUNT_ID_REQUIRED);
        }

        Transaction transaction = new Transaction();
        transaction.transactionType = TransactionType.TRANSFER_RECEIVE;
        transaction.accountId = toAccountId; // 송금 계좌가 주체
        transaction.relatedAccountId = fromAccountId; // 수취 계좌가 연관
        transaction.amount = amount;
        transaction.fee = BusinessConstants.ZERO_AMOUNT;
        transaction.description = description != null ? description : BusinessConstants.DEFAULT_TRANSFER_RECEIVE_DESCRIPTION;
        transaction.createdAt = LocalDateTime.now();
        transaction.updatedAt = LocalDateTime.now();
        return transaction;
    }

    private static void validateBasicData(BigDecimal amount) {
        if (amount == null || amount.compareTo(BusinessConstants.ZERO_AMOUNT) <= 0) {
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