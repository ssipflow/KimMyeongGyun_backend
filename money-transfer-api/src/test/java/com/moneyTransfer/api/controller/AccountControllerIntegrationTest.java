package com.moneyTransfer.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneyTransfer.api.dto.request.CreateAccountApiRequest;
import com.moneyTransfer.api.dto.request.DeleteAccountApiRequest;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import jakarta.persistence.EntityManager;


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

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private CreateAccountApiRequest validRequest;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        long timestamp = System.currentTimeMillis();
        String uniqueEmail = "test" + timestamp + "@example.com";
        String uniqueIdCardNo = "1234567890" + String.format("%03d", timestamp % 1000L);
        validRequest = new CreateAccountApiRequest(
            "홍길동",
            uniqueEmail,
            uniqueIdCardNo,
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
        long timestamp = System.currentTimeMillis();
        String uniqueEmail = "existing" + timestamp + "@example.com";
        String uniqueIdCardNo = "9876543210" + String.format("%03d", timestamp % 1000L);
        UserJpaEntity existingUser = transactionTemplate.execute(status -> {
            UserJpaEntity newUser = new UserJpaEntity("홍길동", uniqueEmail, uniqueIdCardNo, uniqueIdCardNo);
            return userJpaRepository.save(newUser);
        });

        assertThat(existingUser.getId()).isNotNull(); // Verify ID is populated

        // validRequest의 이메일과 주민번호를 기존 사용자와 동일하게 설정
        validRequest.setEmail(uniqueEmail);
        validRequest.setIdCardNo(uniqueIdCardNo);

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
    @DisplayName("계좌 삭제가 성공한다")
    void deleteAccount_Success() throws Exception {
        // given - 계좌 생성
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        DeleteAccountApiRequest deleteRequest = new DeleteAccountApiRequest("001", "1123456789");

        // when & then
        mockMvc.perform(delete("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deleteRequest)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("존재하지 않는 계좌 삭제 시 404를 반환한다")
    void deleteAccount_NotFound() throws Exception {
        // given
        DeleteAccountApiRequest deleteRequest = new DeleteAccountApiRequest("001", "9999999999");

        // when & then
        mockMvc.perform(delete("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deleteRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("중복된 계좌번호로 계좌 생성 시 실패한다")
    void createAccount_DuplicateAccountNo_Fails() throws Exception {
        // given - 첫 번째 계좌 생성
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());

        // when & then - 같은 계좌번호로 두 번째 계좌 생성 시도
        CreateAccountApiRequest duplicateRequest = new CreateAccountApiRequest(
            "김철수",
            "kim@example.com",
            "9876543210987",
            "001",
            "1123456789"  // 동일한 계좌번호
        );

        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict());
    }

}