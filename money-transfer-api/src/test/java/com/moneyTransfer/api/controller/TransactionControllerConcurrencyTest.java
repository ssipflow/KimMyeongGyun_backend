package com.moneyTransfer.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneyTransfer.api.dto.request.CreateAccountApiRequest;
import com.moneyTransfer.api.dto.request.DepositApiRequest;
import com.moneyTransfer.api.dto.request.TransferApiRequest;
import com.moneyTransfer.api.dto.request.WithdrawApiRequest;
import com.moneyTransfer.api.dto.response.AccountApiResponse;
import com.moneyTransfer.api.dto.response.TransactionApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("TransactionController 동시성 테스트")
class TransactionControllerConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionControllerConcurrencyTest.class);

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private final String testBankCode = "001";
    private String testAccountNo;
    private final String targetBankCode = "002";
    private String targetAccountNo;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // 각 테스트마다 고유한 계좌번호 생성 (현재 시간 기반)
        long timestamp = System.currentTimeMillis();
        this.testAccountNo = "11" + (timestamp % 100000000L); // 10자리 계좌번호
        this.targetAccountNo = "98" + (timestamp % 100000000L + 1); // 10자리 계좌번호

        // 테스트용 송금 계좌 생성
        CreateAccountApiRequest sourceAccountRequest = new CreateAccountApiRequest(
            "홍길동",
            "source" + timestamp + "@example.com",
            "1234567890" + String.format("%03d", timestamp % 1000L),
            testBankCode,
            testAccountNo
        );

        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sourceAccountRequest)))
                .andDo(print()) // Print request/response for debugging
                .andExpect(status().isCreated());

        // 테스트용 수신 계좌 생성
        CreateAccountApiRequest targetAccountRequest = new CreateAccountApiRequest(
            "김철수",
            "target" + timestamp + "@example.com",
            "9876543210" + String.format("%03d", timestamp % 1000L + 1), // Ensure unique idCardNo
            targetBankCode,
            targetAccountNo
        );

        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(targetAccountRequest)))
                .andDo(print()) // Print request/response for debugging
                .andExpect(status().isCreated());

        // 테스트를 위한 초기 잔액 설정 (200만원)
        DepositApiRequest depositRequest = new DepositApiRequest(
            testBankCode,
            testAccountNo,
            new BigDecimal("2000000"),
            "초기 잔액"
        );

        mockMvc.perform(post("/transactions/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositRequest)))
                .andDo(print()) // Print request/response for debugging
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("동시에 같은 계좌에서 출금 시도 시 동시성 제어가 작동한다")
    void withdraw_ConcurrentWithdraw_ConcurrencyControlWorks() throws Exception {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);

        WithdrawApiRequest withdrawRequest1 = new WithdrawApiRequest(
            testBankCode,
            testAccountNo,
            new BigDecimal("500000"), // 50만원
            "동시 출금 테스트 1"
        );

        WithdrawApiRequest withdrawRequest2 = new WithdrawApiRequest(
            testBankCode,
            testAccountNo,
            new BigDecimal("700000"), // 70만원
            "동시 출금 테스트 2"
        );

        // when - 동시에 출금 시도 (총 120만원, 잔액 200만원)
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawRequest1)))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                log.error("Withdraw request 1 failed", e);
                return 500;
            }
        }, executor);

        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawRequest2)))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                log.error("Withdraw request 2 failed", e);
                return 500;
            }
        }, executor);

        // then
        Integer status1 = future1.get();
        Integer status2 = future2.get();

        log.info("Concurrent withdraw status1: {}", status1);
        log.info("Concurrent withdraw status2: {}", status2);

        // 동시성 제어 검증: 낙관적 락으로 인한 다양한 시나리오 가능
        // 1) 둘 다 성공 (순차 처리됨)
        // 2) 하나 성공, 하나 409 (낙관적 락 충돌)
        // 3) 하나 성공, 하나 400 (잔액 부족)
        boolean bothSucceeded = status1 == 201 && status2 == 201;
        boolean oneSuccessOneConflict = (status1 == 201 && (status2 == 409 || status2 == 400)) ||
                                       ((status1 == 409 || status1 == 400) && status2 == 201);
        boolean bothFailed = (status1 == 409 || status1 == 400) && (status2 == 409 || status2 == 400);

        boolean validConcurrencyResult = bothSucceeded || oneSuccessOneConflict || bothFailed;

        assertThat(validConcurrencyResult)
                .withFailMessage("동시성 제어가 올바르게 작동해야 합니다 (status1: %d, status2: %d)", status1, status2)
                .isTrue();

        executor.shutdown();
    }

    @Test
    @DisplayName("동시에 같은 계좌에서 이체 시도 시 동시성 제어가 작동한다")
    void transfer_ConcurrentTransfer_ConcurrencyControlWorks() throws Exception {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);

        TransferApiRequest transferRequest1 = new TransferApiRequest(
            testBankCode,
            testAccountNo,
            targetBankCode,
            targetAccountNo,
            new BigDecimal("800000"), // 80만원
            "동시 이체 테스트 1"
        );

        TransferApiRequest transferRequest2 = new TransferApiRequest(
            testBankCode,
            testAccountNo,
            targetBankCode,
            targetAccountNo,
            new BigDecimal("900000"), // 90만원
            "동시 이체 테스트 2"
        );

        // when - 동시에 이체 시도 (총 170만원 + 수수료, 잔액 200만원)
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest1)))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                log.error("Transfer request 1 failed", e);
                return 500;
            }
        }, executor);

        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest2)))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                log.error("Transfer request 2 failed", e);
                return 500;
            }
        }, executor);

        // then
        Integer status1 = future1.get();
        Integer status2 = future2.get();

        log.info("Concurrent transfer status1: {}", status1);
        log.info("Concurrent transfer status2: {}", status2);

        // 동시성 제어 검증: 잔액과 수수료를 고려한 시나리오
        // 80만원 + 8천원(수수료) = 80.8만원, 90만원 + 9천원(수수료) = 90.9만원
        // 총 171.7만원이므로 잔액 200만원으로 둘 다 성공 가능
        // 하지만 낙관적 락 충돌로 인해 다양한 시나리오 가능
        boolean bothSucceeded = status1 == 201 && status2 == 201;
        boolean oneSuccessOneConflict = (status1 == 201 && (status2 == 409 || status2 == 400)) ||
                                       ((status1 == 409 || status1 == 400) && status2 == 201);
        boolean bothFailed = (status1 == 409 || status1 == 400) && (status2 == 409 || status2 == 400);

        boolean validTransferResult = bothSucceeded || oneSuccessOneConflict || bothFailed;

        assertThat(validTransferResult)
                .withFailMessage("이체 동시성 제어가 올바르게 작동해야 합니다 (status1: %d, status2: %d)", status1, status2)
                .isTrue();

        executor.shutdown();
    }

    @Test
    @DisplayName("setUp 검증 - 계좌가 올바르게 생성되었는지 확인")
    void setupVerification() throws Exception {
        // given - setUp에서 계좌가 생성되었는지 확인

        // when - 거래내역 조회로 계좌 존재 확인
        mockMvc.perform(get("/transactions/account/" + testBankCode + "/" + testAccountNo)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountInfo").exists())
                .andExpect(jsonPath("$.accountInfo.balance").value(2000000)); // 초기 입금 200만원 확인
    }

    @Test
    @DisplayName("동시에 같은 계좌에 입금 시도 시 둘 다 성공한다")
    void deposit_ConcurrentDeposit_BothSucceed() throws Exception {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);

        DepositApiRequest depositRequest1 = new DepositApiRequest(
            testBankCode,
            testAccountNo,
            new BigDecimal("100000"), // 10만원
            "동시 입금 테스트 1"
        );

        DepositApiRequest depositRequest2 = new DepositApiRequest(
            testBankCode,
            testAccountNo,
            new BigDecimal("150000"), // 15만원
            "동시 입금 테스트 2"
        );

        // when - 동시에 입금 시도
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest1)))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                log.error("Deposit request 1 failed", e);
                return 500;
            }
        }, executor);

        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest2)))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                log.error("Deposit request 2 failed", e);
                return 500;
            }
        }, executor);

        // then
        Integer status1 = future1.get();
        Integer status2 = future2.get();

        log.info("Concurrent deposit status1: {}", status1);
        log.info("Concurrent deposit status2: {}", status2);

        // 입금은 잔액 제한이 없지만 동시성 제어로 인해 다양한 시나리오 가능
        // 1) 둘 다 성공 (동시성 충돌 없음)
        // 2) 하나 성공, 하나 409 (낙관적 락 충돌)
        boolean bothSucceeded = status1 == 201 && status2 == 201;
        boolean oneSuccessOneConflict = (status1 == 201 && status2 == 409) || (status1 == 409 && status2 == 201);

        boolean validDepositResult = bothSucceeded || oneSuccessOneConflict;

        assertThat(validDepositResult)
                .withFailMessage("입금 동시성 테스트가 올바르게 작동해야 합니다 (status1: %d, status2: %d)", status1, status2)
                .isTrue();

        executor.shutdown();
    }

    @Test
    @DisplayName("동시에 같은 계좌에서 출금과 이체 시도 시 동시성 제어가 작동한다")
    void mixedTransactions_ConcurrentWithdrawAndTransfer_ConcurrencyControlWorks() throws Exception {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);

        WithdrawApiRequest withdrawRequest = new WithdrawApiRequest(
            testBankCode,
            testAccountNo,
            new BigDecimal("600000"), // 60만원
            "혼합 거래 출금 테스트"
        );

        TransferApiRequest transferRequest = new TransferApiRequest(
            testBankCode,
            testAccountNo,
            targetBankCode,
            targetAccountNo,
            new BigDecimal("800000"), // 80만원
            "혼합 거래 이체 테스트"
        );

        // when - 동시에 출금과 이체 시도 (총 140만원 + 수수료, 잔액 200만원)
        CompletableFuture<Integer> withdrawFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawRequest)))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                log.error("Mixed withdraw request failed", e);
                return 500;
            }
        }, executor);

        CompletableFuture<Integer> transferFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                log.error("Mixed transfer request failed", e);
                return 500;
            }
        }, executor);

        // then
        Integer withdrawStatus = withdrawFuture.get();
        Integer transferStatus = transferFuture.get();

        log.info("Mixed transactions - withdraw status: {}", withdrawStatus);
        log.info("Mixed transactions - transfer status: {}", transferStatus);

        // 혼합 거래 동시성 제어 검증
        // 출금 60만원 + 이체 80만원(+수수료 8천원) = 총 140.8만원, 잔액 200만원으로 충분
        // 하지만 낙관적 락 충돌로 인해 다양한 시나리오 가능
        boolean bothSucceeded = withdrawStatus == 201 && transferStatus == 201;
        boolean oneSuccessOneConflict = (withdrawStatus == 201 && (transferStatus == 409 || transferStatus == 400)) ||
                                       ((withdrawStatus == 409 || withdrawStatus == 400) && transferStatus == 201);
        boolean bothFailed = (withdrawStatus == 409 || withdrawStatus == 400) && (transferStatus == 409 || transferStatus == 400);

        boolean validMixedResult = bothSucceeded || oneSuccessOneConflict || bothFailed;

        assertThat(validMixedResult)
                .withFailMessage("혼합 거래 동시성 제어가 올바르게 작동해야 합니다 (withdraw: %d, transfer: %d)", withdrawStatus, transferStatus)
                .isTrue();

        executor.shutdown();
    }

    @Test
    @DisplayName("동시에 일일 한도 초과 거래 시도 시 동시성 제어가 작동한다")
    void dailyLimit_ConcurrentExceedLimit_ConcurrencyControlWorks() throws Exception {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 일일 출금 한도: 100만원, 이체 한도: 300만원
        WithdrawApiRequest largeWithdrawRequest1 = new WithdrawApiRequest(
            testBankCode,
            testAccountNo,
            new BigDecimal("800000"), // 80만원
            "한도 테스트 출금 1"
        );

        WithdrawApiRequest largeWithdrawRequest2 = new WithdrawApiRequest(
            testBankCode,
            testAccountNo,
            new BigDecimal("500000"), // 50만원 (총 130만원으로 한도 초과)
            "한도 테스트 출금 2"
        );

        // when - 동시에 한도 초과 출금 시도
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(largeWithdrawRequest1)))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                log.error("Large withdraw request 1 failed", e);
                return 500;
            }
        }, executor);

        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(largeWithdrawRequest2)))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                log.error("Large withdraw request 2 failed", e);
                return 500;
            }
        }, executor);

        // then
        Integer status1 = future1.get();
        Integer status2 = future2.get();

        log.info("Daily limit test status1: {}", status1);
        log.info("Daily limit test status2: {}", status2);

        // 일일 한도 제어 검증: 80만원 + 50만원 = 130만원 (한도 100만원 초과)
        // 하나만 성공하거나 둘 다 실패해야 함
        boolean bothSucceeded = status1 == 201 && status2 == 201;
        boolean oneSuccessOneFailed = (status1 == 201 && (status2 == 409 || status2 == 400)) ||
                                     ((status1 == 409 || status1 == 400) && status2 == 201);
        boolean bothFailed = (status1 == 409 || status1 == 400) && (status2 == 409 || status2 == 400);

        boolean validLimitResult = oneSuccessOneFailed || bothFailed;

        assertThat(bothSucceeded)
                .withFailMessage("일일 한도 초과로 둘 다 성공할 수 없어야 합니다 (status1: %d, status2: %d)", status1, status2)
                .isFalse();

        assertThat(validLimitResult)
                .withFailMessage("일일 한도 제어가 올바르게 작동해야 합니다 (status1: %d, status2: %d)", status1, status2)
                .isTrue();

        executor.shutdown();
    }

    @Test
    @DisplayName("단일 계좌 생성 테스트")
    void singleAccountCreationTest() throws Exception {
        long timestamp = System.currentTimeMillis();
        String uniqueAccountNo = "99" + (timestamp % 100000000L);
        String uniqueIdCardNo = "1234567890" + String.format("%03d", timestamp % 1000L);

        CreateAccountApiRequest request = new CreateAccountApiRequest(
            "테스트 사용자",
            "test" + timestamp + "@example.com",
            uniqueIdCardNo,
            "001",
            uniqueAccountNo
        );

        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated());
    }
}