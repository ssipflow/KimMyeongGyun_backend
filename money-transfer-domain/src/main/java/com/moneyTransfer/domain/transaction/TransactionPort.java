package com.moneyTransfer.domain.transaction;

import java.time.LocalDate;
import java.util.List;

public interface TransactionPort {
    Transaction save(Transaction transaction);

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    List<Transaction> findByAccountIdAndDateRange(Long accountId, LocalDate startDate, LocalDate endDate);
}