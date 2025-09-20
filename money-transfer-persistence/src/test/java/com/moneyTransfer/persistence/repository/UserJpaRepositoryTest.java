package com.moneyTransfer.persistence.repository;

import com.moneyTransfer.persistence.entity.AccountJpaEntity;
import com.moneyTransfer.persistence.entity.UserJpaEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserJpaRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(UserJpaRepositoryTest.class);

    @Autowired
    private UserJpaRepository userRepository;
    
    @Autowired
    private AccountJpaRepository accountRepository;
    
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("계좌소유자를 저장하고 조회할 수 있다")
    void saveAndFindAccountOwner() {
        // given
        UserJpaEntity user = new UserJpaEntity("홍길동", "test@domain.com", "1234567890123", "1234567890123");
        
        // when
        UserJpaEntity savedUser = userRepository.save(user);
        
        // 저장된 엔티티 정보 로깅
        log.info("Saved User: id={}, name={}, email={}, idCardNo={}, idCardNoNorm={}, createdAt={}",
                savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.getIdCardNo(),
                savedUser.getIdCardNoNorm(), savedUser.getCreatedAt());
        
        // then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("홍길동");
        assertThat(savedUser.getIdCardNoNorm()).isEqualTo("1234567890123");
    }

    @Test
    @DisplayName("주민번호 정규화로 계좌소유자를 조회할 수 있다")
    void findByIdCardNoNorm() {
        // given
        UserJpaEntity user = new UserJpaEntity("홍길동", "test@doamin.com", "123-456-7890123", "1234567890123");
        userRepository.save(user);
        
        // when
        Optional<UserJpaEntity> found = userRepository.findByIdCardNoNorm("1234567890123");

        // 로드한 엔티티 정보 로깅
        found.ifPresent(o -> log.info("Found User: id={}, name={}, email={}, idCardNo={}, idCardNoNorm={}, createdAt={}",
                o.getId(), o.getName(), o.getEmail(), o.getIdCardNo(),
                o.getIdCardNoNorm(), o.getCreatedAt()));
        
        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("홍길동");
        assertThat(found.get().getIdCardNo()).isEqualTo("123-456-7890123");
    }

    @Test
    @DisplayName("User와 Account 연관관계는 Account Repository Fetch Join을 통해 조회한다")
    void userAccountRelationshipThroughRepository() {
        // given - User 생성
        UserJpaEntity user1 = new UserJpaEntity("홍길동", "test1@domain.com", "1111111111111", "1111111111111");
        UserJpaEntity savedUser1 = userRepository.save(user1);

        // User의 계좌들 생성
        AccountJpaEntity account1 = new AccountJpaEntity(savedUser1, "001", "123456789", "123456789");
        AccountJpaEntity account2 = new AccountJpaEntity(savedUser1, "002", "987654321", "987654321");
        accountRepository.save(account1);
        accountRepository.save(account2);

        // when - AccountRepository를 통해 User의 계좌 조회
        List<AccountJpaEntity> userAccounts = accountRepository.findByUserIdWithUser(savedUser1.getId());

        // then - 검증
        assertThat(userAccounts).hasSize(2);
        userAccounts.forEach(account -> {
            assertThat(account.getUser().getId()).isEqualTo(savedUser1.getId());
            log.info("Account: bankCode={}, accountNo={}, user={}",
                    account.getBankCode(), account.getAccountNo(), account.getUser().getName());
        });
    }

    @Test
    @DisplayName("주민번호 정규화로 계좌소유자 존재 여부를 확인할 수 있다")
    void existsByIdCardNoNorm() {
        // given
        UserJpaEntity user = new UserJpaEntity("홍길동", "test@domain.com", "1234567890123", "1234567890123");
        userRepository.save(user);
        
        // when & then
        assertThat(userRepository.existsByIdCardNoNorm("1234567890123")).isTrue();
        assertThat(userRepository.existsByIdCardNoNorm("9999999999999")).isFalse();
    }

    @Test
    @DisplayName("Email로 계좌소유자 존재 여부를 확인할 수 있다")
    void existsByEmail() {
        // given
        UserJpaEntity user = new UserJpaEntity("홍길동", "test@domain.com", "1234567890123", "1234567890123");
        userRepository.save(user);

        // when & then
        assertThat(userRepository.existsByEmail("test@domain.com")).isTrue();
        assertThat(userRepository.existsByEmail("test@false.com")).isFalse();
    }
}