package com.moneyTransfer.domain.transaction;

import com.moneyTransfer.domain.common.PageResult;
import com.moneyTransfer.domain.common.PageQuery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionPort {
    Transaction save(Transaction transaction);

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    List<Transaction> findByAccountIdAndDateRange(Long accountId, LocalDate startDate, LocalDate endDate);

    PageResult<Transaction> findByAccountIdWithPaging(Long accountId, PageQuery pageQuery);

    PageResult<Transaction> findByAccountIdAndDateRangeWithPaging(Long accountId, LocalDateTime startDate, LocalDateTime endDate, PageQuery pageQuery);
}