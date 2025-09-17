package com.moneyTransfer.application.usecase.account;

import com.moneyTransfer.application.dto.account.AccountResponse;
import com.moneyTransfer.domain.account.AccountPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class GetAccountUseCase {

    private final AccountPort accountPort;

    public GetAccountUseCase(AccountPort accountPort) {
        this.accountPort = accountPort;
    }

    public Optional<AccountResponse> execute(Long accountId) {
        return accountPort.findById(accountId)
            .map(AccountResponse::new);
    }
}