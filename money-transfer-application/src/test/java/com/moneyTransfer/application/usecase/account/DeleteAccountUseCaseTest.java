package com.moneyTransfer.application.usecase.account;

import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
import com.moneyTransfer.domain.account.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteAccountUseCase 테스트")
class DeleteAccountUseCaseTest {

    @Mock
    private AccountPort accountPort;

    @InjectMocks
    private DeleteAccountUseCase deleteAccountUseCase;

    private Account mockAccount;

    @BeforeEach
    void setUp() {
        mockAccount = new Account();
        mockAccount.setId(1L);
        mockAccount.setUserId(1L);
        mockAccount.setBankCode("001");
        mockAccount.setAccountNo("1123456789");
        mockAccount.setAccountNoNorm("1123456789");
        mockAccount.setBalance(BigDecimal.ZERO);
        mockAccount.setStatus(com.moneyTransfer.domain.account.AccountStatus.ACTIVATE);
    }

    @Test
    @DisplayName("정상적인 계좌 삭제가 성공한다")
    void deleteAccount_Success() {
        // given
        given(accountPort.findById(1L)).willReturn(Optional.of(mockAccount));

        // when
        deleteAccountUseCase.execute(1L);

        // then
        then(accountPort).should().save(any(Account.class));
    }

    @Test
    @DisplayName("존재하지 않는 계좌 삭제 시 실패한다")
    void deleteAccount_AccountNotFound_ThrowsException() {
        // given
        given(accountPort.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deleteAccountUseCase.execute(1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("계좌를 찾을 수 없습니다");

        then(accountPort).should(never()).save(any(Account.class));
    }

    @Test
    @DisplayName("이미 비활성화된 계좌 삭제 시 실패한다")
    void deleteAccount_AlreadyDeactivated_ThrowsException() {
        // given
        mockAccount.deactivate();  // 이미 비활성화
        given(accountPort.findById(1L)).willReturn(Optional.of(mockAccount));

        // when & then
        assertThatThrownBy(() -> deleteAccountUseCase.execute(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("이미 비활성화된 계좌입니다");

        then(accountPort).should(never()).save(any(Account.class));
    }

    @Test
    @DisplayName("잔액이 있는 계좌 삭제 시 실패한다")
    void deleteAccount_HasBalance_ThrowsException() {
        // given
        mockAccount.deposit(BigDecimal.valueOf(10000));  // 잔액 추가
        given(accountPort.findById(1L)).willReturn(Optional.of(mockAccount));

        // when & then
        assertThatThrownBy(() -> deleteAccountUseCase.execute(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("잔액이 있는 계좌는 삭제할 수 없습니다");

        then(accountPort).should(never()).save(any(Account.class));
    }
}