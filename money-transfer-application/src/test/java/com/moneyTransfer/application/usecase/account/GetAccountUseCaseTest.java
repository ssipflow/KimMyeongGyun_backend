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
@DisplayName("GetAccountUseCase 테스트")
class GetAccountUseCaseTest {

    @Mock
    private AccountPort accountPort;

    @InjectMocks
    private GetAccountUseCase getAccountUseCase;

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
    @DisplayName("계좌 조회가 성공한다")
    void getAccount_Success() {
        // given
        given(accountPort.findById(1L)).willReturn(Optional.of(mockAccount));

        // when
        Optional<AccountResponse> result = getAccountUseCase.execute(1L);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getBankCode()).isEqualTo("001");

        then(accountPort).should().findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 계좌 조회 시 빈 결과를 반환한다")
    void getAccount_NotFound_ReturnsEmpty() {
        // given
        given(accountPort.findById(999L)).willReturn(Optional.empty());

        // when
        Optional<AccountResponse> result = getAccountUseCase.execute(999L);

        // then
        assertThat(result).isEmpty();

        then(accountPort).should().findById(999L);
    }
}