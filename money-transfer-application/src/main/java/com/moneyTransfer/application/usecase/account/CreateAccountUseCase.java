package com.moneyTransfer.application.usecase.account;

import com.moneyTransfer.application.dto.account.AccountResponse;
import com.moneyTransfer.application.dto.account.CreateAccountRequest;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
import com.moneyTransfer.domain.user.User;
import com.moneyTransfer.domain.user.UserPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateAccountUseCase {

    private final AccountPort accountPort;
    private final UserPort userPort;

    public CreateAccountUseCase(AccountPort accountPort, UserPort userPort) {
        this.accountPort = accountPort;
        this.userPort = userPort;
    }

    public AccountResponse execute(CreateAccountRequest request) {
        // 1. User 도메인 객체 생성 (도메인에서 검증)
        User user = User.create(request.getUserName(), request.getEmail(), request.getIdCardNo());

        // 2. 비즈니스 규칙 검증: 중복 체크
        validateUserUniqueness(user);

        // 3. User 저장
        User savedUser = userPort.save(user);

        // 4. Account 도메인 객체 생성 (도메인에서 검증)
        Account account = Account.create(savedUser.getId(), request.getBankCode(), request.getAccountNo());

        // 5. 비즈니스 규칙 검증: 계좌번호 중복 체크
        validateAccountUniqueness(account);

        // 6. Account 저장
        Account savedAccount = accountPort.save(account);

        return new AccountResponse(savedAccount);
    }

    private void validateUserUniqueness(User user) {
        if (userPort.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException(ErrorMessages.DUPLICATE_EMAIL);
        }
        if (userPort.existsByIdCardNoNorm(user.getIdCardNoNorm())) {
            throw new IllegalArgumentException(ErrorMessages.DUPLICATE_ID_CARD);
        }
    }

    private void validateAccountUniqueness(Account account) {
        if (accountPort.existsByBankCodeAndAccountNoNorm(account.getBankCode(), account.getAccountNoNorm())) {
            throw new IllegalArgumentException(ErrorMessages.DUPLICATE_ACCOUNT_NO);
        }
    }
}