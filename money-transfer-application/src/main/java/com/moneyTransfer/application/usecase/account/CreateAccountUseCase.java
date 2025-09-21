package com.moneyTransfer.application.usecase.account;

import com.moneyTransfer.application.dto.account.AccountResponse;
import com.moneyTransfer.application.dto.account.CreateAccountRequest;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.common.util.StringNormalizer;
import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
import com.moneyTransfer.domain.user.User;
import com.moneyTransfer.domain.user.UserPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class CreateAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateAccountUseCase.class);

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

        // 3. Account 저장 (동시성 제어)
        Account savedAccount = saveAccountWithConcurrencyControl(account);

        return new AccountResponse(savedAccount);
    }

    private User findOrCreateUser(CreateAccountRequest request) {
        // 정규화된 주민번호로 기존 User 찾기
        String idCardNoNorm = StringNormalizer.normalizeIdCardNo(request.getIdCardNo());
        log.info("Searching for user with idCardNoNorm: {}", idCardNoNorm);

        return userPort.findByIdCardNoNorm(idCardNoNorm)
                .map(existingUser -> {
                    // 기존 User가 있으면 제공된 정보와 일치하는지 검증
                    validateUserDataConsistency(existingUser, request);
                    return existingUser;
                })
                .orElseGet(() -> createNewUser(request));
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

    private User createNewUser(CreateAccountRequest request) {
        User newUser = User.create(request.getUserName(), request.getEmail(), request.getIdCardNo());

        try {
            return userPort.save(newUser);
        } catch (DataIntegrityViolationException e) {
            // 동시성으로 인한 중복 발생 시 적절한 에러 메시지 제공
            String message = e.getMessage();
            if (message != null && message.toLowerCase().contains("email")) {
                throw new IllegalArgumentException(ErrorMessages.DUPLICATE_EMAIL);
            }
            if (message != null && message.toLowerCase().contains("id_card")) {
                throw new IllegalArgumentException(ErrorMessages.DUPLICATE_ID_CARD);
            }
            throw e;
        }
    }

    private Account saveAccountWithConcurrencyControl(Account account) {
        try {
            return accountPort.save(account);
        } catch (DataIntegrityViolationException e) {
            // 동시성으로 인한 계좌번호 중복 발생 시
            String message = e.getMessage();
            if (message != null && (message.toLowerCase().contains("account") || message.toLowerCase().contains("unique"))) {
                throw new IllegalArgumentException(ErrorMessages.DUPLICATE_ACCOUNT_NO);
            }
            throw e;
        }
    }
}