package com.moneyTransfer.application.usecase.transaction;

import com.moneyTransfer.application.dto.transaction.DepositRequest;
import com.moneyTransfer.application.dto.transaction.TransactionResponse;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
import com.moneyTransfer.domain.account.AccountStatus;
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
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepositUseCase 테스트")
class DepositUseCaseTest {

    @Mock
    private AccountPort accountPort;

    @Mock
    private TransactionPort transactionPort;

    @InjectMocks
    private DepositUseCase depositUseCase;

    private DepositRequest validRequest;
    private Account mockAccount;
    private Transaction mockTransaction;

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
        validRequest = new DepositRequest(
                "001",
                "123-456-789",
                new BigDecimal("10000"),
                "급여 입금"
        );

        mockAccount = createMockAccount(1L, 1L, "001", "123-456-789", new BigDecimal("50000"));

        mockTransaction = Transaction.createDeposit(
                1L,
                new BigDecimal("10000"),
                "급여 입금"
        );
        mockTransaction.setId(1L);
        mockTransaction.setBalanceAfter(new BigDecimal("60000"));
        mockTransaction.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("정상적인 입금 처리")
    void successfulDeposit() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(accountPort.findByIdWithLock(eq(1L)))
                .willReturn(Optional.of(mockAccount));
        given(accountPort.save(any(Account.class)))
                .willReturn(mockAccount);
        given(transactionPort.save(any(Transaction.class)))
                .willReturn(mockTransaction);

        // when
        TransactionResponse response = depositUseCase.execute(validRequest);

        // then
        assertThat(response.getTransactionId()).isEqualTo(1L);
        assertThat(response.getAccountInfo().getBankCode()).isEqualTo("001");
        assertThat(response.getAccountInfo().getAccountNo()).isEqualTo("123-456-789");
        assertThat(response.getRelatedAccountInfo()).isNull(); // 입금은 관련 계좌 없음
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("10000"));
        assertThat(response.getBalanceAfter()).isEqualTo(new BigDecimal("60000"));

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(accountPort).should().findByIdWithLock(1L);
        then(accountPort).should().save(any(Account.class));
        then(transactionPort).should().save(any(Transaction.class));
    }

    @Test
    @DisplayName("존재하지 않는 계좌로 입금 시도")
    void depositToNonExistentAccount() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> depositUseCase.execute(validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ErrorMessages.ACCOUNT_NOT_FOUND);

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(accountPort).should(never()).findByIdWithLock(any());
        then(accountPort).should(never()).save(any());
        then(transactionPort).should(never()).save(any());
    }

    @Test
    @DisplayName("계좌번호 정규화 테스트")
    void accountNumberNormalization() {
        // given
        DepositRequest requestWithHyphens = new DepositRequest(
                "001",
                "123-456-789",
                new BigDecimal("10000"),
                "입금"
        );

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(accountPort.findByIdWithLock(eq(1L)))
                .willReturn(Optional.of(mockAccount));
        given(accountPort.save(any(Account.class)))
                .willReturn(mockAccount);
        given(transactionPort.save(any(Transaction.class)))
                .willReturn(mockTransaction);

        // when
        depositUseCase.execute(requestWithHyphens);

        // then
        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
    }

    @Test
    @DisplayName("잘못된 입금 금액 - 0원 입금 시도")
    void depositZeroAmount() {
        // given
        DepositRequest zeroAmountRequest = new DepositRequest(
                "001",
                "123-456-789",
                BigDecimal.ZERO,
                "0원 입금"
        );

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(accountPort.findByIdWithLock(eq(1L)))
                .willReturn(Optional.of(mockAccount));

        // when & then
        assertThatThrownBy(() -> depositUseCase.execute(zeroAmountRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ErrorMessages.DEPOSIT_AMOUNT_INVALID);

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(accountPort).should().findByIdWithLock(1L);
        then(accountPort).should(never()).save(any());
        then(transactionPort).should(never()).save(any());
    }

    @Test
    @DisplayName("잘못된 입금 금액 - 음수 금액 입금 시도")
    void depositNegativeAmount() {
        // given
        DepositRequest negativeAmountRequest = new DepositRequest(
                "001",
                "123-456-789",
                new BigDecimal("-1000"),
                "음수 입금"
        );

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(accountPort.findByIdWithLock(eq(1L)))
                .willReturn(Optional.of(mockAccount));

        // when & then
        assertThatThrownBy(() -> depositUseCase.execute(negativeAmountRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ErrorMessages.DEPOSIT_AMOUNT_INVALID);

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(accountPort).should().findByIdWithLock(1L);
        then(accountPort).should(never()).save(any());
        then(transactionPort).should(never()).save(any());
    }

    @Test
    @DisplayName("비활성화된 계좌로 입금 시도")
    void depositToDeactivatedAccount() {
        // given
        Account deactivatedAccount = createMockAccount(1L, 1L, "001", "123-456-789", BigDecimal.ZERO);
        deactivatedAccount.deactivate();

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(deactivatedAccount));
        given(accountPort.findByIdWithLock(eq(1L)))
                .willReturn(Optional.of(deactivatedAccount));

        // when & then
        assertThatThrownBy(() -> depositUseCase.execute(validRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ErrorMessages.INACTIVE_ACCOUNT_DEPOSIT);

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(accountPort).should().findByIdWithLock(1L);
        then(accountPort).should(never()).save(any());
        then(transactionPort).should(never()).save(any());
    }

    @Test
    @DisplayName("계좌 잠금 실패 시 예외 발생")
    void failToLockAccount() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(accountPort.findByIdWithLock(eq(1L)))
                .willReturn(Optional.empty()); // 잠금 실패

        // when & then
        assertThatThrownBy(() -> depositUseCase.execute(validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ErrorMessages.ACCOUNT_NOT_FOUND);

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(accountPort).should().findByIdWithLock(1L);
        then(accountPort).should(never()).save(any());
        then(transactionPort).should(never()).save(any());
    }

    @Test
    @DisplayName("큰 금액 입금 처리")
    void depositLargeAmount() {
        // given
        BigDecimal largeAmount = new BigDecimal("1000000000"); // 10억원
        DepositRequest largeAmountRequest = new DepositRequest(
                "001",
                "123-456-789",
                largeAmount,
                "대액 입금"
        );

        Account largeBalanceAccount = createMockAccount(1L, 1L, "001", "123-456-789", new BigDecimal("50000"));

        Transaction largeTransaction = Transaction.createDeposit(1L, largeAmount, "대액 입금");
        largeTransaction.setId(1L);
        largeTransaction.setBalanceAfter(new BigDecimal("1000050000"));
        largeTransaction.setCreatedAt(LocalDateTime.now());

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(largeBalanceAccount));
        given(accountPort.findByIdWithLock(eq(1L)))
                .willReturn(Optional.of(largeBalanceAccount));
        given(accountPort.save(any(Account.class)))
                .willReturn(largeBalanceAccount);
        given(transactionPort.save(any(Transaction.class)))
                .willReturn(largeTransaction);

        // when
        TransactionResponse response = depositUseCase.execute(largeAmountRequest);

        // then
        assertThat(response.getAmount()).isEqualTo(largeAmount);
        assertThat(response.getBalanceAfter()).isEqualTo(new BigDecimal("1000050000"));
        assertThat(response.getDescription()).isEqualTo("대액 입금");

        then(accountPort).should().save(any(Account.class));
        then(transactionPort).should().save(any(Transaction.class));
    }

    @Test
    @DisplayName("다양한 계좌번호 형식 정규화 테스트")
    void variousAccountNumberFormats() {
        // given
        String[] accountNumbers = {
                "123456789",      // 숫자만
                "123-456-789",    // 하이픈 포함
                "123 456 789",    // 공백 포함
                "123-456 789",    // 하이픈과 공백 혼합
                "  123-456-789  " // 앞뒤 공백 포함
        };

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(accountPort.findByIdWithLock(eq(1L)))
                .willReturn(Optional.of(mockAccount));
        given(accountPort.save(any(Account.class)))
                .willReturn(mockAccount);
        given(transactionPort.save(any(Transaction.class)))
                .willReturn(mockTransaction);

        // when & then
        for (String accountNo : accountNumbers) {
            DepositRequest request = new DepositRequest(
                    "001",
                    accountNo,
                    new BigDecimal("10000"),
                    "정규화 테스트"
            );

            // 예외가 발생하지 않아야 함
            TransactionResponse response = depositUseCase.execute(request);
            assertThat(response.getTransactionId()).isEqualTo(1L);
        }

        // 모든 요청이 동일하게 정규화된 계좌번호로 처리됨을 확인
        then(accountPort).should(times(accountNumbers.length))
                .findByBankCodeAndAccountNoNorm("001", "123456789");
    }

    @Test
    @DisplayName("입금 설명이 null인 경우 기본값 사용")
    void depositWithNullDescription() {
        // given
        DepositRequest requestWithNullDescription = new DepositRequest(
                "001",
                "123-456-789",
                new BigDecimal("10000"),
                null
        );

        Transaction transactionWithDefaultDesc = Transaction.createDeposit(
                1L,
                new BigDecimal("10000"),
                "입금" // 기본값
        );
        transactionWithDefaultDesc.setId(1L);
        transactionWithDefaultDesc.setBalanceAfter(new BigDecimal("60000"));
        transactionWithDefaultDesc.setCreatedAt(LocalDateTime.now());

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(accountPort.findByIdWithLock(eq(1L)))
                .willReturn(Optional.of(mockAccount));
        given(accountPort.save(any(Account.class)))
                .willReturn(mockAccount);
        given(transactionPort.save(any(Transaction.class)))
                .willReturn(transactionWithDefaultDesc);

        // when
        TransactionResponse response = depositUseCase.execute(requestWithNullDescription);

        // then
        assertThat(response.getDescription()).isEqualTo("입금");
    }

    @Test
    @DisplayName("동시성 제어 - 계좌 잠금 확인")
    void concurrencyControl() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(accountPort.findByIdWithLock(eq(1L)))
                .willReturn(Optional.of(mockAccount));
        given(accountPort.save(any(Account.class)))
                .willReturn(mockAccount);
        given(transactionPort.save(any(Transaction.class)))
                .willReturn(mockTransaction);

        // when
        depositUseCase.execute(validRequest);

        // then - 반드시 잠금을 획득한 후 처리해야 함
        then(accountPort).should().findByIdWithLock(1L);

        // Mock 호출 순서 확인
        var inOrder = inOrder(accountPort, transactionPort);
        inOrder.verify(accountPort).findByBankCodeAndAccountNoNorm("001", "123456789");
        inOrder.verify(accountPort).findByIdWithLock(1L);
        inOrder.verify(accountPort).save(any(Account.class));
        inOrder.verify(transactionPort).save(any(Transaction.class));
    }
}