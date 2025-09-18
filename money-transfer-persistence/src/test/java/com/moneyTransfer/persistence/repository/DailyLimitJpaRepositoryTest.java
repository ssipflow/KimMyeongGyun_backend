package com.moneyTransfer.persistence.repository;

import com.moneyTransfer.persistence.entity.AccountJpaEntity;
import com.moneyTransfer.persistence.entity.DailyLimitJpaEntity;
import com.moneyTransfer.persistence.entity.UserJpaEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DailyLimitJpaRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(DailyLimitJpaRepositoryTest.class);

    @Autowired
    private DailyLimitJpaRepository dailyLimitRepository;

    @Autowired
    private AccountJpaRepository accountRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private AccountJpaEntity testAccount;

    @BeforeEach
    void setUp() {
        UserJpaEntity user = new UserJpaEntity("홍길동", "test@domain.com", "1234567890123", "1234567890123");
        user = userRepository.save(user);

        testAccount = new AccountJpaEntity(user, "001", "123456789", "123456789");
        testAccount = accountRepository.save(testAccount);
    }

    @Test
    @DisplayName("일일 한도를 저장하고 조회할 수 있다")
    void saveAndFindDailyLimit() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimitJpaEntity dailyLimit = new DailyLimitJpaEntity(testAccount, today);
        dailyLimit.setWithdrawUsed(new BigDecimal("50000"));
        dailyLimit.setTransferUsed(new BigDecimal("100000"));

        // when
        DailyLimitJpaEntity savedDailyLimit = dailyLimitRepository.save(dailyLimit);

        // 저장된 엔티티 정보 로깅
        log.info("Saved DailyLimit: id={}, accountId={}, limitDate={}, withdrawUsed={}, transferUsed={}, createdAt={}",
                savedDailyLimit.getId(), savedDailyLimit.getAccount().getId(),
                savedDailyLimit.getLimitDate(), savedDailyLimit.getWithdrawUsed(),
                savedDailyLimit.getTransferUsed(), savedDailyLimit.getCreatedAt());

        // then
        assertThat(savedDailyLimit.getId()).isNotNull();
        assertThat(savedDailyLimit.getAccount().getId()).isEqualTo(testAccount.getId());
        assertThat(savedDailyLimit.getLimitDate()).isEqualTo(today);
        assertThat(savedDailyLimit.getWithdrawUsed()).isEqualTo(new BigDecimal("50000"));
        assertThat(savedDailyLimit.getTransferUsed()).isEqualTo(new BigDecimal("100000"));
    }

    @Test
    @DisplayName("계좌와 날짜로 일일 한도를 조회할 수 있다")
    void findByAccountIdAndLimitDateWithAccount() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimitJpaEntity dailyLimit = new DailyLimitJpaEntity(testAccount, today);
        dailyLimit.setWithdrawUsed(new BigDecimal("30000"));
        dailyLimit.setTransferUsed(new BigDecimal("150000"));
        dailyLimitRepository.save(dailyLimit);

        // when
        Optional<DailyLimitJpaEntity> found = dailyLimitRepository.findByAccountIdAndLimitDateWithAccount(
                testAccount.getId(), today);

        // 로드한 엔티티 정보 로깅
        found.ifPresent(d -> log.info("Found DailyLimit: id={}, accountId={}, limitDate={}, withdrawUsed={}, transferUsed={}, accountOwner={}",
                d.getId(), d.getAccount().getId(), d.getLimitDate(),
                d.getWithdrawUsed(), d.getTransferUsed(), d.getAccount().getUser().getName()));

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getAccount().getId()).isEqualTo(testAccount.getId());
        assertThat(found.get().getLimitDate()).isEqualTo(today);
        assertThat(found.get().getWithdrawUsed()).isEqualTo(new BigDecimal("30000"));
        assertThat(found.get().getTransferUsed()).isEqualTo(new BigDecimal("150000"));
        // Fetch Join 확인
        assertThat(found.get().getAccount().getUser().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("계좌와 날짜로 일일 한도를 비관적 락으로 조회할 수 있다")
    void findByAccountIdAndLimitDateWithAccountAndLock() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimitJpaEntity dailyLimit = new DailyLimitJpaEntity(testAccount, today);
        dailyLimit.setWithdrawUsed(new BigDecimal("70000"));
        dailyLimit.setTransferUsed(new BigDecimal("200000"));
        dailyLimitRepository.save(dailyLimit);

        // when
        Optional<DailyLimitJpaEntity> found = dailyLimitRepository.findByAccountIdAndLimitDateWithAccountAndLock(
                testAccount.getId(), today);

        // 로드한 엔티티 정보 로깅
        found.ifPresent(d -> log.info("Found DailyLimit with Lock: id={}, accountId={}, limitDate={}, withdrawUsed={}, transferUsed={}, version={}",
                d.getId(), d.getAccount().getId(), d.getLimitDate(),
                d.getWithdrawUsed(), d.getTransferUsed(), d.getVersion()));

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getAccount().getId()).isEqualTo(testAccount.getId());
        assertThat(found.get().getLimitDate()).isEqualTo(today);
        assertThat(found.get().getWithdrawUsed()).isEqualTo(new BigDecimal("70000"));
        assertThat(found.get().getTransferUsed()).isEqualTo(new BigDecimal("200000"));
        // Fetch Join 확인
        assertThat(found.get().getAccount().getUser().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("다른 날짜의 일일 한도는 조회되지 않는다")
    void findByAccountIdAndLimitDateShouldNotReturnDifferentDate() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        DailyLimitJpaEntity todayLimit = new DailyLimitJpaEntity(testAccount, today);
        DailyLimitJpaEntity yesterdayLimit = new DailyLimitJpaEntity(testAccount, yesterday);

        dailyLimitRepository.save(todayLimit);
        dailyLimitRepository.save(yesterdayLimit);

        // when
        Optional<DailyLimitJpaEntity> todayFound = dailyLimitRepository.findByAccountIdAndLimitDateWithAccount(
                testAccount.getId(), today);
        Optional<DailyLimitJpaEntity> yesterdayFound = dailyLimitRepository.findByAccountIdAndLimitDateWithAccount(
                testAccount.getId(), yesterday);

        // then
        assertThat(todayFound).isPresent();
        assertThat(todayFound.get().getLimitDate()).isEqualTo(today);

        assertThat(yesterdayFound).isPresent();
        assertThat(yesterdayFound.get().getLimitDate()).isEqualTo(yesterday);
    }

    @Test
    @DisplayName("다른 계좌의 일일 한도는 조회되지 않는다")
    void findByAccountIdAndLimitDateShouldNotReturnOtherAccount() {
        // given
        UserJpaEntity anotherUser = new UserJpaEntity("김철수", "test2@domain.com", "9876543210987", "9876543210987");
        anotherUser = userRepository.save(anotherUser);
        AccountJpaEntity anotherAccount = new AccountJpaEntity(anotherUser, "002", "987654321", "987654321");
        anotherAccount = accountRepository.save(anotherAccount);

        LocalDate today = LocalDate.now();
        DailyLimitJpaEntity testAccountLimit = new DailyLimitJpaEntity(testAccount, today);
        DailyLimitJpaEntity anotherAccountLimit = new DailyLimitJpaEntity(anotherAccount, today);

        dailyLimitRepository.save(testAccountLimit);
        dailyLimitRepository.save(anotherAccountLimit);

        // when
        Optional<DailyLimitJpaEntity> testAccountFound = dailyLimitRepository.findByAccountIdAndLimitDateWithAccount(
                testAccount.getId(), today);
        Optional<DailyLimitJpaEntity> anotherAccountFound = dailyLimitRepository.findByAccountIdAndLimitDateWithAccount(
                anotherAccount.getId(), today);

        // then
        assertThat(testAccountFound).isPresent();
        assertThat(testAccountFound.get().getAccount().getId()).isEqualTo(testAccount.getId());

        assertThat(anotherAccountFound).isPresent();
        assertThat(anotherAccountFound.get().getAccount().getId()).isEqualTo(anotherAccount.getId());
    }

    @Test
    @DisplayName("존재하지 않는 계좌와 날짜로 조회하면 빈 결과를 반환한다")
    void findByAccountIdAndLimitDateReturnsEmptyForNonExistent() {
        // given
        LocalDate today = LocalDate.now();

        // when
        Optional<DailyLimitJpaEntity> found = dailyLimitRepository.findByAccountIdAndLimitDateWithAccount(
                9999L, today); // 존재하지 않는 계좌 ID

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("일일 한도 수정 시 버전이 증가한다")
    void versionShouldIncreaseOnUpdate() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimitJpaEntity dailyLimit = new DailyLimitJpaEntity(testAccount, today);
        DailyLimitJpaEntity savedLimit = dailyLimitRepository.save(dailyLimit);

        Integer initialVersion = savedLimit.getVersion();
        log.info("Initial version: {}", initialVersion);

        // when
        savedLimit.setWithdrawUsed(new BigDecimal("10000"));
        DailyLimitJpaEntity updatedLimit = dailyLimitRepository.save(savedLimit);

        // 영속성 컨텍스트 클리어 후 다시 조회
        entityManager.flush();
        entityManager.clear();

        Optional<DailyLimitJpaEntity> reloadedLimit = dailyLimitRepository.findByAccountIdAndLimitDateWithAccount(
                testAccount.getId(), today);

        // then
        assertThat(reloadedLimit).isPresent();
        assertThat(reloadedLimit.get().getVersion()).isGreaterThan(initialVersion);
        log.info("Updated version: {}", reloadedLimit.get().getVersion());
    }
}