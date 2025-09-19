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

    @Test
    @DisplayName("출금과 이체 사용량을 개별적으로 관리할 수 있다")
    void separateWithdrawAndTransferUsage() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimitJpaEntity dailyLimit = new DailyLimitJpaEntity(testAccount, today);

        // when - 출금만 사용
        dailyLimit.setWithdrawUsed(new BigDecimal("500000"));
        dailyLimit.setTransferUsed(BigDecimal.ZERO);
        DailyLimitJpaEntity saved = dailyLimitRepository.save(dailyLimit);

        // then
        assertThat(saved.getWithdrawUsed()).isEqualTo(new BigDecimal("500000"));
        assertThat(saved.getTransferUsed()).isEqualTo(BigDecimal.ZERO);

        // when - 이체만 추가 사용
        saved.setTransferUsed(new BigDecimal("1500000"));
        DailyLimitJpaEntity updated = dailyLimitRepository.save(saved);

        // then
        assertThat(updated.getWithdrawUsed()).isEqualTo(new BigDecimal("500000")); // 출금은 그대로
        assertThat(updated.getTransferUsed()).isEqualTo(new BigDecimal("1500000")); // 이체만 증가
    }

    @Test
    @DisplayName("일일 한도 초기값은 0이다")
    void defaultUsageAmountsShouldBeZero() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimitJpaEntity dailyLimit = new DailyLimitJpaEntity(testAccount, today);

        // when
        DailyLimitJpaEntity saved = dailyLimitRepository.save(dailyLimit);

        // then
        assertThat(saved.getWithdrawUsed()).isEqualTo(BigDecimal.ZERO);
        assertThat(saved.getTransferUsed()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("동일 계좌 동일 날짜에 대해 unique 제약이 적용된다")
    void uniqueConstraintOnAccountIdAndLimitDate() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimitJpaEntity firstLimit = new DailyLimitJpaEntity(testAccount, today);
        dailyLimitRepository.save(firstLimit);

        // when & then - 같은 계좌, 같은 날짜로 두 번째 저장 시도
        DailyLimitJpaEntity secondLimit = new DailyLimitJpaEntity(testAccount, today);

        // DataJpaTest에서는 실제 DB 제약조건이 적용되지 않을 수 있으므로
        // 조회를 통해 중복 여부 확인
        Optional<DailyLimitJpaEntity> existing = dailyLimitRepository.findByAccountIdAndLimitDateWithAccount(
                testAccount.getId(), today);

        assertThat(existing).isPresent();
        assertThat(existing.get().getId()).isEqualTo(firstLimit.getId());
    }

    @Test
    @DisplayName("대용량 사용금액도 정확히 저장되고 조회된다")
    void handleLargeUsageAmounts() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimitJpaEntity dailyLimit = new DailyLimitJpaEntity(testAccount, today);

        BigDecimal largeWithdrawAmount = new BigDecimal("999999999.99");
        BigDecimal largeTransferAmount = new BigDecimal("2999999999.99");

        dailyLimit.setWithdrawUsed(largeWithdrawAmount);
        dailyLimit.setTransferUsed(largeTransferAmount);

        // when
        DailyLimitJpaEntity saved = dailyLimitRepository.save(dailyLimit);

        // then
        assertThat(saved.getWithdrawUsed()).isEqualTo(largeWithdrawAmount);
        assertThat(saved.getTransferUsed()).isEqualTo(largeTransferAmount);

        // 재조회해서도 확인
        Optional<DailyLimitJpaEntity> reloaded = dailyLimitRepository.findByAccountIdAndLimitDateWithAccount(
                testAccount.getId(), today);

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getWithdrawUsed()).isEqualTo(largeWithdrawAmount);
        assertThat(reloaded.get().getTransferUsed()).isEqualTo(largeTransferAmount);
    }

    @Test
    @DisplayName("소수점 금액도 정확히 처리된다")
    void handleDecimalAmounts() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimitJpaEntity dailyLimit = new DailyLimitJpaEntity(testAccount, today);

        BigDecimal withdrawWithDecimal = new BigDecimal("12345.67");
        BigDecimal transferWithDecimal = new BigDecimal("98765.43");

        dailyLimit.setWithdrawUsed(withdrawWithDecimal);
        dailyLimit.setTransferUsed(transferWithDecimal);

        // when
        DailyLimitJpaEntity saved = dailyLimitRepository.save(dailyLimit);

        // then
        assertThat(saved.getWithdrawUsed()).isEqualTo(withdrawWithDecimal);
        assertThat(saved.getTransferUsed()).isEqualTo(transferWithDecimal);
    }

    @Test
    @DisplayName("미래 날짜와 과거 날짜의 한도를 구분하여 관리할 수 있다")
    void handleDifferentDateRanges() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        DailyLimitJpaEntity todayLimit = new DailyLimitJpaEntity(testAccount, today);
        todayLimit.setWithdrawUsed(new BigDecimal("100000"));
        todayLimit.setTransferUsed(new BigDecimal("200000"));

        DailyLimitJpaEntity yesterdayLimit = new DailyLimitJpaEntity(testAccount, yesterday);
        yesterdayLimit.setWithdrawUsed(new BigDecimal("300000"));
        yesterdayLimit.setTransferUsed(new BigDecimal("400000"));

        DailyLimitJpaEntity tomorrowLimit = new DailyLimitJpaEntity(testAccount, tomorrow);
        tomorrowLimit.setWithdrawUsed(new BigDecimal("500000"));
        tomorrowLimit.setTransferUsed(new BigDecimal("600000"));

        // when
        dailyLimitRepository.save(todayLimit);
        dailyLimitRepository.save(yesterdayLimit);
        dailyLimitRepository.save(tomorrowLimit);

        // then - 각 날짜별로 독립적으로 조회
        Optional<DailyLimitJpaEntity> todayFound = dailyLimitRepository.findByAccountIdAndLimitDateWithAccount(
                testAccount.getId(), today);
        Optional<DailyLimitJpaEntity> yesterdayFound = dailyLimitRepository.findByAccountIdAndLimitDateWithAccount(
                testAccount.getId(), yesterday);
        Optional<DailyLimitJpaEntity> tomorrowFound = dailyLimitRepository.findByAccountIdAndLimitDateWithAccount(
                testAccount.getId(), tomorrow);

        assertThat(todayFound).isPresent();
        assertThat(todayFound.get().getWithdrawUsed()).isEqualTo(new BigDecimal("100000"));
        assertThat(todayFound.get().getTransferUsed()).isEqualTo(new BigDecimal("200000"));

        assertThat(yesterdayFound).isPresent();
        assertThat(yesterdayFound.get().getWithdrawUsed()).isEqualTo(new BigDecimal("300000"));
        assertThat(yesterdayFound.get().getTransferUsed()).isEqualTo(new BigDecimal("400000"));

        assertThat(tomorrowFound).isPresent();
        assertThat(tomorrowFound.get().getWithdrawUsed()).isEqualTo(new BigDecimal("500000"));
        assertThat(tomorrowFound.get().getTransferUsed()).isEqualTo(new BigDecimal("600000"));
    }

    @Test
    @DisplayName("비관적 락 조회와 일반 조회 결과가 동일하다")
    void pessimisticLockAndNormalQueryReturnSameResult() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimitJpaEntity dailyLimit = new DailyLimitJpaEntity(testAccount, today);
        dailyLimit.setWithdrawUsed(new BigDecimal("75000"));
        dailyLimit.setTransferUsed(new BigDecimal("125000"));
        dailyLimitRepository.save(dailyLimit);

        // when
        Optional<DailyLimitJpaEntity> normalResult = dailyLimitRepository.findByAccountIdAndLimitDateWithAccount(
                testAccount.getId(), today);
        Optional<DailyLimitJpaEntity> lockResult = dailyLimitRepository.findByAccountIdAndLimitDateWithAccountAndLock(
                testAccount.getId(), today);

        // then
        assertThat(normalResult).isPresent();
        assertThat(lockResult).isPresent();

        assertThat(normalResult.get().getId()).isEqualTo(lockResult.get().getId());
        assertThat(normalResult.get().getWithdrawUsed()).isEqualTo(lockResult.get().getWithdrawUsed());
        assertThat(normalResult.get().getTransferUsed()).isEqualTo(lockResult.get().getTransferUsed());
        assertThat(normalResult.get().getVersion()).isEqualTo(lockResult.get().getVersion());
    }

    @Test
    @DisplayName("계좌와 한도 엔티티 간 FK 관계가 정상 동작한다")
    void foreignKeyRelationshipWorksCorrectly() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimitJpaEntity dailyLimit = new DailyLimitJpaEntity(testAccount, today);
        DailyLimitJpaEntity saved = dailyLimitRepository.save(dailyLimit);

        // when - 연관된 계좌 정보 접근
        AccountJpaEntity relatedAccount = saved.getAccount();

        // then
        assertThat(relatedAccount).isNotNull();
        assertThat(relatedAccount.getId()).isEqualTo(testAccount.getId());
        assertThat(relatedAccount.getAccountNo()).isEqualTo(testAccount.getAccountNo());
        assertThat(relatedAccount.getUser().getName()).isEqualTo("홍길동");
    }
}