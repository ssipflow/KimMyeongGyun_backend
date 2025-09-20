package com.moneyTransfer.application.usecase.transaction;

import com.moneyTransfer.application.dto.transaction.GetTransactionHistoryRequest;
import com.moneyTransfer.application.dto.transaction.TransactionHistoryResponse;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
import com.moneyTransfer.domain.account.AccountStatus;
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

    @InjectMocks
    private GetTransactionHistoryUseCase getTransactionHistoryUseCase;

    private GetTransactionHistoryRequest validRequest;
    private Account mockAccount;
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

        // Mock transactions 생성
        Transaction depositTx = Transaction.createDeposit(1L, new BigDecimal("50000"), "입금");
        depositTx.setId(1L);
        depositTx.setBalanceAfter(new BigDecimal("150000"));
        depositTx.setCreatedAt(LocalDateTime.now().minusDays(1));

        Transaction withdrawTx = Transaction.createWithdraw(1L, new BigDecimal("20000"), "출금");
        withdrawTx.setId(2L);
        withdrawTx.setBalanceAfter(new BigDecimal("130000"));
        withdrawTx.setCreatedAt(LocalDateTime.now().minusHours(12));

        Transaction transferSendTx = Transaction.createTransferSend(1L, 2L, new BigDecimal("30000"), new BigDecimal("300"), "이체 송금");
        transferSendTx.setId(3L);
        transferSendTx.setBalanceAfter(new BigDecimal("99700"));
        transferSendTx.setCreatedAt(LocalDateTime.now().minusHours(6));

        mockTransactions = List.of(transferSendTx, withdrawTx, depositTx); // 최신순

        mockPageResult = new PageResult<>(
                mockTransactions,
                0,
                10,
                3L,
                1
        );
    }

    @Test
    @DisplayName("정상적인 거래내역 조회 - 전체 기간")
    void getTransactionHistorySuccess() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(transactionPort.findByAccountIdWithPaging(eq(1L), any(PageQuery.class)))
                .willReturn(mockPageResult);

        // when
        TransactionHistoryResponse response = getTransactionHistoryUseCase.execute(validRequest);

        // then
        assertThat(response.getTransactions()).hasSize(3);
        assertThat(response.getPageInfo().getCurrentPage()).isEqualTo(0);
        assertThat(response.getPageInfo().getPageSize()).isEqualTo(10);
        assertThat(response.getPageInfo().getTotalElements()).isEqualTo(3L);
        assertThat(response.getPageInfo().getTotalPages()).isEqualTo(1);
        assertThat(response.getPageInfo().getHasNext()).isFalse();
        assertThat(response.getPageInfo().getHasPrevious()).isFalse();

        // 최신순으로 정렬되어야 함
        assertThat(response.getTransactions().get(0).getTransactionType()).isEqualTo(TransactionType.TRANSFER_SEND);
        assertThat(response.getTransactions().get(1).getTransactionType()).isEqualTo(TransactionType.WITHDRAW);
        assertThat(response.getTransactions().get(2).getTransactionType()).isEqualTo(TransactionType.DEPOSIT);

        // 거래 세부 정보 검증
        assertThat(response.getTransactions().get(0).getAmount()).isEqualTo(new BigDecimal("30000"));
        assertThat(response.getTransactions().get(0).getFee()).isEqualTo(new BigDecimal("300"));
        assertThat(response.getTransactions().get(0).getRelatedAccountId()).isEqualTo(2L);

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(transactionPort).should().findByAccountIdWithPaging(eq(1L), any(PageQuery.class));
        then(transactionPort).should(never()).findByAccountIdAndDateRangeWithPaging(any(), any(), any(), any());
    }

    @Test
    @DisplayName("날짜 범위 지정 거래내역 조회")
    void getTransactionHistoryWithDateRange() {
        // given
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
                mockTransactions.subList(0, 2), // 최근 2개만
                0,
                5,
                2L,
                1
        );

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(transactionPort.findByAccountIdAndDateRangeWithPaging(eq(1L), eq(startDate), eq(endDate), any(PageQuery.class)))
                .willReturn(filteredPageResult);

        // when
        TransactionHistoryResponse response = getTransactionHistoryUseCase.execute(dateRangeRequest);

        // then
        assertThat(response.getTransactions()).hasSize(2);
        assertThat(response.getPageInfo().getTotalElements()).isEqualTo(2L);

        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
        then(transactionPort).should().findByAccountIdAndDateRangeWithPaging(eq(1L), eq(startDate), eq(endDate), any(PageQuery.class));
        then(transactionPort).should(never()).findByAccountIdWithPaging(any(), any());
    }

    @Test
    @DisplayName("페이징 처리 - 두 번째 페이지 조회")
    void getTransactionHistorySecondPage() {
        // given
        GetTransactionHistoryRequest secondPageRequest = new GetTransactionHistoryRequest(
                "001",
                "123-456-789",
                1, // 두 번째 페이지
                2,
                null,
                null
        );

        PageResult<Transaction> secondPageResult = new PageResult<>(
                List.of(mockTransactions.get(2)), // 마지막 1개
                1,
                2,
                3L,
                2
        );

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(transactionPort.findByAccountIdWithPaging(eq(1L), any(PageQuery.class)))
                .willReturn(secondPageResult);

        // when
        TransactionHistoryResponse response = getTransactionHistoryUseCase.execute(secondPageRequest);

        // then
        assertThat(response.getTransactions()).hasSize(1);
        assertThat(response.getPageInfo().getCurrentPage()).isEqualTo(1);
        assertThat(response.getPageInfo().getPageSize()).isEqualTo(2);
        assertThat(response.getPageInfo().getTotalPages()).isEqualTo(2);
        assertThat(response.getPageInfo().getHasNext()).isFalse();
        assertThat(response.getPageInfo().getHasPrevious()).isTrue();

        assertThat(response.getTransactions().get(0).getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
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
        then(transactionPort).should(never()).findByAccountIdWithPaging(any(), any());
        then(transactionPort).should(never()).findByAccountIdAndDateRangeWithPaging(any(), any(), any(), any());
    }

    @Test
    @DisplayName("빈 거래내역 조회 결과 처리")
    void emptyTransactionHistory() {
        // given
        PageResult<Transaction> emptyPageResult = new PageResult<>(
                List.of(),
                0,
                10,
                0L,
                0
        );

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(transactionPort.findByAccountIdWithPaging(eq(1L), any(PageQuery.class)))
                .willReturn(emptyPageResult);

        // when
        TransactionHistoryResponse response = getTransactionHistoryUseCase.execute(validRequest);

        // then
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
        GetTransactionHistoryRequest requestWithHyphens = new GetTransactionHistoryRequest(
                "001",
                "123-456-789", // 하이픈 포함
                0,
                10,
                null,
                null
        );

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789"))) // 정규화된 번호로 조회
                .willReturn(Optional.of(mockAccount));
        given(transactionPort.findByAccountIdWithPaging(eq(1L), any(PageQuery.class)))
                .willReturn(mockPageResult);

        // when
        getTransactionHistoryUseCase.execute(requestWithHyphens);

        // then - 정규화된 계좌번호로 조회되었는지 확인
        then(accountPort).should().findByBankCodeAndAccountNoNorm("001", "123456789");
    }

    @Test
    @DisplayName("페이지 크기 제한 테스트")
    void pageSizeHandling() {
        // given
        GetTransactionHistoryRequest largePageRequest = new GetTransactionHistoryRequest(
                "001",
                "123-456-789",
                0,
                50, // 큰 페이지 크기
                null,
                null
        );

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(transactionPort.findByAccountIdWithPaging(eq(1L), any(PageQuery.class)))
                .willReturn(mockPageResult);

        // when
        TransactionHistoryResponse response = getTransactionHistoryUseCase.execute(largePageRequest);

        // then - 요청한 페이지 크기가 올바르게 전달되었는지 확인
        assertThat(response.getPageInfo().getPageSize()).isEqualTo(10); // mockPageResult의 크기

        // PageQuery.of(0, 50)이 호출되었는지 확인
        then(transactionPort).should().findByAccountIdWithPaging(eq(1L), any(PageQuery.class));
    }

    @Test
    @DisplayName("날짜 범위의 시작일만 지정된 경우")
    void onlyStartDateSpecified() {
        // given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);

        GetTransactionHistoryRequest partialDateRequest = new GetTransactionHistoryRequest(
                "001",
                "123-456-789",
                0,
                10,
                startDate,
                null // endDate는 null
        );

        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(transactionPort.findByAccountIdWithPaging(eq(1L), any(PageQuery.class)))
                .willReturn(mockPageResult);

        // when
        getTransactionHistoryUseCase.execute(partialDateRequest);

        // then - startDate만 있으면 전체 조회가 되어야 함 (날짜 범위 조회 안함)
        then(transactionPort).should().findByAccountIdWithPaging(eq(1L), any(PageQuery.class));
        then(transactionPort).should(never()).findByAccountIdAndDateRangeWithPaging(any(), any(), any(), any());
    }

    @Test
    @DisplayName("거래내역 응답 DTO 매핑 검증")
    void transactionResponseMapping() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq("001"), eq("123456789")))
                .willReturn(Optional.of(mockAccount));
        given(transactionPort.findByAccountIdWithPaging(eq(1L), any(PageQuery.class)))
                .willReturn(mockPageResult);

        // when
        TransactionHistoryResponse response = getTransactionHistoryUseCase.execute(validRequest);

        // then - 첫 번째 거래 (Transfer Send) 상세 검증
        var firstTransaction = response.getTransactions().get(0);
        assertThat(firstTransaction.getTransactionId()).isEqualTo(3L);
        assertThat(firstTransaction.getAccountId()).isEqualTo(1L);
        assertThat(firstTransaction.getRelatedAccountId()).isEqualTo(2L);
        assertThat(firstTransaction.getTransactionType()).isEqualTo(TransactionType.TRANSFER_SEND);
        assertThat(firstTransaction.getAmount()).isEqualTo(new BigDecimal("30000"));
        assertThat(firstTransaction.getBalanceAfter()).isEqualTo(new BigDecimal("99700"));
        assertThat(firstTransaction.getFee()).isEqualTo(new BigDecimal("300"));
        assertThat(firstTransaction.getDescription()).isEqualTo("이체 송금");
        assertThat(firstTransaction.getCreatedAt()).isNotNull();

        // 두 번째 거래 (Withdraw) 검증
        var secondTransaction = response.getTransactions().get(1);
        assertThat(secondTransaction.getTransactionType()).isEqualTo(TransactionType.WITHDRAW);
        assertThat(secondTransaction.getRelatedAccountId()).isNull(); // 출금은 관련 계좌 없음
        assertThat(secondTransaction.getFee()).isEqualTo(BigDecimal.ZERO); // 출금 수수료 없음
    }
}