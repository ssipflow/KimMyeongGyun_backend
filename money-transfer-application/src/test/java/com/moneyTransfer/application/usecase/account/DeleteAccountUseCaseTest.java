package com.moneyTransfer.application.usecase.account;

import com.moneyTransfer.common.constant.ErrorMessages;
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
import static org.mockito.ArgumentMatchers.eq;
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
    private static final String TEST_BANK_CODE = "001";
    private static final String TEST_ACCOUNT_NO = "123-456-789";
    private static final String TEST_ACCOUNT_NO_NORM = "123456789";

    @BeforeEach
    void setUp() {
        mockAccount = new Account();
        mockAccount.setId(1L);
        mockAccount.setUserId(1L);
        mockAccount.setBankCode(TEST_BANK_CODE);
        mockAccount.setAccountNo(TEST_ACCOUNT_NO);
        mockAccount.setAccountNoNorm(TEST_ACCOUNT_NO_NORM);
        mockAccount.setBalance(BigDecimal.ZERO);
        mockAccount.setStatus(AccountStatus.ACTIVATE);
    }

    @Test
    @DisplayName("정상적인 계좌 삭제가 성공한다 - bankCode와 accountNo 사용")
    void deleteAccountByBankCodeAndAccountNo_Success() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq(TEST_BANK_CODE), eq(TEST_ACCOUNT_NO_NORM)))
                .willReturn(Optional.of(mockAccount));

        // when
        deleteAccountUseCase.execute(TEST_BANK_CODE, TEST_ACCOUNT_NO);

        // then
        then(accountPort).should().findByBankCodeAndAccountNoNorm(TEST_BANK_CODE, TEST_ACCOUNT_NO_NORM);
        then(accountPort).should().save(any(Account.class));
    }

    @Test
    @DisplayName("존재하지 않는 계좌 삭제 시 실패한다 - bankCode와 accountNo 사용")
    void deleteAccountByBankCodeAndAccountNo_AccountNotFound_ThrowsException() {
        // given
        given(accountPort.findByBankCodeAndAccountNoNorm(eq(TEST_BANK_CODE), eq(TEST_ACCOUNT_NO_NORM)))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deleteAccountUseCase.execute(TEST_BANK_CODE, TEST_ACCOUNT_NO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(ErrorMessages.ACCOUNT_NOT_FOUND);

        then(accountPort).should().findByBankCodeAndAccountNoNorm(TEST_BANK_CODE, TEST_ACCOUNT_NO_NORM);
        then(accountPort).should(never()).save(any(Account.class));
    }

    @Test
    @DisplayName("이미 비활성화된 계좌 삭제 시 실패한다 - bankCode와 accountNo 사용")
    void deleteAccountByBankCodeAndAccountNo_AlreadyDeactivated_ThrowsException() {
        // given
        mockAccount.deactivate();  // 이미 비활성화
        given(accountPort.findByBankCodeAndAccountNoNorm(eq(TEST_BANK_CODE), eq(TEST_ACCOUNT_NO_NORM)))
                .willReturn(Optional.of(mockAccount));

        // when & then
        assertThatThrownBy(() -> deleteAccountUseCase.execute(TEST_BANK_CODE, TEST_ACCOUNT_NO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ErrorMessages.ACCOUNT_ALREADY_DEACTIVATED);

        then(accountPort).should().findByBankCodeAndAccountNoNorm(TEST_BANK_CODE, TEST_ACCOUNT_NO_NORM);
        then(accountPort).should(never()).save(any(Account.class));
    }

    @Test
    @DisplayName("잔액이 있는 계좌 삭제 시 실패한다 - bankCode와 accountNo 사용")
    void deleteAccountByBankCodeAndAccountNo_HasBalance_ThrowsException() {
        // given
        mockAccount.deposit(BigDecimal.valueOf(10000));  // 잔액 추가
        given(accountPort.findByBankCodeAndAccountNoNorm(eq(TEST_BANK_CODE), eq(TEST_ACCOUNT_NO_NORM)))
                .willReturn(Optional.of(mockAccount));

        // when & then
        assertThatThrownBy(() -> deleteAccountUseCase.execute(TEST_BANK_CODE, TEST_ACCOUNT_NO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(ErrorMessages.ACCOUNT_HAS_BALANCE);

        then(accountPort).should().findByBankCodeAndAccountNoNorm(TEST_BANK_CODE, TEST_ACCOUNT_NO_NORM);
        then(accountPort).should(never()).save(any(Account.class));
    }

    @Test
    @DisplayName("계좌번호 정규화 테스트 - 하이픈 포함된 계좌번호")
    void deleteAccount_AccountNumberNormalization() {
        // given
        String accountNoWithHyphens = "123-456-789";
        given(accountPort.findByBankCodeAndAccountNoNorm(eq(TEST_BANK_CODE), eq("123456789")))
                .willReturn(Optional.of(mockAccount));

        // when
        deleteAccountUseCase.execute(TEST_BANK_CODE, accountNoWithHyphens);

        // then - 정규화된 계좌번호로 조회되었는지 확인
        then(accountPort).should().findByBankCodeAndAccountNoNorm(TEST_BANK_CODE, "123456789");
        then(accountPort).should().save(any(Account.class));
    }

    @Test
    @DisplayName("다양한 계좌번호 형식 정규화 테스트")
    void deleteAccount_VariousAccountNumberFormats() {
        // given
        String accountNoWithSpaces = "123 456 789";
        given(accountPort.findByBankCodeAndAccountNoNorm(eq(TEST_BANK_CODE), eq("123456789")))
                .willReturn(Optional.of(mockAccount));

        // when
        deleteAccountUseCase.execute(TEST_BANK_CODE, accountNoWithSpaces);

        // then - 정규화된 계좌번호로 조회되었는지 확인
        then(accountPort).should().findByBankCodeAndAccountNoNorm(TEST_BANK_CODE, "123456789");
        then(accountPort).should().save(any(Account.class));
    }
}