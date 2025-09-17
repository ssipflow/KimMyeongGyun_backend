package com.moneyTransfer.application.usecase.account;

import com.moneyTransfer.application.dto.account.AccountResponse;
import com.moneyTransfer.common.util.StringNormalizer;
import com.moneyTransfer.domain.account.AccountPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetAccountByBankCodeAndAccountNoUseCase {

    private final AccountPort accountPort;

    public GetAccountByBankCodeAndAccountNoUseCase(AccountPort accountPort) {
        this.accountPort = accountPort;
    }

    public Optional<AccountResponse> execute(String bankCode, String accountNo) {
        String accountNoNorm = StringNormalizer.normalizeAccountNo(accountNo);
        return accountPort.findByBankCodeAndAccountNoNorm(bankCode, accountNoNorm)
            .map(AccountResponse::new);
    }
}