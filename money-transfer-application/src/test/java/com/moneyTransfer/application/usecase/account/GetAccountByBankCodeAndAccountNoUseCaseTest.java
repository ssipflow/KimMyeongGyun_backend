package com.moneyTransfer.application.usecase.account;

import com.moneyTransfer.application.dto.account.AccountResponse;
import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAccountByBankCodeAndAccountNoUseCase 테스트")
class GetAccountByBankCodeAndAccountNoUseCaseTest {

    @Mock
    private AccountPort accountPort;

    @InjectMocks
    private GetAccountByBankCodeAndAccountNoUseCase getAccountByBankCodeAndAccountNoUseCase;

    private Account mockAccount;

    @BeforeEach
    void setUp() {
        mockAccount = new Account();
        mockAccount.setId(1L);
        mockAccount.setUserId(1L);
        mockAccount.setBankCode("001");
        mockAccount.setAccountNo("1123456789");
        mockAccount.setAccountNoNorm("1123456789");
    }

    @Test
    @DisplayName("은행코드와 계좌번호로 계좌 조회가 성공한다")
    void getAccountByBankCodeAndAccountNo_Success() {
        // given
        String bankCode = "001";
        String accountNo = "1123-456-789";
        String normalizedAccountNo = "1123456789";

        given(accountPort.findByBankCodeAndAccountNoNorm(bankCode, normalizedAccountNo))
            .willReturn(Optional.of(mockAccount));

        // when
        Optional<AccountResponse> result = getAccountByBankCodeAndAccountNoUseCase.execute(bankCode, accountNo);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getBankCode()).isEqualTo("001");

        then(accountPort).should().findByBankCodeAndAccountNoNorm(bankCode, normalizedAccountNo);
    }

    @Test
    @DisplayName("특수문자가 포함된 계좌번호도 정규화되어 조회된다")
    void getAccountByBankCodeAndAccountNo_WithSpecialCharacters_Success() {
        // given
        String bankCode = "001";
        String accountNoWithSpecialChars = "1123-456-789";
        String normalizedAccountNo = "1123456789";

        given(accountPort.findByBankCodeAndAccountNoNorm(bankCode, normalizedAccountNo))
            .willReturn(Optional.of(mockAccount));

        // when
        Optional<AccountResponse> result = getAccountByBankCodeAndAccountNoUseCase
            .execute(bankCode, accountNoWithSpecialChars);

        // then
        assertThat(result).isPresent();

        then(accountPort).should().findByBankCodeAndAccountNoNorm(bankCode, normalizedAccountNo);
    }

    @Test
    @DisplayName("존재하지 않는 계좌 조회 시 빈 결과를 반환한다")
    void getAccountByBankCodeAndAccountNo_NotFound_ReturnsEmpty() {
        // given
        String bankCode = "999";
        String accountNo = "999-999-999";
        String normalizedAccountNo = "999999999";

        given(accountPort.findByBankCodeAndAccountNoNorm(bankCode, normalizedAccountNo))
            .willReturn(Optional.empty());

        // when
        Optional<AccountResponse> result = getAccountByBankCodeAndAccountNoUseCase.execute(bankCode, accountNo);

        // then
        assertThat(result).isEmpty();

        then(accountPort).should().findByBankCodeAndAccountNoNorm(bankCode, normalizedAccountNo);
    }
}