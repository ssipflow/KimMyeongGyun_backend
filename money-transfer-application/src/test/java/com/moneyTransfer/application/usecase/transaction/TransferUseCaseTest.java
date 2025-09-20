package com.moneyTransfer.application.usecase.transaction;

import com.moneyTransfer.application.dto.transaction.TransactionResponse;
import com.moneyTransfer.application.dto.transaction.TransferRequest;
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
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferUseCase 테스트")
class TransferUseCaseTest {

    @Mock
    private AccountPort accountPort;

    @Mock
    private TransactionPort transactionPort;

    @Mock
    private DailyLimitPort dailyLimitPort;

    @InjectMocks
    private TransferUseCase transferUseCase;

    private TransferRequest validRequest;
    private Account mockFromAccount;
    private Account mockToAccount;
    private Transaction mockSendTransaction;
    private Transaction mockReceiveTransaction;
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
        validRequest = new TransferRequest(
                "001",
                "123-456-789",
                "002",
                "987-654-321",
                new BigDecimal("100000"),
                "친구에게 이체"
        );

        mockFromAccount = createMockAccount(1L, 1L, "001", "123-456-789", new BigDecimal("200000"));
        mockToAccount = createMockAccount(2L, 2L, "002", "987-654-321", new BigDecimal("50000"));

        mockSendTransaction = Transaction.createTransferSend(
                1L,
                2L,
                new BigDecimal("100000"),
                new BigDecimal("1000"), // 1% 수수료
                "친구에게 이체"
        );
        mockSendTransaction.setId(1L);
        mockSendTransaction.setBalanceAfter(new BigDecimal("99000")); // 200000 - 100000 - 1000
        mockSendTransaction.setCreatedAt(LocalDateTime.now());

        mockReceiveTransaction = Transaction.createTransferReceive(
                2L,
                1L,
                new BigDecimal("100000"),
                "친구에게 이체"
        );
        mockReceiveTransaction.setId(2L);
        mockReceiveTransaction.setBalanceAfter(new BigDecimal("150000")); // 50000 + 100000
        mockReceiveTransaction.setCreatedAt(LocalDateTime.now());

        mockDailyLimit = DailyLimit.createNew(1L, LocalDate.now());
    }

    @Test
    @DisplayName("정상적인 이체 처리")
    void successfulTransfer() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockFromAccount));
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("002"), eq("987654321")))
                .willReturn(Optional.of(mockToAccount));
        given(dailyLimitPort.findByAccountIdAndLimitDateWithLock(eq(1L), any(LocalDate.class)))
                .willReturn(Optional.of(mockDailyLimit));
        given(dailyLimitPort.save(any(DailyLimit.class)))
                .willReturn(mockDailyLimit);
        given(accountPort.findByIdWithLock(eq(1L)))
                .willReturn(Optional.of(mockFromAccount));
        given(accountPort.findByIdWithLock(eq(2L)))
                .willReturn(Optional.of(mockToAccount));
        given(accountPort.save(any(Account.class)))
                .willReturn(mockFromAccount);
        given(transactionPort.save(any(Transaction.class)))
                .willReturn(mockSendTransaction)
                .willReturn(mockReceiveTransaction);

        // when
        TransactionResponse response = transferUseCase.execute(validRequest);

        // then
        assertThat(response.getTransactionId()).isEqualTo(1L);
        assertThat(response.getAccountInfo().getBankCode()).isEqualTo("001");
        assertThat(response.getAccountInfo().getAccountNo()).isEqualTo("123-456-789");
        assertThat(response.getRelatedAccountInfo().getBankCode()).isEqualTo("002");
        assertThat(response.getRelatedAccountInfo().getAccountNo()).isEqualTo("987-654-321");
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.TRANSFER_SEND);
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("100000"));
        assertThat(response.getFee()).isEqualTo(new BigDecimal("1000"));

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(accountPort).should().findByBankCodeAndAccountNoNorm("002", "987654321");
        then(dailyLimitPort).should().findByAccountIdAndLimitDateWithLock(eq(1L), any(LocalDate.class));
        then(accountPort).should().findByIdWithLock(1L);
        then(accountPort).should().findByIdWithLock(2L);
        then(transactionPort).should(times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("동일 계좌로 이체 시도 시 예외 발생")
    void transferToSameAccount() {
        // given
        TransferRequest sameAccountRequest = new TransferRequest(
                "001",
                "123-456-789",
                "001",
                "123-456-789",
                new BigDecimal("100000"),
                "동일 계좌 이체"
        );

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockFromAccount));

        // when & then
        assertThatThrownBy(() -> transferUseCase.execute(sameAccountRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ErrorMessages.CANNOT_TRANSFER_TO_SAME_ACCOUNT);

        then(accountPort).should(times(2)).findByBankCodeAndAccountNoNorm("001", "123456789");
        then(dailyLimitPort).should(never()).findByAccountIdAndLimitDateWithLock(any(), any());
    }

    @Test
    @DisplayName("송금 계좌가 존재하지 않을 때 예외 발생")
    void fromAccountNotFound() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> transferUseCase.execute(validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ErrorMessages.ACCOUNT_NOT_FOUND);

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(accountPort).should(never()).findByBankCodeAndAccountNoNorm("002", "987654321");
    }

    @Test
    @DisplayName("수취 계좌가 존재하지 않을 때 예외 발생")
    void toAccountNotFound() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockFromAccount));
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("002"), eq("987654321")))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> transferUseCase.execute(validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ErrorMessages.TARGET_ACCOUNT_NOT_FOUND);

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(accountPort).should().findByBankCodeAndAccountNoNorm("002", "987654321");
    }

    @Test
    @DisplayName("일일 이체 한도 초과 시 예외 발생")
    void dailyTransferLimitExceeded() {
        // given
        TransferRequest largeRequest = new TransferRequest(
                "001",
                "123-456-789",
                "002",
                "987-654-321",
                new BigDecimal("3000001"), // 한도 초과
                "큰 금액 이체"
        );

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockFromAccount));
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("002"), eq("987654321")))
                .willReturn(Optional.of(mockToAccount));
        given(dailyLimitPort.findByAccountIdAndLimitDateWithLock(eq(1L), any(LocalDate.class)))
                .willReturn(Optional.of(mockDailyLimit));

        // when & then
        assertThatThrownBy(() -> transferUseCase.execute(largeRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ErrorMessages.DAILY_TRANSFER_LIMIT_EXCEEDED);

        then(dailyLimitPort).should().findByAccountIdAndLimitDateWithLock(eq(1L), any(LocalDate.class));
        then(dailyLimitPort).should(never()).save(any());
        then(accountPort).should(never()).findByIdWithLock(any());
    }

    @Test
    @DisplayName("계좌번호 정규화 테스트")
    void accountNumberNormalization() {
        // given
        TransferRequest requestWithHyphens = new TransferRequest(
                "001",
                "123-456-789",
                "002",
                "987-654-321",
                new BigDecimal("10000"),
                "이체"
        );

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockFromAccount));
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("002"), eq("987654321")))
                .willReturn(Optional.of(mockToAccount));
        given(dailyLimitPort.findByAccountIdAndLimitDateWithLock(eq(1L), any(LocalDate.class)))
                .willReturn(Optional.of(mockDailyLimit));
        given(dailyLimitPort.save(any(DailyLimit.class)))
                .willReturn(mockDailyLimit);
        given(accountPort.findByIdWithLock(eq(1L)))
                .willReturn(Optional.of(mockFromAccount));
        given(accountPort.findByIdWithLock(eq(2L)))
                .willReturn(Optional.of(mockToAccount));
        given(accountPort.save(any(Account.class)))
                .willReturn(mockFromAccount);
        given(transactionPort.save(any(Transaction.class)))
                .willReturn(mockSendTransaction)
                .willReturn(mockReceiveTransaction);

        // when
        transferUseCase.execute(requestWithHyphens);

        // then
        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(accountPort).should().findByBankCodeAndAccountNoNorm("002", "987654321");
    }

    @Test
    @DisplayName("계좌 ID 순서대로 락 획득 테스트")
    void accountLockingOrder() {
        // given - fromAccount ID > toAccount ID인 경우
        Account higherIdFromAccount = createMockAccount(2L, 2L, "002", "987-654-321", new BigDecimal("200000"));
        Account lowerIdToAccount = createMockAccount(1L, 1L, "001", "123-456-789", new BigDecimal("50000"));

        TransferRequest reverseOrderRequest = new TransferRequest(
                "002",
                "987-654-321",
                "001",
                "123-456-789",
                new BigDecimal("100000"),
                "역순 이체"
        );

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("002"), eq("987654321")))
                .willReturn(Optional.of(higherIdFromAccount));
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(lowerIdToAccount));
        given(dailyLimitPort.findByAccountIdAndLimitDateWithLock(eq(2L), any(LocalDate.class)))
                .willReturn(Optional.of(mockDailyLimit));
        given(dailyLimitPort.save(any(DailyLimit.class)))
                .willReturn(mockDailyLimit);

        // ID 순서대로 락 획득: 1L 먼저, 2L 나중에
        given(accountPort.findByIdWithLock(eq(1L)))
                .willReturn(Optional.of(lowerIdToAccount));
        given(accountPort.findByIdWithLock(eq(2L)))
                .willReturn(Optional.of(higherIdFromAccount));

        given(accountPort.save(any(Account.class)))
                .willReturn(higherIdFromAccount);
        given(transactionPort.save(any(Transaction.class)))
                .willReturn(mockSendTransaction)
                .willReturn(mockReceiveTransaction);

        // when
        transferUseCase.execute(reverseOrderRequest);

        // then - ID 순서대로 락이 획득되었는지 검증
        then(accountPort).should().findByIdWithLock(1L); // 낮은 ID 먼저
        then(accountPort).should().findByIdWithLock(2L); // 높은 ID 나중에
    }
}