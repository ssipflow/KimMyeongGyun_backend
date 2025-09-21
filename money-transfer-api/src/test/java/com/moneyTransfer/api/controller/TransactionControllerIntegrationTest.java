package com.moneyTransfer.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneyTransfer.api.dto.request.CreateAccountApiRequest;
import com.moneyTransfer.api.dto.request.DepositApiRequest;
import com.moneyTransfer.api.dto.request.TransferApiRequest;
import com.moneyTransfer.api.dto.request.WithdrawApiRequest;
import com.moneyTransfer.api.dto.response.AccountApiResponse;
import com.moneyTransfer.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("TransactionController 통합 테스트")
class TransactionControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private String testBankCode = "001";
    private String testAccountNo = "1123456789";

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // 테스트용 계좌 생성
        CreateAccountApiRequest accountRequest = new CreateAccountApiRequest(
            "홍길동",
            "test@example.com",
            "1234567890123",
            testBankCode,
            testAccountNo
        );

        String response = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(accountRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        AccountApiResponse accountResponse = objectMapper.readValue(response, AccountApiResponse.class);

        // 테스트를 위한 초기 잔액 설정 (입금)
        DepositApiRequest depositRequest = new DepositApiRequest(
            testBankCode,
            testAccountNo,
            new BigDecimal("100000"),
            "초기 잔액"
        );

        mockMvc.perform(post("/transactions/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("입금 API 유효성 검증 - null 은행코드")
    void depositNullBankCodeTest() throws Exception {
        DepositApiRequest invalidRequest = new DepositApiRequest(
                null, // bankCode 누락
                testAccountNo,
                new BigDecimal("10000"),
                "입금"
        );

        mockMvc.perform(post("/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("입금 API 비즈니스 로직 검증 - 존재하지 않는 계좌")
    void depositNonExistentAccountTest() throws Exception {
        DepositApiRequest invalidRequest = new DepositApiRequest(
                "001",
                "9999999999", // 존재하지 않는 계좌
                new BigDecimal("10000"),
                "입금"
        );

        mockMvc.perform(post("/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("입금 API 비즈니스 로직 검증 - 음수 금액")
    void depositNegativeAmountTest() throws Exception {
        DepositApiRequest invalidRequest = new DepositApiRequest(
                testBankCode,
                testAccountNo,
                new BigDecimal("-1000"), // 음수 금액
                "입금"
        );

        mockMvc.perform(post("/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("출금 API 비즈니스 로직 검증 - 0원 출금")
    void withdrawZeroAmountTest() throws Exception {
        WithdrawApiRequest invalidRequest = new WithdrawApiRequest(
                testBankCode,
                testAccountNo,
                BigDecimal.ZERO, // 0원
                "출금"
        );

        mockMvc.perform(post("/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이체 API 유효성 검증 - 동일 계좌 이체")
    void transferSameAccountTest() throws Exception {
        TransferApiRequest invalidRequest = new TransferApiRequest(
                testBankCode,
                testAccountNo,
                testBankCode,
                testAccountNo, // 동일 계좌
                new BigDecimal("10000"),
                "이체"
        );

        mockMvc.perform(post("/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("거래내역 조회 API - 정상 요청")
    void getTransactionHistoryTest() throws Exception {
        mockMvc.perform(get("/transactions/account/" + testBankCode + "/" + testAccountNo)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountInfo").exists())
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.pageInfo").exists());
    }

    @Test
    @DisplayName("거래내역 조회 API - 날짜 범위 지정")
    void getTransactionHistoryWithDateRangeTest() throws Exception {
        mockMvc.perform(get("/transactions/account/" + testBankCode + "/" + testAccountNo)
                        .param("page", "0")
                        .param("size", "10")
                        .param("startDate", "2024-01-01T00:00:00")
                        .param("endDate", "2024-12-31T23:59:59"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("거래내역 조회 API - 잘못된 날짜 형식")
    void getTransactionHistoryInvalidDateFormatTest() throws Exception {
        mockMvc.perform(get("/transactions/account/" + testBankCode + "/" + testAccountNo)
                        .param("startDate", "invalid-date-format"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("거래내역 조회 API - 존재하지 않는 계좌")
    void getTransactionHistoryNonExistentAccountTest() throws Exception {
        mockMvc.perform(get("/transactions/account/001/9999999999")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("입금 API Content-Type 검증")
    void depositContentTypeTest() throws Exception {
        DepositApiRequest validRequest = new DepositApiRequest(
                testBankCode,
                testAccountNo,
                new BigDecimal("10000"),
                "입금"
        );

        // JSON이 아닌 Content-Type으로 요청
        mockMvc.perform(post("/transactions/deposit")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("존재하지 않는 엔드포인트 호출")
    void nonExistentEndpointTest() throws Exception {
        mockMvc.perform(get("/transactions/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("HTTP 메서드 오류")
    void wrongHttpMethodTest() throws Exception {
        // POST 엔드포인트에 GET 요청
        mockMvc.perform(get("/transactions/deposit"))
                .andExpect(status().isMethodNotAllowed());
    }
}