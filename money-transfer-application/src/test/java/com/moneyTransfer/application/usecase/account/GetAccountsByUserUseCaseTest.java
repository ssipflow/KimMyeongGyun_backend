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

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAccountsByUserUseCase 테스트")
class GetAccountsByUserUseCaseTest {

    @Mock
    private AccountPort accountPort;

    @InjectMocks
    private GetAccountsByUserUseCase getAccountsByUserUseCase;

    private Account mockAccount1;
    private Account mockAccount2;

    @BeforeEach
    void setUp() {
        mockAccount1 = new Account();
        mockAccount1.setId(1L);
        mockAccount1.setUserId(1L);
        mockAccount1.setBankCode("001");
        mockAccount1.setAccountNo("1123456789");
        mockAccount1.setAccountNoNorm("1123456789");

        mockAccount2 = new Account();
        mockAccount2.setId(2L);
        mockAccount2.setUserId(1L);
        mockAccount2.setBankCode("002");
        mockAccount2.setAccountNo("9876543211");
        mockAccount2.setAccountNoNorm("9876543211");
    }

    @Test
    @DisplayName("사용자의 계좌 목록 조회가 성공한다")
    void getAccountsByUser_Success() {
        // given
        List<Account> mockAccounts = Arrays.asList(mockAccount1, mockAccount2);
        given(accountPort.findByUserId(1L)).willReturn(mockAccounts);

        // when
        List<AccountResponse> result = getAccountsByUserUseCase.execute(1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getBankCode()).isEqualTo("001");
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getBankCode()).isEqualTo("002");

        then(accountPort).should().findByUserId(1L);
    }

    @Test
    @DisplayName("계좌가 없는 사용자 조회 시 빈 목록을 반환한다")
    void getAccountsByUser_NoAccounts_ReturnsEmptyList() {
        // given
        given(accountPort.findByUserId(999L)).willReturn(List.of());

        // when
        List<AccountResponse> result = getAccountsByUserUseCase.execute(999L);

        // then
        assertThat(result).isEmpty();

        then(accountPort).should().findByUserId(999L);
    }
}