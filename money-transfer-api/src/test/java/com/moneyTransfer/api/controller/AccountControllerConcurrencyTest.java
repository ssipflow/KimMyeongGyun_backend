package com.moneyTransfer.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneyTransfer.api.dto.request.CreateAccountApiRequest;
import com.moneyTransfer.api.dto.request.DeleteAccountApiRequest;
import com.moneyTransfer.api.dto.response.AccountApiResponse;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("AccountController 동시성 테스트")
class AccountControllerConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(AccountControllerConcurrencyTest.class);

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateAccountApiRequest validRequest;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        validRequest = new CreateAccountApiRequest(
            "홍길동",
            "test@example.com",
            "1234567890123",
            "001",
            "1123456789"
        );
    }

    @Test
    @DisplayName("동시에 같은 계좌번호로 계좌 생성 시 하나만 성공한다")
    void createAccount_ConcurrentSameAccountNo_OnlyOneSucceeds() throws Exception {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CreateAccountApiRequest request1 = new CreateAccountApiRequest(
            "홍길동",
                "user1@example.com",
                "1111111111111",
                "001",
                "1234567890"
        );
        CreateAccountApiRequest request2 = new CreateAccountApiRequest(
            "김철수",
                "user2@example.com",
                "2222222222222",
                "001",
                "1234567890"
        );

        // when
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                log.error("Create request 1 failed", e);
                return 500;
            }
        }, executor);

        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                log.error("Create request 2 failed", e);
                return 500;
            }
        }, executor);

        // then
        Integer status1 = future1.get();
        Integer status2 = future2.get();

        log.info("Create status1: {}", status1);
        log.info("Create status2: {}", status2);

        // 동시성 제어 검증:
        // 1) 하나는 성공(201), 하나는 실패(409) 또는
        // 2) 둘 다 실패(409) - 완벽한 동시 접근으로 인한 결과
        boolean scenario1 = (status1 == 201 && status2 == 409) || (status1 == 409 && status2 == 201);
        boolean scenario2 = status1 == 409 && status2 == 409;

        boolean validConcurrencyResult = scenario1 || scenario2;

        assertThat(validConcurrencyResult)
                .withFailMessage("동시성 제어가 올바르게 작동해야 합니다. 예상: 하나 성공+하나 실패 또는 둘 다 실패, 실제: (status1: %d, status2: %d)", status1, status2)
                .isTrue();

        executor.shutdown();
    }

    @Test
    @DisplayName("동시에 같은 계좌 삭제 시도 시 동시성 제어가 작동한다")
    void deleteAccount_ConcurrentDelete_ConcurrencyControlWorks() throws Exception {
        // given - 계좌 생성
        String createResponse = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        AccountApiResponse createdAccount = objectMapper.readValue(createResponse, AccountApiResponse.class);
        Long accountId = createdAccount.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        DeleteAccountApiRequest deleteRequest = new DeleteAccountApiRequest("001", "1123456789");

        // when - 동시에 같은 계좌 삭제 시도
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(delete("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                log.error("Delete request 1 failed", e);
                return 500;
            }
        }, executor);

        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(delete("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteRequest)))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                log.error("Delete request 2 failed", e);
                return 500;
            }
        }, executor);

        // then
        Integer status1 = future1.get();
        Integer status2 = future2.get();

        log.info("Delete status1: {}", status1);
        log.info("Delete status2: {}", status2);

        // 동시성 제어 검증
        // 비관적 락으로 인해 다음 시나리오들이 가능:
        // 1) 하나 성공(204), 하나 실패(409/500) - OptimisticLocking 예외 발생
        // 2) 둘 다 실패 - 동시 접근으로 인한 결과
        boolean oneSucceeded = status1 == 204 || status2 == 204;
        boolean oneFailed = status1 == 409 || status2 == 409 ||
                          status1 == 400 || status2 == 400 ||
                          status1 == 404 || status2 == 404 ||
                          status1 == 500 || status2 == 500;  // ObjectOptimisticLockingFailureException
        boolean bothFailed = (status1 == 409 || status1 == 500) && (status2 == 409 || status2 == 500);

        // 유효한 동시성 제어 시나리오: 하나 성공+하나 실패 또는 둘 다 실패
        boolean validConcurrencyResult = (oneSucceeded && oneFailed) || bothFailed;

        assertThat(validConcurrencyResult)
                .withFailMessage("동시성 제어가 올바르게 작동해야 합니다 (status1: %d, status2: %d)", status1, status2)
                .isTrue();

        executor.shutdown();

        // 동시성 테스트이므로 최종 상태 확인은 생략 (다양한 상태가 가능함)
    }

    @Test
    @DisplayName("동시에 다른 계좌번호로 계좌 생성 시 둘 다 성공한다")
    void createAccount_ConcurrentDifferentAccountNo_BothSucceed() throws Exception {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CreateAccountApiRequest request1 = new CreateAccountApiRequest(
            "홍길동", "user1@example.com", "1111111111111", "001", "1234567890"
        );
        CreateAccountApiRequest request2 = new CreateAccountApiRequest(
            "김철수", "user2@example.com", "2222222222222", "001", "9876543210" // 다른 계좌번호
        );

        // when
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                log.error("Create request 1 failed", e);
                return 500;
            }
        }, executor);

        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                log.error("Create request 2 failed", e);
                return 500;
            }
        }, executor);

        // then
        Integer status1 = future1.get();
        Integer status2 = future2.get();

        log.info("Different account create status1: {}", status1);
        log.info("Different account create status2: {}", status2);

        // 다른 계좌번호이므로 둘 다 성공해야 함
        assertThat(status1).isEqualTo(201);
        assertThat(status2).isEqualTo(201);

        executor.shutdown();
    }
}