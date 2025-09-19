package com.moneyTransfer.persistence.adapter;

import com.moneyTransfer.domain.dailylimit.DailyLimit;
import com.moneyTransfer.persistence.entity.AccountJpaEntity;
import com.moneyTransfer.persistence.entity.DailyLimitJpaEntity;
import com.moneyTransfer.persistence.entity.UserJpaEntity;
import com.moneyTransfer.persistence.repository.AccountJpaRepository;
import com.moneyTransfer.persistence.repository.DailyLimitJpaRepository;
import com.moneyTransfer.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaDailyLimitPort.class) // Port 구현체를 테스트 컨텍스트에 포함
class JpaDailyLimitPortTest {

    private static final Logger log = LoggerFactory.getLogger(JpaDailyLimitPortTest.class);

    @Autowired
    private JpaDailyLimitPort dailyLimitPort;

    @Autowired
    private AccountJpaRepository accountRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private DailyLimitJpaRepository dailyLimitJpaRepository;

    private AccountJpaEntity testAccount;
    private AccountJpaEntity targetAccount;

    @BeforeEach
    void setUp() {
        UserJpaEntity user1 = new UserJpaEntity("홍길동", "test1@domain.com", "1234567890123", "1234567890123");
        UserJpaEntity user2 = new UserJpaEntity("김철수", "test2@domain.com", "9876543210987", "9876543210987");
        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);

        testAccount = new AccountJpaEntity(user1, "001", "123456789", "123456789");
        targetAccount = new AccountJpaEntity(user2, "002", "987654321", "987654321");
        testAccount = accountRepository.save(testAccount);
        targetAccount = accountRepository.save(targetAccount);
    }

    @Test
    @DisplayName("새로운 일일 한도를 저장할 수 있다")
    void saveNewDailyLimit() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimit dailyLimit = DailyLimit.createNew(testAccount.getId(), today);
        dailyLimit.setWithdrawUsed(new BigDecimal("50000"));
        dailyLimit.setTransferUsed(new BigDecimal("100000"));

        // when
        DailyLimit savedDailyLimit = dailyLimitPort.save(dailyLimit);

        // 저장된 도메인 정보 로깅
        log.info("Saved DailyLimit: id={}, accountId={}, limitDate={}, withdrawUsed={}, transferUsed={}, createdAt={}",
                savedDailyLimit.getId(), savedDailyLimit.getAccountId(), savedDailyLimit.getLimitDate(),
                savedDailyLimit.getWithdrawUsed(), savedDailyLimit.getTransferUsed(), savedDailyLimit.getCreatedAt());

        // then
        assertThat(savedDailyLimit.getId()).isNotNull();
        assertThat(savedDailyLimit.getAccountId()).isEqualTo(testAccount.getId());
        assertThat(savedDailyLimit.getLimitDate()).isEqualTo(today);
        assertThat(savedDailyLimit.getWithdrawUsed()).isEqualTo(new BigDecimal("50000"));
        assertThat(savedDailyLimit.getTransferUsed()).isEqualTo(new BigDecimal("100000"));
        assertThat(savedDailyLimit.getCreatedAt()).isNotNull();
        assertThat(savedDailyLimit.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("기존 일일 한도를 업데이트할 수 있다")
    void updateExistingDailyLimit() {
        // given - 먼저 일일 한도 생성
        LocalDate today = LocalDate.now();
        DailyLimit originalLimit = DailyLimit.createNew(testAccount.getId(), today);
        DailyLimit savedLimit = dailyLimitPort.save(originalLimit);

        // when - 사용량 업데이트
        savedLimit.setWithdrawUsed(new BigDecimal("30000"));
        savedLimit.setTransferUsed(new BigDecimal("150000"));
        DailyLimit updatedLimit = dailyLimitPort.save(savedLimit);

        // 업데이트된 도메인 정보 로깅
        log.info("Updated DailyLimit: id={}, accountId={}, limitDate={}, withdrawUsed={}, transferUsed={}, updatedAt={}",
                updatedLimit.getId(), updatedLimit.getAccountId(), updatedLimit.getLimitDate(),
                updatedLimit.getWithdrawUsed(), updatedLimit.getTransferUsed(), updatedLimit.getUpdatedAt());

        // then
        assertThat(updatedLimit.getId()).isEqualTo(savedLimit.getId());
        assertThat(updatedLimit.getAccountId()).isEqualTo(testAccount.getId());
        assertThat(updatedLimit.getLimitDate()).isEqualTo(today);
        assertThat(updatedLimit.getWithdrawUsed()).isEqualTo(new BigDecimal("30000"));
        assertThat(updatedLimit.getTransferUsed()).isEqualTo(new BigDecimal("150000"));
    }

    @Test
    @DisplayName("존재하지 않는 계좌로 일일 한도 저장 시 예외가 발생한다")
    void saveWithNonExistentAccount() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimit dailyLimit = DailyLimit.createNew(9999L, today); // 존재하지 않는 계좌

        // when & then
        assertThatThrownBy(() -> dailyLimitPort.save(dailyLimit))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("계좌를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("계좌와 날짜로 일일 한도를 조회할 수 있다")
    void findByAccountIdAndLimitDate() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimit dailyLimit = DailyLimit.createNew(testAccount.getId(), today);
        dailyLimit.setWithdrawUsed(new BigDecimal("25000"));
        dailyLimit.setTransferUsed(new BigDecimal("75000"));
        DailyLimit saved = dailyLimitPort.save(dailyLimit);

        // when
        Optional<DailyLimit> found = dailyLimitPort.findByAccountIdAndLimitDate(testAccount.getId(), today);

        // 조회된 도메인 정보 로깅
        found.ifPresent(d -> log.info("Found DailyLimit: id={}, accountId={}, limitDate={}, withdrawUsed={}, transferUsed={}",
                d.getId(), d.getAccountId(), d.getLimitDate(), d.getWithdrawUsed(), d.getTransferUsed()));

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getAccountId()).isEqualTo(testAccount.getId());
        assertThat(found.get().getLimitDate()).isEqualTo(today);
        assertThat(found.get().getWithdrawUsed()).isEqualTo(new BigDecimal("25000"));
        assertThat(found.get().getTransferUsed()).isEqualTo(new BigDecimal("75000"));
    }

    @Test
    @DisplayName("비관적 락으로 일일 한도를 조회할 수 있다")
    void findByAccountIdAndLimitDateWithLock() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimit dailyLimit = DailyLimit.createNew(testAccount.getId(), today);
        dailyLimit.setWithdrawUsed(new BigDecimal("40000"));
        dailyLimit.setTransferUsed(new BigDecimal("80000"));
        DailyLimit saved = dailyLimitPort.save(dailyLimit);

        // when
        Optional<DailyLimit> found = dailyLimitPort.findByAccountIdAndLimitDateWithLock(testAccount.getId(), today);

        // 조회된 도메인 정보 로깅
        found.ifPresent(d -> log.info("Found DailyLimit with Lock: id={}, accountId={}, limitDate={}, withdrawUsed={}, transferUsed={}",
                d.getId(), d.getAccountId(), d.getLimitDate(), d.getWithdrawUsed(), d.getTransferUsed()));

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getAccountId()).isEqualTo(testAccount.getId());
        assertThat(found.get().getLimitDate()).isEqualTo(today);
        assertThat(found.get().getWithdrawUsed()).isEqualTo(new BigDecimal("40000"));
        assertThat(found.get().getTransferUsed()).isEqualTo(new BigDecimal("80000"));
    }

    @Test
    @DisplayName("존재하지 않는 계좌와 날짜로 조회하면 빈 결과를 반환한다")
    void findByAccountIdAndLimitDateReturnsEmptyForNonExistent() {
        // given
        LocalDate today = LocalDate.now();

        // when
        Optional<DailyLimit> found = dailyLimitPort.findByAccountIdAndLimitDate(9999L, today);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("다른 날짜의 일일 한도는 조회되지 않는다")
    void findByAccountIdAndLimitDateShouldNotReturnDifferentDate() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        DailyLimit todayLimit = DailyLimit.createNew(testAccount.getId(), today);
        DailyLimit yesterdayLimit = DailyLimit.createNew(testAccount.getId(), yesterday);

        dailyLimitPort.save(todayLimit);
        dailyLimitPort.save(yesterdayLimit);

        // when
        Optional<DailyLimit> todayFound = dailyLimitPort.findByAccountIdAndLimitDate(testAccount.getId(), today);
        Optional<DailyLimit> yesterdayFound = dailyLimitPort.findByAccountIdAndLimitDate(testAccount.getId(), yesterday);

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
        LocalDate today = LocalDate.now();
        DailyLimit testAccountLimit = DailyLimit.createNew(testAccount.getId(), today);
        DailyLimit targetAccountLimit = DailyLimit.createNew(targetAccount.getId(), today);

        dailyLimitPort.save(testAccountLimit);
        dailyLimitPort.save(targetAccountLimit);

        // when
        Optional<DailyLimit> testAccountFound = dailyLimitPort.findByAccountIdAndLimitDate(testAccount.getId(), today);
        Optional<DailyLimit> targetAccountFound = dailyLimitPort.findByAccountIdAndLimitDate(targetAccount.getId(), today);

        // then
        assertThat(testAccountFound).isPresent();
        assertThat(testAccountFound.get().getAccountId()).isEqualTo(testAccount.getId());

        assertThat(targetAccountFound).isPresent();
        assertThat(targetAccountFound.get().getAccountId()).isEqualTo(targetAccount.getId());
    }

    @Test
    @DisplayName("Domain-Entity 매핑이 정확하게 동작한다")
    void testDomainEntityMapping() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimit originalDailyLimit = DailyLimit.createNew(testAccount.getId(), today);
        originalDailyLimit.setWithdrawUsed(new BigDecimal("123456.78"));
        originalDailyLimit.setTransferUsed(new BigDecimal("987654.32"));

        // when
        DailyLimit savedDailyLimit = dailyLimitPort.save(originalDailyLimit);

        // then - 모든 필드가 정확히 매핑되는지 검증
        assertThat(savedDailyLimit.getId()).isNotNull();
        assertThat(savedDailyLimit.getAccountId()).isEqualTo(originalDailyLimit.getAccountId());
        assertThat(savedDailyLimit.getLimitDate()).isEqualTo(originalDailyLimit.getLimitDate());
        assertThat(savedDailyLimit.getWithdrawUsed()).isEqualTo(originalDailyLimit.getWithdrawUsed());
        assertThat(savedDailyLimit.getTransferUsed()).isEqualTo(originalDailyLimit.getTransferUsed());
        assertThat(savedDailyLimit.getCreatedAt()).isNotNull();
        assertThat(savedDailyLimit.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("출금과 이체 사용량을 독립적으로 관리할 수 있다")
    void testSeparateWithdrawAndTransferUsage() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimit dailyLimit = DailyLimit.createNew(testAccount.getId(), today);
        DailyLimit saved = dailyLimitPort.save(dailyLimit);

        // when - 출금만 사용
        saved.setWithdrawUsed(new BigDecimal("500000"));
        DailyLimit afterWithdraw = dailyLimitPort.save(saved);

        // then
        assertThat(afterWithdraw.getWithdrawUsed()).isEqualTo(new BigDecimal("500000"));
        assertThat(afterWithdraw.getTransferUsed()).isEqualTo(BigDecimal.ZERO);

        // when - 이체만 추가 사용
        afterWithdraw.setTransferUsed(new BigDecimal("1500000"));
        DailyLimit afterTransfer = dailyLimitPort.save(afterWithdraw);

        // then
        assertThat(afterTransfer.getWithdrawUsed()).isEqualTo(new BigDecimal("500000")); // 출금은 그대로
        assertThat(afterTransfer.getTransferUsed()).isEqualTo(new BigDecimal("1500000")); // 이체만 증가
    }

    @Test
    @DisplayName("비관적 락과 일반 조회 결과가 동일하다")
    void testLockAndNormalQueryReturnSameResult() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimit dailyLimit = DailyLimit.createNew(testAccount.getId(), today);
        dailyLimit.setWithdrawUsed(new BigDecimal("60000"));
        dailyLimit.setTransferUsed(new BigDecimal("120000"));
        dailyLimitPort.save(dailyLimit);

        // when
        Optional<DailyLimit> normalResult = dailyLimitPort.findByAccountIdAndLimitDate(testAccount.getId(), today);
        Optional<DailyLimit> lockResult = dailyLimitPort.findByAccountIdAndLimitDateWithLock(testAccount.getId(), today);

        // then
        assertThat(normalResult).isPresent();
        assertThat(lockResult).isPresent();

        assertThat(normalResult.get().getId()).isEqualTo(lockResult.get().getId());
        assertThat(normalResult.get().getAccountId()).isEqualTo(lockResult.get().getAccountId());
        assertThat(normalResult.get().getLimitDate()).isEqualTo(lockResult.get().getLimitDate());
        assertThat(normalResult.get().getWithdrawUsed()).isEqualTo(lockResult.get().getWithdrawUsed());
        assertThat(normalResult.get().getTransferUsed()).isEqualTo(lockResult.get().getTransferUsed());
    }

    @Test
    @DisplayName("addWithdrawUsed와 addTransferUsed 메서드가 정상 동작한다")
    void testAddUsageAmountMethods() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimit dailyLimit = DailyLimit.createNew(testAccount.getId(), today);
        dailyLimit.setWithdrawUsed(new BigDecimal("100000"));
        dailyLimit.setTransferUsed(new BigDecimal("200000"));
        DailyLimit saved = dailyLimitPort.save(dailyLimit);

        // when - addWithdrawUsed 사용
        DailyLimit afterAddWithdraw = saved.addWithdrawUsed(new BigDecimal("50000"));
        DailyLimit savedAfterWithdraw = dailyLimitPort.save(afterAddWithdraw);

        // then
        assertThat(savedAfterWithdraw.getWithdrawUsed()).isEqualTo(new BigDecimal("150000")); // 100000 + 50000
        assertThat(savedAfterWithdraw.getTransferUsed()).isEqualTo(new BigDecimal("200000")); // 변경 없음

        // when - addTransferUsed 사용
        DailyLimit afterAddTransfer = savedAfterWithdraw.addTransferUsed(new BigDecimal("100000"));
        DailyLimit savedAfterTransfer = dailyLimitPort.save(afterAddTransfer);

        // then
        assertThat(savedAfterTransfer.getWithdrawUsed()).isEqualTo(new BigDecimal("150000")); // 변경 없음
        assertThat(savedAfterTransfer.getTransferUsed()).isEqualTo(new BigDecimal("300000")); // 200000 + 100000
    }

    @Test
    @DisplayName("대용량 사용금액도 정확히 처리된다")
    void testLargeUsageAmounts() {
        // given
        LocalDate today = LocalDate.now();
        DailyLimit dailyLimit = DailyLimit.createNew(testAccount.getId(), today);

        BigDecimal largeWithdrawAmount = new BigDecimal("999999999.99");
        BigDecimal largeTransferAmount = new BigDecimal("2999999999.99");

        dailyLimit.setWithdrawUsed(largeWithdrawAmount);
        dailyLimit.setTransferUsed(largeTransferAmount);

        // when
        DailyLimit saved = dailyLimitPort.save(dailyLimit);

        // then
        assertThat(saved.getWithdrawUsed()).isEqualTo(largeWithdrawAmount);
        assertThat(saved.getTransferUsed()).isEqualTo(largeTransferAmount);

        // 재조회해서도 확인
        Optional<DailyLimit> reloaded = dailyLimitPort.findByAccountIdAndLimitDate(testAccount.getId(), today);

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getWithdrawUsed()).isEqualTo(largeWithdrawAmount);
        assertThat(reloaded.get().getTransferUsed()).isEqualTo(largeTransferAmount);
    }
}