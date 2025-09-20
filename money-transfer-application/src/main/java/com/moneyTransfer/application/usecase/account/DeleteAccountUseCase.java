package com.moneyTransfer.application.usecase.account;

import com.moneyTransfer.common.constant.BusinessConstants;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.common.util.StringNormalizer;
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

    public void execute(String bankCode, String accountNo) {
        try {
            deleteAccountWithConcurrencyControl(bankCode, accountNo);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException(ErrorMessages.OPTIMISTIC_LOCK_CONFLICT);
        }
    }

    private void deleteAccountWithConcurrencyControl(String bankCode, String accountNo) {
        // bankCode + accountNo로 계좌 조회
        String accountNoNorm = StringNormalizer.normalizeAccountNo(accountNo);
        Account account = accountPort.findByBankCodeAndAccountNoNorm(bankCode, accountNoNorm)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));

        validateAndDeleteAccount(account);
    }

    private void validateAndDeleteAccount(Account account) {
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