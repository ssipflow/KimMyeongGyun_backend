package com.moneyTransfer.domain.account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(Long id);

    Optional<Account> findByIdWithLock(Long id);

    List<Account> findByUserId(Long userId);

    Optional<Account> findByBankCodeAndAccountNoNorm(String bankCode, String accountNoNorm);

    void delete(Account account);

    boolean existsByBankCodeAndAccountNoNorm(String bankCode, String accountNoNorm);
}