package com.moneyTransfer.application.usecase.account;

import com.moneyTransfer.application.dto.account.AccountResponse;
import com.moneyTransfer.application.dto.account.CreateAccountRequest;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.common.util.StringNormalizer;
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
        // 1. 주민번호로 기존 User 찾기 또는 생성
        User user = findOrCreateUser(request);

        // 2. Account 도메인 객체 생성 (도메인에서 검증)
        Account account = Account.create(user.getId(), request.getBankCode(), request.getAccountNo());

        // 3. 비즈니스 규칙 검증: 계좌번호 중복 체크
        validateAccountUniqueness(account);

        // 4. Account 저장
        Account savedAccount = accountPort.save(account);

        return new AccountResponse(savedAccount);
    }

    private User findOrCreateUser(CreateAccountRequest request) {
        // 정규화된 주민번호로 기존 User 찾기
        String idCardNoNorm = StringNormalizer.normalizeIdCardNo(request.getIdCardNo());

        return userPort.findByIdCardNoNorm(idCardNoNorm)
                .map(existingUser -> {
                    // 기존 User가 있으면 제공된 정보와 일치하는지 검증
                    validateUserDataConsistency(existingUser, request);
                    return existingUser;
                })
                .orElseGet(() -> {
                    // 기존 User가 없으면 새로 생성
                    User newUser = User.create(request.getUserName(), request.getEmail(), request.getIdCardNo());
                    validateNewUserUniqueness(newUser);
                    return userPort.save(newUser);
                });
    }

    private void validateUserDataConsistency(User existingUser, CreateAccountRequest request) {
        // 이름 검증
        if (!existingUser.getName().equals(request.getUserName())) {
            throw new IllegalArgumentException(ErrorMessages.USER_NAME_MISMATCH);
        }

        // 이메일 검증
        if (!existingUser.getEmail().equals(request.getEmail())) {
            throw new IllegalArgumentException(ErrorMessages.USER_EMAIL_MISMATCH);
        }
    }

    private void validateNewUserUniqueness(User user) {
        if (userPort.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException(ErrorMessages.DUPLICATE_EMAIL);
        }
    }

    private void validateAccountUniqueness(Account account) {
        if (accountPort.existsByBankCodeAndAccountNoNorm(account.getBankCode(), account.getAccountNoNorm())) {
            throw new IllegalArgumentException(ErrorMessages.DUPLICATE_ACCOUNT_NO);
        }
    }
}