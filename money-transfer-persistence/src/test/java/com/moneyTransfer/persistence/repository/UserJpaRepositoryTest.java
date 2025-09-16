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
    @DisplayName("BatchSize를 통한 계좌 Lazy Loading 테스트")
    void lazyLoadingWithBatchSize() {
        // given - 여러 User와 Account를 생성해서 BatchSize 효과 확인
        UserJpaEntity user1 = new UserJpaEntity("홍길동", "test1@domain.com", "1111111111111", "1111111111111");
        UserJpaEntity user2 = new UserJpaEntity("김철수", "test2@domain.com", "2222222222222", "2222222222222");
        UserJpaEntity savedUser1 = userRepository.save(user1);
        UserJpaEntity savedUser2 = userRepository.save(user2);
        
        // User1의 계좌들
        AccountJpaEntity account1 = new AccountJpaEntity(savedUser1, "001", "123456789", "123456789");
        AccountJpaEntity account2 = new AccountJpaEntity(savedUser1, "002", "987654321", "987654321");
        accountRepository.save(account1);
        accountRepository.save(account2);
        
        // User2의 계좌들
        AccountJpaEntity account3 = new AccountJpaEntity(savedUser2, "001", "111111111", "111111111");
        accountRepository.save(account3);
        
        // 영속성 컨텍스트 초기화 - 실제 DB에서 다시 조회하도록 강제
        entityManager.flush(); // DB에 변경사항 반영
        entityManager.clear(); // 영속성 컨텍스트 초기화
        
        // when - 여러 User 조회 (BatchSize 효과를 위해)
        List<UserJpaEntity> allUsers = userRepository.findAll();
        
        // then - 각 User의 accounts에 접근 (BatchSize로 한번에 로딩되어야 함)
        log.info("=== About to access accounts for all users (should trigger BatchSize loading) ===");
        
        for (UserJpaEntity user : allUsers) {
            List<AccountJpaEntity> accounts = user.getAccounts();
            log.info("User {} has {} accounts", user.getName(), accounts.size());
            accounts.forEach(account ->
                log.info("  - Account: bankCode={}, accountNo={}", account.getBankCode(), account.getAccountNo()));
        }
        
        log.info("=== Finished accessing all accounts ===");
        
        // 검증
        assertThat(allUsers).hasSize(2);
        assertThat(allUsers.get(0).getAccounts()).hasSize(2);
        assertThat(allUsers.get(1).getAccounts()).hasSize(1);
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