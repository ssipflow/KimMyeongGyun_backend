package com.moneyTransfer.application.account;

import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
import com.moneyTransfer.domain.user.User;
import com.moneyTransfer.domain.user.UserPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AccountApplicationService {

    private final AccountPort accountPort;
    private final UserPort userPort;

    public AccountApplicationService(AccountPort accountRepository, UserPort userRepository) {
        this.accountPort = accountRepository;
        this.userPort = userRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
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
            throw new IllegalArgumentException("이미 존재하는 이메일입니다");
        }
        if (userPort.existsByIdCardNoNorm(user.getIdCardNoNorm())) {
            throw new IllegalArgumentException("이미 존재하는 주민번호입니다");
        }
    }

    private void validateAccountUniqueness(Account account) {
        if (accountPort.existsByBankCodeAndAccountNoNorm(account.getBankCode(), account.getAccountNoNorm())) {
            throw new IllegalArgumentException("이미 존재하는 계좌번호입니다");
        }
    }

    public void deleteAccount(Long accountId) {
        Account account = accountPort.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다"));

        if (!account.isActive()) {
            throw new IllegalStateException("이미 비활성화된 계좌입니다");
        }

        // 잔액이 있는지 확인
        if (account.getBalance().compareTo(java.math.BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("잔액이 있는 계좌는 삭제할 수 없습니다");
        }

        account.deactivate();
        accountPort.save(account);
    }

    public Optional<AccountResponse> getAccount(Long accountId) {
        return accountPort.findById(accountId)
            .map(AccountResponse::new);
    }

    public List<AccountResponse> getAccountsByUserId(Long userId) {
        return accountPort.findByUserId(userId)
            .stream()
            .map(AccountResponse::new)
            .collect(Collectors.toList());
    }

    public Optional<AccountResponse> getAccountByBankCodeAndAccountNo(String bankCode, String accountNo) {
        // Account 도메인의 정규화 로직 사용
        String accountNoNorm = accountNo.replaceAll("[^0-9]", "");
        return accountPort.findByBankCodeAndAccountNoNorm(bankCode, accountNoNorm)
            .map(AccountResponse::new);
    }
}