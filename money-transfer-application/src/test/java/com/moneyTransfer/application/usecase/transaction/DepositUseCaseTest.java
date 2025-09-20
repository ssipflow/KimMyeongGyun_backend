package com.moneyTransfer.application.usecase.transaction;

import com.moneyTransfer.application.dto.transaction.DepositRequest;
import com.moneyTransfer.application.dto.transaction.TransactionResponse;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
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

    @BeforeEach
    void setUp() {
        validRequest = new DepositRequest(
                "001",
                "123-456-789",
                new BigDecimal("10000"),
                "급여 입금"
        );

        mockAccount = Account.createNew(
                1L,
                "001",
                "123456789",
                "123456789"
        );
        mockAccount.setId(1L);
        mockAccount.setBalance(new BigDecimal("50000"));

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
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAccountId()).isEqualTo(1L);
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
}