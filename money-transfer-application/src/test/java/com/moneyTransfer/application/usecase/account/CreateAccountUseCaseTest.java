package com.moneyTransfer.application.usecase.account;

import com.moneyTransfer.application.dto.account.AccountResponse;
import com.moneyTransfer.application.dto.account.CreateAccountRequest;
import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
import com.moneyTransfer.domain.user.User;
import com.moneyTransfer.domain.user.UserPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAccountUseCase 테스트")
class CreateAccountUseCaseTest {

    @Mock
    private AccountPort accountPort;

    @Mock
    private UserPort userPort;

    @InjectMocks
    private CreateAccountUseCase createAccountUseCase;

    private CreateAccountRequest validRequest;
    private User mockUser;
    private Account mockAccount;

    @BeforeEach
    void setUp() {
        validRequest = new CreateAccountRequest(
            "홍길동",
            "test@example.com",
            "1234567890123",
            "001",
            "1123456789"
        );

        // Mock 객체를 도메인 create 메서드로 생성
        mockUser = User.create("홍길동", "test@example.com", "1234567890123");
        mockUser.setId(1L);  // ID는 별도로 설정

        mockAccount = Account.create(1L, "001", "1123456789");
        mockAccount.setId(1L);  // ID는 별도로 설정
    }

    @Test
    @DisplayName("정상적인 계좌 생성이 성공한다 - 새로운 사용자")
    void createAccount_Success_NewUser() {
        // given
        given(userPort.findByIdCardNoNorm("1234567890123")).willReturn(Optional.empty());
        given(userPort.save(any(User.class))).willReturn(mockUser);
        given(accountPort.save(any(Account.class))).willReturn(mockAccount);

        // when
        AccountResponse response = createAccountUseCase.execute(validRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);

        then(userPort).should().findByIdCardNoNorm("1234567890123");
        then(userPort).should().save(any(User.class));
        then(accountPort).should().save(any(Account.class));
    }

    @Test
    @DisplayName("기존 사용자로 계좌 생성이 성공한다")
    void createAccount_Success_ExistingUser() {
        // given
        given(userPort.findByIdCardNoNorm("1234567890123")).willReturn(Optional.of(mockUser));
        given(accountPort.save(any(Account.class))).willReturn(mockAccount);

        // when
        AccountResponse response = createAccountUseCase.execute(validRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);

        then(userPort).should().findByIdCardNoNorm("1234567890123");
        then(userPort).should(never()).save(any(User.class));  // 기존 사용자이므로 저장하지 않음
        then(accountPort).should().save(any(Account.class));
    }

    @Test
    @DisplayName("새로운 사용자 생성 시 중복된 이메일로 실패한다")
    void createAccount_DuplicateEmail_ThrowsException() {
        // given
        given(userPort.findByIdCardNoNorm("1234567890123")).willReturn(Optional.empty());
        given(userPort.save(any(User.class))).willThrow(new DataIntegrityViolationException("UNIQUE constraint email"));

        // when & then
        assertThatThrownBy(() -> createAccountUseCase.execute(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이미 존재하는 이메일입니다");

        then(userPort).should().findByIdCardNoNorm("1234567890123");
        then(userPort).should().save(any(User.class));
        then(accountPort).should(never()).save(any(Account.class));
    }

    @Test
    @DisplayName("기존 사용자와 이름이 다를 때 실패한다")
    void createAccount_ExistingUser_NameMismatch_ThrowsException() {
        // given
        User existingUser = new User();
        existingUser.setName("김철수");  // 다른 이름
        existingUser.setEmail("test@example.com");
        existingUser.setIdCardNoNorm("1234567890123");

        given(userPort.findByIdCardNoNorm("1234567890123")).willReturn(Optional.of(existingUser));

        // when & then
        assertThatThrownBy(() -> createAccountUseCase.execute(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("제공된 사용자명이 기존 정보와 일치하지 않습니다");

        then(userPort).should().findByIdCardNoNorm("1234567890123");
        then(userPort).should(never()).save(any(User.class));
        then(accountPort).should(never()).save(any(Account.class));
    }

    @Test
    @DisplayName("기존 사용자와 이메일이 다를 때 실패한다")
    void createAccount_ExistingUser_EmailMismatch_ThrowsException() {
        // given
        User existingUser = new User();
        existingUser.setName("홍길동");
        existingUser.setEmail("different@example.com");  // 다른 이메일
        existingUser.setIdCardNoNorm("1234567890123");

        given(userPort.findByIdCardNoNorm("1234567890123")).willReturn(Optional.of(existingUser));

        // when & then
        assertThatThrownBy(() -> createAccountUseCase.execute(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("제공된 이메일이 기존 정보와 일치하지 않습니다");

        then(userPort).should().findByIdCardNoNorm("1234567890123");
        then(userPort).should(never()).save(any(User.class));
        then(accountPort).should(never()).save(any(Account.class));
    }

    @Test
    @DisplayName("중복된 계좌번호로 계좌 생성 시 실패한다")
    void createAccount_DuplicateAccountNo_ThrowsException() {
        // given
        given(userPort.findByIdCardNoNorm("1234567890123")).willReturn(Optional.empty());
        given(userPort.save(any(User.class))).willReturn(mockUser);
        given(accountPort.save(any(Account.class))).willThrow(new DataIntegrityViolationException("UNIQUE constraint account"));

        // when & then
        assertThatThrownBy(() -> createAccountUseCase.execute(validRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이미 존재하는 계좌번호입니다");

        then(userPort).should().findByIdCardNoNorm("1234567890123");
        then(userPort).should().save(any(User.class));
        then(accountPort).should().save(any(Account.class));
    }

    @Test
    @DisplayName("잘못된 사용자명으로 계좌 생성 시 실패한다")
    void createAccount_InvalidUserName_ThrowsException() {
        // given
        CreateAccountRequest invalidRequest = new CreateAccountRequest(
            "",  // 빈 이름
            "test@example.com",
            "1234567890123",
            "001",
            "1123456789"
        );

        // when & then
        assertThatThrownBy(() -> createAccountUseCase.execute(invalidRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("사용자명은 필수입니다");
    }

    @Test
    @DisplayName("잘못된 이메일 형식으로 계좌 생성 시 실패한다")
    void createAccount_InvalidEmail_ThrowsException() {
        // given
        CreateAccountRequest invalidRequest = new CreateAccountRequest(
            "홍길동",
            "invalid-email",  // 잘못된 이메일 형식
            "1234567890123",
            "001",
            "123456789"
        );

        // when & then
        assertThatThrownBy(() -> createAccountUseCase.execute(invalidRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("유효한 이메일 형식이 아닙니다");
    }
}