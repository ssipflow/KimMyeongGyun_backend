package com.moneyTransfer.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneyTransfer.api.dto.request.CreateAccountApiRequest;
import com.moneyTransfer.api.dto.response.AccountApiResponse;
import com.moneyTransfer.persistence.entity.UserJpaEntity;
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


import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AccountController 통합 테스트")
class AccountControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userJpaRepository;

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
    @DisplayName("정상적인 계좌 생성 요청이 성공한다")
    void createAccount_Success() throws Exception {
        // when & then
        String response = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.bankCode").value("001"))
                .andExpect(jsonPath("$.accountNo").value("1123456789"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVATE"))
                .andReturn().getResponse().getContentAsString();

        AccountApiResponse accountResponse = objectMapper.readValue(response, AccountApiResponse.class);
        assertThat(accountResponse.getId()).isNotNull();
        assertThat(accountResponse.getUserId()).isNotNull();
    }

    @Test
    @DisplayName("기존 사용자로 계좌 생성이 성공한다")
    void createAccount_ExistingUser_Success() throws Exception {
        // given - 기존 사용자 생성
        UserJpaEntity existingUser = new UserJpaEntity("홍길동", "test@example.com", "1234567890123", "1234567890123");
        userJpaRepository.save(existingUser);

        // when & then
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(existingUser.getId()));
    }

    @Test
    @DisplayName("잘못된 이메일 형식으로 계좌 생성 시 실패한다")
    void createAccount_InvalidEmail_Fails() throws Exception {
        // given
        CreateAccountApiRequest invalidRequest = new CreateAccountApiRequest(
            "홍길동",
            "invalid-email",
            "1234567890123",
            "001",
            "1123456789"
        );

        // when & then
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("계좌 조회가 성공한다")
    void getAccount_Success() throws Exception {
        // given - 계좌 생성
        String createResponse = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        AccountApiResponse createdAccount = objectMapper.readValue(createResponse, AccountApiResponse.class);

        // when & then
        mockMvc.perform(get("/accounts/" + createdAccount.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdAccount.getId()))
                .andExpect(jsonPath("$.bankCode").value("001"))
                .andExpect(jsonPath("$.accountNo").value("1123456789"));
    }

    @Test
    @DisplayName("존재하지 않는 계좌 조회 시 404를 반환한다")
    void getAccount_NotFound() throws Exception {
        // when & then
        mockMvc.perform(get("/accounts/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("사용자별 계좌 목록 조회가 성공한다")
    void getAccountsByUser_Success() throws Exception {
        // given - 계좌 생성
        String createResponse = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        AccountApiResponse createdAccount = objectMapper.readValue(createResponse, AccountApiResponse.class);

        // when & then
        mockMvc.perform(get("/accounts?userId=" + createdAccount.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(createdAccount.getId()));
    }

    @Test
    @DisplayName("은행코드와 계좌번호로 계좌 조회가 성공한다")
    void getAccountByBankCodeAndAccountNo_Success() throws Exception {
        // given - 계좌 생성
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        // when & then
        mockMvc.perform(get("/accounts?bankCode=001&accountNo=1123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].bankCode").value("001"))
                .andExpect(jsonPath("$[0].accountNo").value("1123456789"));
    }

    @Test
    @DisplayName("필수 파라미터 없이 계좌 조회 시 실패한다")
    void getAccounts_MissingParameters_Fails() throws Exception {
        // when & then
        mockMvc.perform(get("/accounts"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("userId 또는 (bankCode와 accountNo) 파라미터가 필요합니다"));
    }

    @Test
    @DisplayName("계좌 삭제가 성공한다")
    void deleteAccount_Success() throws Exception {
        // given - 계좌 생성
        String createResponse = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        AccountApiResponse createdAccount = objectMapper.readValue(createResponse, AccountApiResponse.class);

        // when & then
        mockMvc.perform(delete("/accounts/" + createdAccount.getId()))
                .andExpect(status().isNoContent());

        // 삭제 후 조회 시 비활성 상태 확인
        mockMvc.perform(get("/accounts/" + createdAccount.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEACTIVATE"));
    }

}