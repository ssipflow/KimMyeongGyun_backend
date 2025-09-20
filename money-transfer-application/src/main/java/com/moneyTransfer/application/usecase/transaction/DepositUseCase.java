package com.moneyTransfer.application.usecase.transaction;

import com.moneyTransfer.application.dto.transaction.DepositRequest;
import com.moneyTransfer.application.dto.transaction.TransactionResponse;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.common.util.StringNormalizer;
import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
import com.moneyTransfer.domain.transaction.Transaction;
import com.moneyTransfer.domain.transaction.TransactionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DepositUseCase {

    private final AccountPort accountPort;
    private final TransactionPort transactionPort;

    public TransactionResponse execute(DepositRequest request) {
        // 1. bankCode + accountNo → Account 조회
        String accountNoNorm = StringNormalizer.normalizeAccountNo(request.getAccountNo());
        Account account = accountPort.findByBankCodeAndAccountNoNorm(request.getBankCode(), accountNoNorm)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));

        // 2. 비관적 락으로 계좌 다시 조회
        account = accountPort.findByIdWithLock(account.getId())
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
                savedTransaction.getRelatedAccountId(),
                savedTransaction.getTransactionType(),
                savedTransaction.getAmount(),
                savedTransaction.getBalanceAfter(),
                savedTransaction.getDescription(),
                savedTransaction.getCreatedAt(),
                savedTransaction.getFee()
        );
    }
}