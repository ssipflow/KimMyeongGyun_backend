package com.moneyTransfer.api.mapper;

import com.moneyTransfer.api.dto.request.CreateAccountApiRequest;
import com.moneyTransfer.api.dto.response.AccountApiResponse;
import com.moneyTransfer.application.dto.account.AccountResponse;
import com.moneyTransfer.application.dto.account.CreateAccountRequest;
import com.moneyTransfer.domain.account.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AccountDtoMapper 테스트")
class AccountDtoMapperTest {

    private AccountDtoMapper accountDtoMapper;

    @BeforeEach
    void setUp() {
        accountDtoMapper = new AccountDtoMapper();
    }

    @Test
    @DisplayName("CreateAccountApiRequest를 CreateAccountRequest로 변환")
    void toApplicationRequestFromCreateAccountApiRequest() {
        // given
        CreateAccountApiRequest apiRequest = new CreateAccountApiRequest(
                "홍길동",
                "hong@example.com",
                "1234567890123",
                "001",
                "123-456-789"
        );

        // when
        CreateAccountRequest applicationRequest = accountDtoMapper.toApplicationRequest(apiRequest);

        // then
        assertThat(applicationRequest.getUserName()).isEqualTo("홍길동");
        assertThat(applicationRequest.getEmail()).isEqualTo("hong@example.com");
        assertThat(applicationRequest.getIdCardNo()).isEqualTo("1234567890123");
        assertThat(applicationRequest.getBankCode()).isEqualTo("001");
        assertThat(applicationRequest.getAccountNo()).isEqualTo("123-456-789");
    }

    @Test
    @DisplayName("AccountResponse를 AccountApiResponse로 변환 - 활성 계좌")
    void toApiResponseFromAccountResponseActive() {
        // given
        AccountResponse applicationResponse = new AccountResponse();
        applicationResponse.setId(1L);
        applicationResponse.setUserId(100L);
        applicationResponse.setBankCode("001");
        applicationResponse.setAccountNo("123-456-789");
        applicationResponse.setBalance(new BigDecimal("50000"));
        applicationResponse.setStatus(AccountStatus.ACTIVATE);
        applicationResponse.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        applicationResponse.setDeactivatedAt(null);

        // when
        AccountApiResponse apiResponse = accountDtoMapper.toApiResponse(applicationResponse);

        // then
        assertThat(apiResponse.getId()).isEqualTo(1L);
        assertThat(apiResponse.getUserId()).isEqualTo(100L);
        assertThat(apiResponse.getBankCode()).isEqualTo("001");
        assertThat(apiResponse.getAccountNo()).isEqualTo("123-456-789");
        assertThat(apiResponse.getBalance()).isEqualTo(new BigDecimal("50000"));
        assertThat(apiResponse.getStatus()).isEqualTo("ACTIVATE");
        assertThat(apiResponse.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        assertThat(apiResponse.getDeactivatedAt()).isNull();
    }

    @Test
    @DisplayName("AccountResponse를 AccountApiResponse로 변환 - 비활성 계좌")
    void toApiResponseFromAccountResponseDeactivated() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        LocalDateTime deactivatedAt = LocalDateTime.of(2024, 6, 1, 15, 30, 0);

        AccountResponse applicationResponse = new AccountResponse();
        applicationResponse.setId(2L);
        applicationResponse.setUserId(200L);
        applicationResponse.setBankCode("002");
        applicationResponse.setAccountNo("987-654-321");
        applicationResponse.setBalance(BigDecimal.ZERO);
        applicationResponse.setStatus(AccountStatus.DEACTIVATE);
        applicationResponse.setCreatedAt(createdAt);
        applicationResponse.setDeactivatedAt(deactivatedAt);

        // when
        AccountApiResponse apiResponse = accountDtoMapper.toApiResponse(applicationResponse);

        // then
        assertThat(apiResponse.getId()).isEqualTo(2L);
        assertThat(apiResponse.getUserId()).isEqualTo(200L);
        assertThat(apiResponse.getBankCode()).isEqualTo("002");
        assertThat(apiResponse.getAccountNo()).isEqualTo("987-654-321");
        assertThat(apiResponse.getBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(apiResponse.getStatus()).isEqualTo("DEACTIVATE");
        assertThat(apiResponse.getCreatedAt()).isEqualTo(createdAt);
        assertThat(apiResponse.getDeactivatedAt()).isEqualTo(deactivatedAt);
    }

    @Test
    @DisplayName("AccountResponse 리스트를 AccountApiResponse 리스트로 변환")
    void toApiResponseListFromAccountResponseList() {
        // given
        AccountResponse response1 = new AccountResponse();
        response1.setId(1L);
        response1.setUserId(100L);
        response1.setBankCode("001");
        response1.setAccountNo("123-456-789");
        response1.setBalance(new BigDecimal("50000"));
        response1.setStatus(AccountStatus.ACTIVATE);
        response1.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        response1.setDeactivatedAt(null);

        AccountResponse response2 = new AccountResponse();
        response2.setId(2L);
        response2.setUserId(200L);
        response2.setBankCode("002");
        response2.setAccountNo("987-654-321");
        response2.setBalance(new BigDecimal("100000"));
        response2.setStatus(AccountStatus.ACTIVATE);
        response2.setCreatedAt(LocalDateTime.of(2024, 1, 2, 11, 0, 0));
        response2.setDeactivatedAt(null);

        List<AccountResponse> applicationResponses = List.of(response1, response2);

        // when
        List<AccountApiResponse> apiResponses = accountDtoMapper.toApiResponseList(applicationResponses);

        // then
        assertThat(apiResponses).hasSize(2);

        AccountApiResponse firstResponse = apiResponses.get(0);
        assertThat(firstResponse.getId()).isEqualTo(1L);
        assertThat(firstResponse.getUserId()).isEqualTo(100L);
        assertThat(firstResponse.getBankCode()).isEqualTo("001");
        assertThat(firstResponse.getAccountNo()).isEqualTo("123-456-789");
        assertThat(firstResponse.getBalance()).isEqualTo(new BigDecimal("50000"));
        assertThat(firstResponse.getStatus()).isEqualTo("ACTIVATE");

        AccountApiResponse secondResponse = apiResponses.get(1);
        assertThat(secondResponse.getId()).isEqualTo(2L);
        assertThat(secondResponse.getUserId()).isEqualTo(200L);
        assertThat(secondResponse.getBankCode()).isEqualTo("002");
        assertThat(secondResponse.getAccountNo()).isEqualTo("987-654-321");
        assertThat(secondResponse.getBalance()).isEqualTo(new BigDecimal("100000"));
        assertThat(secondResponse.getStatus()).isEqualTo("ACTIVATE");
    }

    @Test
    @DisplayName("빈 리스트 변환")
    void toApiResponseListFromEmptyList() {
        // given
        List<AccountResponse> emptyList = List.of();

        // when
        List<AccountApiResponse> apiResponses = accountDtoMapper.toApiResponseList(emptyList);

        // then
        assertThat(apiResponses).isEmpty();
    }

    @Test
    @DisplayName("특수 문자가 포함된 데이터 변환")
    void toApplicationRequestWithSpecialCharacters() {
        // given
        CreateAccountApiRequest apiRequest = new CreateAccountApiRequest(
                "김철수-test",  // 하이픈 포함
                "kim.test+1@example.com", // 특수문자 포함 이메일
                "9876543210987",
                "999",
                "000-111-222"
        );

        // when
        CreateAccountRequest applicationRequest = accountDtoMapper.toApplicationRequest(apiRequest);

        // then
        assertThat(applicationRequest.getUserName()).isEqualTo("김철수-test");
        assertThat(applicationRequest.getEmail()).isEqualTo("kim.test+1@example.com");
        assertThat(applicationRequest.getIdCardNo()).isEqualTo("9876543210987");
        assertThat(applicationRequest.getBankCode()).isEqualTo("999");
        assertThat(applicationRequest.getAccountNo()).isEqualTo("000-111-222");
    }

    @Test
    @DisplayName("큰 잔액 처리")
    void toApiResponseWithLargeBalance() {
        // given
        BigDecimal largeBalance = new BigDecimal("999999999999.99");
        AccountResponse applicationResponse = new AccountResponse();
        applicationResponse.setId(1L);
        applicationResponse.setUserId(100L);
        applicationResponse.setBankCode("001");
        applicationResponse.setAccountNo("123-456-789");
        applicationResponse.setBalance(largeBalance);
        applicationResponse.setStatus(AccountStatus.ACTIVATE);
        applicationResponse.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        applicationResponse.setDeactivatedAt(null);

        // when
        AccountApiResponse apiResponse = accountDtoMapper.toApiResponse(applicationResponse);

        // then
        assertThat(apiResponse.getBalance()).isEqualTo(largeBalance);
        assertThat(apiResponse.getBalance().toString()).isEqualTo("999999999999.99");
    }

    @Test
    @DisplayName("경계값 ID 처리")
    void toApiResponseWithBoundaryIds() {
        // given
        AccountResponse applicationResponse = new AccountResponse();
        applicationResponse.setId(Long.MAX_VALUE);
        applicationResponse.setUserId(Long.MAX_VALUE);
        applicationResponse.setBankCode("001");
        applicationResponse.setAccountNo("123-456-789");
        applicationResponse.setBalance(BigDecimal.ZERO);
        applicationResponse.setStatus(AccountStatus.ACTIVATE);
        applicationResponse.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        applicationResponse.setDeactivatedAt(null);

        // when
        AccountApiResponse apiResponse = accountDtoMapper.toApiResponse(applicationResponse);

        // then
        assertThat(apiResponse.getId()).isEqualTo(Long.MAX_VALUE);
        assertThat(apiResponse.getUserId()).isEqualTo(Long.MAX_VALUE);
    }
}