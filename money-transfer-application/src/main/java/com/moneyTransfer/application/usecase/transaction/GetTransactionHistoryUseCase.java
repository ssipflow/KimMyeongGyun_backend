package com.moneyTransfer.application.usecase.transaction;

import com.moneyTransfer.application.dto.transaction.TransactionResponse;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.domain.account.AccountPort;
import com.moneyTransfer.domain.transaction.Transaction;
import com.moneyTransfer.domain.transaction.TransactionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTransactionHistoryUseCase {

    private final TransactionPort transactionPort;
    private final AccountPort accountPort;

    public List<TransactionResponse> execute(Long accountId) {
        // 계좌 존재 여부 확인
        accountPort.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));

        List<Transaction> transactions = transactionPort.findByAccountIdOrderByCreatedAtDesc(accountId);

        return transactions.stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getRelatedAccountId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getDescription(),
                transaction.getCreatedAt(),
                transaction.getFee()
        );
    }
}