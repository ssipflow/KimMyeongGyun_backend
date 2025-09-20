package com.moneyTransfer.application.usecase.transaction;

import com.moneyTransfer.application.dto.transaction.GetTransactionHistoryRequest;
import com.moneyTransfer.application.dto.transaction.TransactionHistoryResponse;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
import com.moneyTransfer.domain.account.AccountStatus;
import com.moneyTransfer.domain.user.User;
import com.moneyTransfer.domain.user.UserPort;
import com.moneyTransfer.domain.common.PageQuery;
import com.moneyTransfer.domain.common.PageResult;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetTransactionHistoryUseCase 테스트")
class GetTransactionHistoryUseCaseTest {

    @Mock
    private TransactionPort transactionPort;

    @Mock
    private AccountPort accountPort;

    @Mock
    private UserPort userPort;

    @InjectMocks
    private GetTransactionHistoryUseCase getTransactionHistoryUseCase;

    private GetTransactionHistoryRequest validRequest;
    private Account mockAccount;
    private User mockUser;
    private Account mockRelatedAccount;
    private List<Transaction> mockTransactions;
    private PageResult<Transaction> mockPageResult;

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
        validRequest = new GetTransactionHistoryRequest(
                "001",
                "123-456-789",
                0,
                10,
                null,
                null
        );

        mockAccount = createMockAccount(1L, 1L, "001", "123-456-789", new BigDecimal("100000"));
        mockRelatedAccount = createMockAccount(2L, 2L, "002", "987-654-321", new BigDecimal("200000"));

        mockUser = User.create("홍길동", "hong@example.com", "1234567890123");
        mockUser.setId(1L);

        // Mock transactions 생성 - 입금/출금만 사용하여 단순화
        Transaction depositTx = Transaction.createDeposit(1L, new BigDecimal("50000"), "입금");
        depositTx.setId(1L);
        depositTx.setBalanceAfter(new BigDecimal("150000"));
        depositTx.setCreatedAt(LocalDateTime.now().minusDays(1));

        Transaction withdrawTx = Transaction.createWithdraw(1L, new BigDecimal("20000"), "출금");
        withdrawTx.setId(2L);
        withdrawTx.setBalanceAfter(new BigDecimal("130000"));
        withdrawTx.setCreatedAt(LocalDateTime.now().minusHours(12));

        Transaction anotherWithdrawTx = Transaction.createWithdraw(1L, new BigDecimal("30000"), "ATM 출금");
        anotherWithdrawTx.setId(3L);
        anotherWithdrawTx.setBalanceAfter(new BigDecimal("70000"));
        anotherWithdrawTx.setCreatedAt(LocalDateTime.now().minusHours(6));

        mockTransactions = List.of(anotherWithdrawTx, withdrawTx, depositTx);

        mockPageResult = new PageResult<>(
                mockTransactions,
                0,
                10,
                3L,
                1
        );

        // 공통 mock 설정은 각 테스트에서 개별적으로 설정
    }

    @Test
    @DisplayName("정상적인 거래내역 조회 - 전체 기간")
    void getTransactionHistorySuccess() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(userPort.findById(eq(1L)))
                .willReturn(Optional.of(mockUser));
        given(transactionPort.findByAccountIdWithPaging(eq(1L), any(PageQuery.class)))
                .willReturn(mockPageResult);

        // when
        TransactionHistoryResponse response = getTransactionHistoryUseCase.execute(validRequest);

        // then
        // 계좌 정보 검증
        assertThat(response.getAccountInfo()).isNotNull();
        assertThat(response.getAccountInfo().getUserName()).isEqualTo("홍길동");
        assertThat(response.getAccountInfo().getEmail()).isEqualTo("hong@example.com");
        assertThat(response.getAccountInfo().getBalance()).isEqualTo(new BigDecimal("100000"));
        assertThat(response.getAccountInfo().getBankCode()).isEqualTo("001");
        assertThat(response.getAccountInfo().getAccountNo()).isEqualTo("123-456-789");

        // 거래내역 검증
        assertThat(response.getTransactions()).hasSize(3);
        assertThat(response.getPageInfo().getCurrentPage()).isEqualTo(0);
        assertThat(response.getPageInfo().getPageSize()).isEqualTo(10);
        assertThat(response.getPageInfo().getTotalElements()).isEqualTo(3L);
        assertThat(response.getPageInfo().getTotalPages()).isEqualTo(1);
        assertThat(response.getPageInfo().getHasNext()).isFalse();
        assertThat(response.getPageInfo().getHasPrevious()).isFalse();

        // 최신순으로 정렬되어야 함
        assertThat(response.getTransactions().get(0).getTransactionType()).isEqualTo(TransactionType.WITHDRAW);
        assertThat(response.getTransactions().get(1).getTransactionType()).isEqualTo(TransactionType.WITHDRAW);
        assertThat(response.getTransactions().get(2).getTransactionType()).isEqualTo(TransactionType.DEPOSIT);

        // 거래 세부 정보 검증
        assertThat(response.getTransactions().get(0).getAmount()).isEqualTo(new BigDecimal("30000"));
        assertThat(response.getTransactions().get(0).getFee()).isEqualTo(new BigDecimal("0"));
        assertThat(response.getTransactions().get(0).getAccountInfo().getBankCode()).isEqualTo("001");
        assertThat(response.getTransactions().get(0).getAccountInfo().getAccountNo()).isEqualTo("123-456-789");

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(userPort).should().findById(1L);
        then(transactionPort).should().findByAccountIdWithPaging(eq(1L), any(PageQuery.class));
        then(transactionPort).should(never()).findByAccountIdAndDateRangeWithPaging(any(), any(), any(), any());
    }

    @Test
    @DisplayName("날짜 범위 지정 거래내역 조회")
    void getTransactionHistoryWithDateRange() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(userPort.findById(eq(1L)))
                .willReturn(Optional.of(mockUser));

        LocalDateTime startDate = LocalDateTime.now().minusDays(2);
        LocalDateTime endDate = LocalDateTime.now();

        GetTransactionHistoryRequest dateRangeRequest = new GetTransactionHistoryRequest(
                "001",
                "123-456-789",
                0,
                5,
                startDate,
                endDate
        );

        PageResult<Transaction> filteredPageResult = new PageResult<>(
                mockTransactions.subList(0, 2),
                0,
                5,
                2L,
                1
        );

        given(transactionPort.findByAccountIdAndDateRangeWithPaging(eq(1L), eq(startDate), eq(endDate), any(PageQuery.class)))
                .willReturn(filteredPageResult);

        // when
        TransactionHistoryResponse response = getTransactionHistoryUseCase.execute(dateRangeRequest);

        // then
        assertThat(response.getTransactions()).hasSize(2);
        assertThat(response.getPageInfo().getTotalElements()).isEqualTo(2L);

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(userPort).should().findById(1L);
        then(transactionPort).should().findByAccountIdAndDateRangeWithPaging(eq(1L), eq(startDate), eq(endDate), any(PageQuery.class));
        then(transactionPort).should(never()).findByAccountIdWithPaging(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 계좌로 조회 시 예외 발생")
    void accountNotFound() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> getTransactionHistoryUseCase.execute(validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ErrorMessages.ACCOUNT_NOT_FOUND);

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(userPort).should(never()).findById(any());
        then(transactionPort).should(never()).findByAccountIdWithPaging(any(), any());
        then(transactionPort).should(never()).findByAccountIdAndDateRangeWithPaging(any(), any(), any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 조회 시 예외 발생")
    void userNotFound() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(userPort.findById(eq(1L)))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> getTransactionHistoryUseCase.execute(validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ErrorMessages.USER_NOT_FOUND);

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(userPort).should().findById(1L);
        then(transactionPort).should(never()).findByAccountIdWithPaging(any(), any());
        then(transactionPort).should(never()).findByAccountIdAndDateRangeWithPaging(any(), any(), any(), any());
    }

    @Test
    @DisplayName("빈 거래내역 조회 결과 처리")
    void emptyTransactionHistory() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(userPort.findById(eq(1L)))
                .willReturn(Optional.of(mockUser));

        PageResult<Transaction> emptyPageResult = new PageResult<>(
                List.of(),
                0,
                10,
                0L,
                0
        );

        given(transactionPort.findByAccountIdWithPaging(eq(1L), any(PageQuery.class)))
                .willReturn(emptyPageResult);

        // when
        TransactionHistoryResponse response = getTransactionHistoryUseCase.execute(validRequest);

        // then
        assertThat(response.getAccountInfo()).isNotNull();
        assertThat(response.getTransactions()).isEmpty();
        assertThat(response.getPageInfo().getTotalElements()).isEqualTo(0L);
        assertThat(response.getPageInfo().getTotalPages()).isEqualTo(0);
        assertThat(response.getPageInfo().getHasNext()).isFalse();
        assertThat(response.getPageInfo().getHasPrevious()).isFalse();
    }

    @Test
    @DisplayName("다양한 계좌번호 형식 정규화 테스트")
    void accountNumberNormalization() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(userPort.findById(eq(1L)))
                .willReturn(Optional.of(mockUser));

        GetTransactionHistoryRequest requestWithHyphens = new GetTransactionHistoryRequest(
                "001",
                "123-456-789",
                0,
                10,
                null,
                null
        );

        given(transactionPort.findByAccountIdWithPaging(eq(1L), any(PageQuery.class)))
                .willReturn(mockPageResult);

        // when
        getTransactionHistoryUseCase.execute(requestWithHyphens);

        // then - 정규화된 계좌번호로 조회되었는지 확인
        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
    }
}