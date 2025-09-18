package com.moneyTransfer.domain.dailylimit;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyLimitPort {
    DailyLimit save(DailyLimit dailyLimit);

    Optional<DailyLimit> findByAccountIdAndLimitDate(Long accountId, LocalDate limitDate);

    Optional<DailyLimit> findByAccountIdAndLimitDateWithLock(Long accountId, LocalDate limitDate);
}