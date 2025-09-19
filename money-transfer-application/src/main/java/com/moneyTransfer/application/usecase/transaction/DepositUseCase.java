package com.moneyTransfer.application.usecase.transaction;

import com.moneyTransfer.application.dto.transaction.DepositRequest;
import com.moneyTransfer.application.dto.transaction.TransactionResponse;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
import com.moneyTransfer.domain.transaction.Transaction;
import com.moneyTransfer.domain.transaction.TransactionPort;
import com.moneyTransfer.domain.transaction.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class DepositUseCase {

    private final AccountPort accountPort;
    private final TransactionPort transactionPort;

    public TransactionResponse execute(DepositRequest request) {
        Account account = accountPort.findByIdWithLock(request.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));

        account.deposit(request.getAmount());
        accountPort.save(account);

        Transaction transaction = Transaction.createDeposit(
                account.getId(),
                request.getAmount(),
                request.getDescription()
        );
        transaction.setBalanceAfter(account.getBalance());

        Transaction savedTransaction = transactionPort.save(transaction);

        return new TransactionResponse(
                savedTransaction.getId(),
                savedTransaction.getAccountId(),
                savedTransaction.getAccountToId(),
                savedTransaction.getTransactionType(),
                savedTransaction.getAmount(),
                savedTransaction.getBalanceAfter(),
                savedTransaction.getDescription(),
                savedTransaction.getCreatedAt(),
                savedTransaction.getFee()
        );
    }
}