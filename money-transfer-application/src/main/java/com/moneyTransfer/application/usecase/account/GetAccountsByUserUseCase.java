package com.moneyTransfer.application.usecase.account;

import com.moneyTransfer.application.dto.account.AccountResponse;
import com.moneyTransfer.domain.account.AccountPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetAccountsByUserUseCase {

    private final AccountPort accountPort;

    public GetAccountsByUserUseCase(AccountPort accountPort) {
        this.accountPort = accountPort;
    }

    public List<AccountResponse> execute(Long userId) {
        return accountPort.findByUserId(userId)
            .stream()
            .map(AccountResponse::new)
            .collect(Collectors.toList());
    }
}