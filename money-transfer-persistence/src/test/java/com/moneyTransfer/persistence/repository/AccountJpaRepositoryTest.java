package com.moneyTransfer.persistence.repository;

import com.moneyTransfer.persistence.entity.AccountJpaEntity;
import com.moneyTransfer.persistence.entity.UserJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AccountJpaRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(AccountJpaRepositoryTest.class);

    @Autowired
    private AccountJpaRepository accountRepository;
    
    @Autowired
    private UserJpaRepository userJpaRepository;
    
    private UserJpaEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserJpaEntity("홍길동", "1234567890123", "1234567890123");
        testUser = userJpaRepository.save(testUser);
    }

    @Test
    @DisplayName("계좌를 저장하고 조회할 수 있다")
    void saveAndFindAccount() {
        // given
        AccountJpaEntity account = new AccountJpaEntity(testUser, "001", "123456789", "123456789");
        
        // when
        AccountJpaEntity savedAccount = accountRepository.save(account);

        // 저장된 엔티티 정보 로깅
        log.info("Saved Account: id={}, bankCode={}, accountNo={}, accountNoNorm={}, balance={}, status={}, createdAt={}",
                savedAccount.getId(), savedAccount.getBankCode(), savedAccount.getAccountNo(),
                savedAccount.getAccountNoNorm(), savedAccount.getBalance(),
                savedAccount.getStatus(), savedAccount.getCreatedAt());
        
        // then
        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getBankCode()).isEqualTo("001");
        assertThat(savedAccount.getAccountNo()).isEqualTo("123456789");
        assertThat(savedAccount.getBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(savedAccount.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("계좌와 소유자 정보를 함께 조회할 수 있다")
    void findByIdWithOwner() {
        // given
        AccountJpaEntity account = new AccountJpaEntity(testUser, "001", "123456789", "123456789");
        AccountJpaEntity savedAccount = accountRepository.save(account);
        
        // when
        Optional<AccountJpaEntity> found = accountRepository.findByIdWithOwner(savedAccount.getId());

        // 로드한 엔티티 정보 로깅
        found.ifPresent(o -> log.info("Found Account: id={}, bankCode={}, accountNo={}, accountNoNorm={}, balance={}, status={}, createdAt={}, ownerName={}",
                o.getId(), o.getBankCode(), o.getAccountNo(),
                o.getAccountNoNorm(), o.getBalance(),
                o.getStatus(), o.getCreatedAt(),
                o.getUser().getName()));
        
        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getName()).isEqualTo("홍길동");
        assertThat(found.get().getBankCode()).isEqualTo("001");
    }

    @Test
    @DisplayName("소유자별 계좌 목록을 조회할 수 있다")
    void findByOwnerIdWithOwner() {
        // given
        AccountJpaEntity account1 = new AccountJpaEntity(testUser, "001", "123456789", "123456789");
        AccountJpaEntity account2 = new AccountJpaEntity(testUser, "002", "987654321", "987654321");
        accountRepository.save(account1);
        accountRepository.save(account2);
        
        // when
        List<AccountJpaEntity> accounts = accountRepository.findByOwnerIdWithOwner(testUser.getId());

        // 로드한 엔티티 정보 로깅
        accounts.forEach(o -> log.info("Found Account: id={}, bankCode={}, accountNo={}, accountNoNorm={}, balance={}, status={}, createdAt={}, ownerName={}",
                o.getId(), o.getBankCode(), o.getAccountNo(),
                o.getAccountNoNorm(), o.getBalance(),
                o.getStatus(), o.getCreatedAt(),
                o.getUser().getName()));
        
        // then
        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(AccountJpaEntity::getBankCode)
                .containsExactlyInAnyOrder("001", "002");
        // Fetch Join 확인
        assertThat(accounts.get(0).getUser().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("복합 유니크 키로 계좌를 조회할 수 있다")
    void findByOwnerIdAndBankCodeAndAccountNoNorm() {
        // given
        AccountJpaEntity account = new AccountJpaEntity(testUser, "001", "123-456-789", "123456789");
        accountRepository.save(account);
        
        // when
        Optional<AccountJpaEntity> found = accountRepository.findByUserIdAndBankCodeAndAccountNoNorm(
                testUser.getId(), "001", "123456789");

        // 로드한 엔티티 정보 로깅
        found.ifPresent(o -> log.info("Found Account: id={}, bankCode={}, accountNo={}, accountNoNorm={}, balance={}, status={}, createdAt={}, ownerName={}",
                o.getId(), o.getBankCode(), o.getAccountNo(),
                o.getAccountNoNorm(), o.getBalance(),
                o.getStatus(), o.getCreatedAt(),
                o.getUser().getName()));
        
        // then
        assertThat(found).isPresent();
        assertThat(found.get().getAccountNo()).isEqualTo("123-456-789");
        assertThat(found.get().getAccountNoNorm()).isEqualTo("123456789");
    }

    @Test
    @DisplayName("계좌 중복 여부를 확인할 수 있다")
    void existsByOwnerIdAndBankCodeAndAccountNoNorm() {
        // given
        AccountJpaEntity account = new AccountJpaEntity(testUser, "001", "123456789", "123456789");
        accountRepository.save(account);
        
        // when & then
        assertThat(accountRepository.existsByUserIdAndBankCodeAndAccountNoNorm(
                testUser.getId(), "001", "123456789")).isTrue();
        assertThat(accountRepository.existsByUserIdAndBankCodeAndAccountNoNorm(
                testUser.getId(), "001", "999999999")).isFalse();
    }

    @Test
    @DisplayName("계좌 상태별로 계좌를 조회할 수 있다")
    void findByStatus() {
        // given
        AccountJpaEntity activeAccount = new AccountJpaEntity(testUser, "001", "123456789", "123456789");
        AccountJpaEntity deactiveAccount = new AccountJpaEntity(testUser, "002", "987654321", "987654321");
        deactiveAccount.setStatus(400); // DEACTIVATED
        
        accountRepository.save(activeAccount);
        accountRepository.save(deactiveAccount);
        
        // when
        List<AccountJpaEntity> activeAccounts = accountRepository.findByStatus(200);
        List<AccountJpaEntity> deactiveAccounts = accountRepository.findByStatus(400);

        // 로드한 activeAccounts 엔티티 정보 로깅
        activeAccounts.forEach(o -> log.info("Active Account: id={}, bankCode={}, accountNo={}, status={}",
                o.getId(), o.getBankCode(), o.getAccountNo(), o.getStatus()));

        // 로드한 deactiveAccounts 엔티티 정보 로깅
        deactiveAccounts.forEach(o -> log.info("Deactivated Account: id={}, bankCode={}, accountNo={}, status={}",
                o.getId(), o.getBankCode(), o.getAccountNo(), o.getStatus()));
        
        // then
        assertThat(activeAccounts).hasSize(1);
        assertThat(activeAccounts.get(0).getBankCode()).isEqualTo("001");
        
        assertThat(deactiveAccounts).hasSize(1);
        assertThat(deactiveAccounts.get(0).getBankCode()).isEqualTo("002");
    }
}