package com.moneyTransfer.application.usecase.transaction;

import com.moneyTransfer.application.dto.transaction.TransactionResponse;
import com.moneyTransfer.application.dto.transaction.WithdrawRequest;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
import com.moneyTransfer.domain.account.AccountStatus;
import com.moneyTransfer.domain.dailylimit.DailyLimit;
import com.moneyTransfer.domain.dailylimit.DailyLimitPort;
import com.moneyTransfer.domain.transaction.Transaction;
import com.moneyTransfer.domain.transaction.TransactionPort;
import com.moneyTransfer.domain.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawUseCase 테스트")
class WithdrawUseCaseTest {

    @Mock
    private AccountPort accountPort;

    @Mock
    private TransactionPort transactionPort;

    @Mock
    private DailyLimitPort dailyLimitPort;

    @InjectMocks
    private WithdrawUseCase withdrawUseCase;

    private WithdrawRequest validRequest;
    private Account mockAccount;
    private Transaction mockTransaction;
    private DailyLimit mockDailyLimit;

    private Account createMockAccount(Long id, Long userId, String bankCode, String accountNo, BigDecimal balance) {
        Account account = new Account();
        account.setId(id);
        account.setUserId(userId);
        account.setBankCode(bankCode);
        account.setAccountNo(accountNo);
        account.setAccountNoNorm(accountNo.replaceAll("[-\\s]", ""));
        account.setBalance(balance);
        account.setStatus(AccountStatus.ACTIVATE);
        return account;
    }

    @BeforeEach
    void setUp() {
        validRequest = new WithdrawRequest(
                "001",
                "123-456-789",
                new BigDecimal("50000"),
                "ATM 출금"
        );

        mockAccount = createMockAccount(1L, 1L, "001", "123-456-789", new BigDecimal("100000"));

        mockTransaction = Transaction.createWithdraw(
                1L,
                new BigDecimal("50000"),
                "ATM 출금"
        );
        mockTransaction.setId(1L);
        mockTransaction.setBalanceAfter(new BigDecimal("50000"));
        mockTransaction.setCreatedAt(LocalDateTime.now());

        mockDailyLimit = DailyLimit.createNew(1L, LocalDate.now());
    }

    @Test
    @DisplayName("정상적인 출금 처리")
    void successfulWithdraw() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(dailyLimitPort.findByAccountIdAndLimitDateWithLock(eq(1L), any(LocalDate.class)))
                .willReturn(Optional.of(mockDailyLimit));
        given(dailyLimitPort.save(any(DailyLimit.class)))
                .willReturn(mockDailyLimit);
        given(accountPort.findByIdWithLock(eq(1L)))
                .willReturn(Optional.of(mockAccount));
        given(accountPort.save(any(Account.class)))
                .willReturn(mockAccount);
        given(transactionPort.save(any(Transaction.class)))
                .willReturn(mockTransaction);

        // when
        TransactionResponse response = withdrawUseCase.execute(validRequest);

        // then
        assertThat(response.getTransactionId()).isEqualTo(1L);
        assertThat(response.getAccountInfo().getBankCode()).isEqualTo("001");
        assertThat(response.getAccountInfo().getAccountNo()).isEqualTo("123-456-789");
        assertThat(response.getRelatedAccountInfo()).isNull(); // 출금은 관련 계좌 없음
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.WITHDRAW);
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("50000"));
        assertThat(response.getBalanceAfter()).isEqualTo(new BigDecimal("50000"));

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(dailyLimitPort).should().findByAccountIdAndLimitDateWithLock(eq(1L), any(LocalDate.class));
        then(dailyLimitPort).should().save(any(DailyLimit.class));
        then(accountPort).should().findByIdWithLock(1L);
        then(accountPort).should().save(any(Account.class));
        then(transactionPort).should().save(any(Transaction.class));
    }

    @Test
    @DisplayName("존재하지 않는 계좌로 출금 시도")
    void withdrawFromNonExistentAccount() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> withdrawUseCase.execute(validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ErrorMessages.ACCOUNT_NOT_FOUND);

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(dailyLimitPort).should(never()).findByAccountIdAndLimitDateWithLock(any(), any());
    }

    @Test
    @DisplayName("일일 출금 한도 초과 시 예외 발생")
    void dailyWithdrawLimitExceeded() {
        // given
        WithdrawRequest largeRequest = new WithdrawRequest(
                "001",
                "123-456-789",
                new BigDecimal("1000001"), // 한도 초과
                "큰 금액 출금"
        );

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(dailyLimitPort.findByAccountIdAndLimitDateWithLock(eq(1L), any(LocalDate.class)))
                .willReturn(Optional.of(mockDailyLimit));

        // when & then
        assertThatThrownBy(() -> withdrawUseCase.execute(largeRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ErrorMessages.DAILY_WITHDRAW_LIMIT_EXCEEDED);

        then(dailyLimitPort).should().findByAccountIdAndLimitDateWithLock(eq(1L), any(LocalDate.class));
        then(dailyLimitPort).should(never()).save(any());
        then(accountPort).should(never()).findByIdWithLock(any());
    }

    @Test
    @DisplayName("계좌번호 정규화 테스트")
    void accountNumberNormalization() {
        // given
        WithdrawRequest requestWithHyphens = new WithdrawRequest(
                "001",
                "123-456-789",
                new BigDecimal("10000"),
                "출금"
        );

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(dailyLimitPort.findByAccountIdAndLimitDateWithLock(eq(1L), any(LocalDate.class)))
                .willReturn(Optional.of(mockDailyLimit));
        given(dailyLimitPort.save(any(DailyLimit.class)))
                .willReturn(mockDailyLimit);
        given(accountPort.findByIdWithLock(eq(1L)))
                .willReturn(Optional.of(mockAccount));
        given(accountPort.save(any(Account.class)))
                .willReturn(mockAccount);
        given(transactionPort.save(any(Transaction.class)))
                .willReturn(mockTransaction);

        // when
        withdrawUseCase.execute(requestWithHyphens);

        // then
        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
    }
}