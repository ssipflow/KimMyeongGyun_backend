package com.moneyTransfer.application.usecase.account;

import com.moneyTransfer.common.constant.BusinessConstants;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteAccountUseCase {

    private final AccountPort accountPort;

    public DeleteAccountUseCase(AccountPort accountPort) {
        this.accountPort = accountPort;
    }

    public void execute(Long accountId) {
        try {
            deleteAccountWithConcurrencyControl(accountId);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException(ErrorMessages.OPTIMISTIC_LOCK_CONFLICT);
        }
    }

    private void deleteAccountWithConcurrencyControl(Long accountId) {
        Account account = accountPort.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));

        if (!account.isActive()) {
            throw new IllegalStateException(ErrorMessages.ACCOUNT_ALREADY_DEACTIVATED);
        }

        // 잔액이 있는지 확인
        if (account.getBalance().compareTo(BusinessConstants.ZERO_AMOUNT) > 0) {
            throw new IllegalStateException(ErrorMessages.ACCOUNT_HAS_BALANCE);
        }

        account.deactivate();
        accountPort.save(account);
    }
}