package com.moneyTransfer.api.mapper;

import com.moneyTransfer.api.dto.request.DepositApiRequest;
import com.moneyTransfer.api.dto.request.TransferApiRequest;
import com.moneyTransfer.api.dto.request.WithdrawApiRequest;
import com.moneyTransfer.api.dto.response.TransactionApiResponse;
import com.moneyTransfer.api.dto.response.TransactionHistoryApiResponse;
import com.moneyTransfer.application.dto.transaction.*;
import com.moneyTransfer.domain.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionDtoMapper 테스트")
class TransactionDtoMapperTest {

    private TransactionDtoMapper transactionDtoMapper;

    @BeforeEach
    void setUp() {
        transactionDtoMapper = new TransactionDtoMapper();
    }

    @Test
    @DisplayName("DepositApiRequest를 DepositRequest로 변환")
    void toApplicationRequestFromDepositApiRequest() {
        // given
        DepositApiRequest apiRequest = new DepositApiRequest(
                "001",
                "123-456-789",
                new BigDecimal("10000"),
                "급여 입금"
        );

        // when
        DepositRequest applicationRequest = transactionDtoMapper.toApplicationRequest(apiRequest);

        // then
        assertThat(applicationRequest.getBankCode()).isEqualTo("001");
        assertThat(applicationRequest.getAccountNo()).isEqualTo("123-456-789");
        assertThat(applicationRequest.getAmount()).isEqualTo(new BigDecimal("10000"));
        assertThat(applicationRequest.getDescription()).isEqualTo("급여 입금");
    }

    @Test
    @DisplayName("WithdrawApiRequest를 WithdrawRequest로 변환")
    void toApplicationRequestFromWithdrawApiRequest() {
        // given
        WithdrawApiRequest apiRequest = new WithdrawApiRequest(
                "001",
                "123-456-789",
                new BigDecimal("50000"),
                "ATM 출금"
        );

        // when
        WithdrawRequest applicationRequest = transactionDtoMapper.toApplicationRequest(apiRequest);

        // then
        assertThat(applicationRequest.getBankCode()).isEqualTo("001");
        assertThat(applicationRequest.getAccountNo()).isEqualTo("123-456-789");
        assertThat(applicationRequest.getAmount()).isEqualTo(new BigDecimal("50000"));
        assertThat(applicationRequest.getDescription()).isEqualTo("ATM 출금");
    }

    @Test
    @DisplayName("TransferApiRequest를 TransferRequest로 변환")
    void toApplicationRequestFromTransferApiRequest() {
        // given
        TransferApiRequest apiRequest = new TransferApiRequest(
                "001",
                "123-456-789",
                "002",
                "987-654-321",
                new BigDecimal("100000"),
                "친구에게 이체"
        );

        // when
        TransferRequest applicationRequest = transactionDtoMapper.toApplicationRequest(apiRequest);

        // then
        assertThat(applicationRequest.getFromBankCode()).isEqualTo("001");
        assertThat(applicationRequest.getFromAccountNo()).isEqualTo("123-456-789");
        assertThat(applicationRequest.getToBankCode()).isEqualTo("002");
        assertThat(applicationRequest.getToAccountNo()).isEqualTo("987-654-321");
        assertThat(applicationRequest.getAmount()).isEqualTo(new BigDecimal("100000"));
        assertThat(applicationRequest.getDescription()).isEqualTo("친구에게 이체");
    }

    @Test
    @DisplayName("거래내역 조회 파라미터를 GetTransactionHistoryRequest로 변환 - 기본값 적용")
    void toApplicationRequestForTransactionHistory() {
        // given
        String bankCode = "001";
        String accountNo = "123-456-789";
        Integer page = null;
        Integer size = null;
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        // when
        GetTransactionHistoryRequest applicationRequest = transactionDtoMapper.toApplicationRequest(
                bankCode, accountNo, page, size, startDate, endDate);

        // then
        assertThat(applicationRequest.getBankCode()).isEqualTo("001");
        assertThat(applicationRequest.getAccountNo()).isEqualTo("123-456-789");
        assertThat(applicationRequest.getPage()).isEqualTo(0); // 기본값
        assertThat(applicationRequest.getSize()).isEqualTo(10); // 기본값
        assertThat(applicationRequest.getStartDate()).isNull();
        assertThat(applicationRequest.getEndDate()).isNull();
    }

    @Test
    @DisplayName("거래내역 조회 파라미터를 GetTransactionHistoryRequest로 변환 - 모든 값 지정")
    void toApplicationRequestForTransactionHistoryWithAllParameters() {
        // given
        String bankCode = "001";
        String accountNo = "123-456-789";
        Integer page = 2;
        Integer size = 5;
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

        // when
        GetTransactionHistoryRequest applicationRequest = transactionDtoMapper.toApplicationRequest(
                bankCode, accountNo, page, size, startDate, endDate);

        // then
        assertThat(applicationRequest.getBankCode()).isEqualTo("001");
        assertThat(applicationRequest.getAccountNo()).isEqualTo("123-456-789");
        assertThat(applicationRequest.getPage()).isEqualTo(2);
        assertThat(applicationRequest.getSize()).isEqualTo(5);
        assertThat(applicationRequest.getStartDate()).isEqualTo(startDate);
        assertThat(applicationRequest.getEndDate()).isEqualTo(endDate);
    }

    @Test
    @DisplayName("TransactionResponse를 TransactionApiResponse로 변환 - 관련 계좌 없음")
    void toApiResponseFromTransactionResponseWithoutRelatedAccount() {
        // given
        TransactionResponse applicationResponse = new TransactionResponse(
                1L,
                new TransactionResponse.AccountInfo("001", "123-456-789"),
                null, // 관련 계좌 없음
                TransactionType.DEPOSIT,
                new BigDecimal("10000"),
                new BigDecimal("60000"),
                "급여 입금",
                LocalDateTime.of(2024, 1, 15, 10, 30, 0),
                BigDecimal.ZERO
        );

        // when
        TransactionApiResponse apiResponse = transactionDtoMapper.toApiResponse(applicationResponse);

        // then
        assertThat(apiResponse.getTransactionId()).isEqualTo(1L);
        assertThat(apiResponse.getAccountInfo().getBankCode()).isEqualTo("001");
        assertThat(apiResponse.getAccountInfo().getAccountNo()).isEqualTo("123-456-789");
        assertThat(apiResponse.getRelatedAccountInfo()).isNull();
        assertThat(apiResponse.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(apiResponse.getAmount()).isEqualTo(new BigDecimal("10000"));
        assertThat(apiResponse.getBalanceAfter()).isEqualTo(new BigDecimal("60000"));
        assertThat(apiResponse.getDescription()).isEqualTo("급여 입금");
        assertThat(apiResponse.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        assertThat(apiResponse.getFee()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("TransactionResponse를 TransactionApiResponse로 변환 - 관련 계좌 있음")
    void toApiResponseFromTransactionResponseWithRelatedAccount() {
        // given
        TransactionResponse applicationResponse = new TransactionResponse(
                2L,
                new TransactionResponse.AccountInfo("001", "123-456-789"),
                new TransactionResponse.AccountInfo("002", "987-654-321"), // 관련 계좌 있음
                TransactionType.TRANSFER_SEND,
                new BigDecimal("100000"),
                new BigDecimal("99000"),
                "친구에게 이체",
                LocalDateTime.of(2024, 1, 15, 14, 30, 0),
                new BigDecimal("1000")
        );

        // when
        TransactionApiResponse apiResponse = transactionDtoMapper.toApiResponse(applicationResponse);

        // then
        assertThat(apiResponse.getTransactionId()).isEqualTo(2L);
        assertThat(apiResponse.getAccountInfo().getBankCode()).isEqualTo("001");
        assertThat(apiResponse.getAccountInfo().getAccountNo()).isEqualTo("123-456-789");
        assertThat(apiResponse.getRelatedAccountInfo()).isNotNull();
        assertThat(apiResponse.getRelatedAccountInfo().getBankCode()).isEqualTo("002");
        assertThat(apiResponse.getRelatedAccountInfo().getAccountNo()).isEqualTo("987-654-321");
        assertThat(apiResponse.getTransactionType()).isEqualTo(TransactionType.TRANSFER_SEND);
        assertThat(apiResponse.getAmount()).isEqualTo(new BigDecimal("100000"));
        assertThat(apiResponse.getBalanceAfter()).isEqualTo(new BigDecimal("99000"));
        assertThat(apiResponse.getDescription()).isEqualTo("친구에게 이체");
        assertThat(apiResponse.getFee()).isEqualTo(new BigDecimal("1000"));
    }

    @Test
    @DisplayName("TransactionHistoryResponse를 TransactionHistoryApiResponse로 변환")
    void toApiResponseFromTransactionHistoryResponse() {
        // given
        TransactionResponse transaction1 = new TransactionResponse(
                1L,
                new TransactionResponse.AccountInfo("001", "123-456-789"),
                null,
                TransactionType.DEPOSIT,
                new BigDecimal("50000"),
                new BigDecimal("150000"),
                "입금",
                LocalDateTime.of(2024, 1, 10, 9, 0, 0),
                BigDecimal.ZERO
        );

        TransactionResponse transaction2 = new TransactionResponse(
                2L,
                new TransactionResponse.AccountInfo("001", "123-456-789"),
                new TransactionResponse.AccountInfo("002", "987-654-321"),
                TransactionType.TRANSFER_SEND,
                new BigDecimal("30000"),
                new BigDecimal("119700"),
                "이체",
                LocalDateTime.of(2024, 1, 11, 15, 30, 0),
                new BigDecimal("300")
        );

        TransactionHistoryResponse applicationResponse = new TransactionHistoryResponse(
                new TransactionHistoryResponse.AccountInfo(
                        "홍길동",
                        "hong@example.com",
                        new BigDecimal("100000"),
                        "001",
                        "123-456-789"
                ),
                List.of(transaction1, transaction2),
                new TransactionHistoryResponse.PageInfo(0, 10, 2L, 1, false, false)
        );

        // when
        TransactionHistoryApiResponse apiResponse = transactionDtoMapper.toApiResponse(applicationResponse);

        // then
        // 계좌 정보 검증
        assertThat(apiResponse.getAccountInfo().getUserName()).isEqualTo("홍길동");
        assertThat(apiResponse.getAccountInfo().getEmail()).isEqualTo("hong@example.com");
        assertThat(apiResponse.getAccountInfo().getBalance()).isEqualTo(new BigDecimal("100000"));
        assertThat(apiResponse.getAccountInfo().getBankCode()).isEqualTo("001");
        assertThat(apiResponse.getAccountInfo().getAccountNo()).isEqualTo("123-456-789");

        // 거래내역 검증
        assertThat(apiResponse.getTransactions()).hasSize(2);

        TransactionApiResponse firstTransaction = apiResponse.getTransactions().get(0);
        assertThat(firstTransaction.getTransactionId()).isEqualTo(1L);
        assertThat(firstTransaction.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(firstTransaction.getAmount()).isEqualTo(new BigDecimal("50000"));
        assertThat(firstTransaction.getRelatedAccountInfo()).isNull();

        TransactionApiResponse secondTransaction = apiResponse.getTransactions().get(1);
        assertThat(secondTransaction.getTransactionId()).isEqualTo(2L);
        assertThat(secondTransaction.getTransactionType()).isEqualTo(TransactionType.TRANSFER_SEND);
        assertThat(secondTransaction.getAmount()).isEqualTo(new BigDecimal("30000"));
        assertThat(secondTransaction.getRelatedAccountInfo()).isNotNull();
        assertThat(secondTransaction.getRelatedAccountInfo().getBankCode()).isEqualTo("002");

        // 페이징 정보 검증
        assertThat(apiResponse.getPageInfo().getCurrentPage()).isEqualTo(0);
        assertThat(apiResponse.getPageInfo().getPageSize()).isEqualTo(10);
        assertThat(apiResponse.getPageInfo().getTotalElements()).isEqualTo(2L);
        assertThat(apiResponse.getPageInfo().getTotalPages()).isEqualTo(1);
        assertThat(apiResponse.getPageInfo().getHasNext()).isFalse();
        assertThat(apiResponse.getPageInfo().getHasPrevious()).isFalse();
    }

    @Test
    @DisplayName("TransactionHistoryResponse를 TransactionHistoryApiResponse로 변환 - 빈 거래내역")
    void toApiResponseFromEmptyTransactionHistoryResponse() {
        // given
        TransactionHistoryResponse applicationResponse = new TransactionHistoryResponse(
                new TransactionHistoryResponse.AccountInfo(
                        "김철수",
                        "kim@example.com",
                        new BigDecimal("50000"),
                        "002",
                        "987-654-321"
                ),
                List.of(), // 빈 거래내역
                new TransactionHistoryResponse.PageInfo(0, 10, 0L, 0, false, false)
        );

        // when
        TransactionHistoryApiResponse apiResponse = transactionDtoMapper.toApiResponse(applicationResponse);

        // then
        assertThat(apiResponse.getAccountInfo().getUserName()).isEqualTo("김철수");
        assertThat(apiResponse.getAccountInfo().getEmail()).isEqualTo("kim@example.com");
        assertThat(apiResponse.getTransactions()).isEmpty();
        assertThat(apiResponse.getPageInfo().getTotalElements()).isEqualTo(0L);
        assertThat(apiResponse.getPageInfo().getTotalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("null 값 처리 - description null")
    void handleNullDescription() {
        // given
        DepositApiRequest apiRequest = new DepositApiRequest(
                "001",
                "123-456-789",
                new BigDecimal("10000"),
                null // description이 null
        );

        // when
        DepositRequest applicationRequest = transactionDtoMapper.toApplicationRequest(apiRequest);

        // then
        assertThat(applicationRequest.getDescription()).isNull();
    }
}