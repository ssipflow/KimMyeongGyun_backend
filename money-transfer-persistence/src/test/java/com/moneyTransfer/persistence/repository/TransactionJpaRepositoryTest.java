package com.moneyTransfer.persistence.repository;

import com.moneyTransfer.persistence.entity.AccountJpaEntity;
import com.moneyTransfer.persistence.entity.TransactionJpaEntity;
import com.moneyTransfer.persistence.entity.UserJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TransactionJpaRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionJpaRepositoryTest.class);

    @Autowired
    private TransactionJpaRepository transactionRepository;

    @Autowired
    private AccountJpaRepository accountRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private AccountJpaEntity testAccount;
    private AccountJpaEntity targetAccount;

    @BeforeEach
    void setUp() {
        UserJpaEntity user1 = new UserJpaEntity("홍길동", "test1@domain.com", "1234567890123", "1234567890123");
        user1 = userRepository.save(user1);

        UserJpaEntity user2 = new UserJpaEntity("김철수", "test2@domain.com", "9876543210987", "9876543210987");
        user2 = userRepository.save(user2);

        testAccount = new AccountJpaEntity(user1, "001", "123456789", "123456789");
        testAccount = accountRepository.save(testAccount);

        targetAccount = new AccountJpaEntity(user2, "002", "987654321", "987654321");
        targetAccount = accountRepository.save(targetAccount);
    }

    @Test
    @DisplayName("입금 거래를 저장하고 조회할 수 있다")
    void saveAndFindDepositTransaction() {
        // given
        TransactionJpaEntity transaction = new TransactionJpaEntity(
                100, // DEPOSIT
                testAccount,
                null, // 입금이므로 대상 계좌 없음
                new BigDecimal("10000"),
                new BigDecimal("10000"), // 거래 후 잔액
                BigDecimal.ZERO, // 입금 수수료 없음
                "급여 입금"
        );

        // when
        TransactionJpaEntity savedTransaction = transactionRepository.save(transaction);

        // 저장된 엔티티 정보 로깅
        log.info("Saved Transaction: id={}, type={}, accountId={}, amount={}, balanceAfter={}, fee={}, description={}, createdAt={}",
                savedTransaction.getId(), savedTransaction.getType(), savedTransaction.getAccount().getId(),
                savedTransaction.getAmount(), savedTransaction.getBalanceAfter(), savedTransaction.getFee(),
                savedTransaction.getDescription(), savedTransaction.getCreatedAt());

        // then
        assertThat(savedTransaction.getId()).isNotNull();
        assertThat(savedTransaction.getType()).isEqualTo(100);
        assertThat(savedTransaction.getAccount().getId()).isEqualTo(testAccount.getId());
        assertThat(savedTransaction.getAmount()).isEqualTo(new BigDecimal("10000"));
        assertThat(savedTransaction.getBalanceAfter()).isEqualTo(new BigDecimal("10000"));
        assertThat(savedTransaction.getFee()).isEqualTo(BigDecimal.ZERO);
        assertThat(savedTransaction.getRelatedAccount()).isNull();
    }

    @Test
    @DisplayName("출금 거래를 저장하고 조회할 수 있다")
    void saveAndFindWithdrawTransaction() {
        // given
        TransactionJpaEntity transaction = new TransactionJpaEntity(
                200, // WITHDRAW
                testAccount,
                null, // 출금이므로 대상 계좌 없음
                new BigDecimal("5000"),
                new BigDecimal("5000"), // 거래 후 잔액
                BigDecimal.ZERO, // 출금 수수료 없음
                "ATM 출금"
        );

        // when
        TransactionJpaEntity savedTransaction = transactionRepository.save(transaction);

        // 저장된 엔티티 정보 로깅
        log.info("Saved Transaction: id={}, type={}, accountId={}, amount={}, balanceAfter={}, fee={}, description={}, createdAt={}",
                savedTransaction.getId(), savedTransaction.getType(), savedTransaction.getAccount().getId(),
                savedTransaction.getAmount(), savedTransaction.getBalanceAfter(), savedTransaction.getFee(),
                savedTransaction.getDescription(), savedTransaction.getCreatedAt());

        // then
        assertThat(savedTransaction.getId()).isNotNull();
        assertThat(savedTransaction.getType()).isEqualTo(200);
        assertThat(savedTransaction.getAccount().getId()).isEqualTo(testAccount.getId());
        assertThat(savedTransaction.getAmount()).isEqualTo(new BigDecimal("5000"));
        assertThat(savedTransaction.getBalanceAfter()).isEqualTo(new BigDecimal("5000"));
        assertThat(savedTransaction.getFee()).isEqualTo(BigDecimal.ZERO);
        assertThat(savedTransaction.getRelatedAccount()).isNull();
    }

    @Test
    @DisplayName("이체 송금 거래를 저장하고 조회할 수 있다")
    void saveAndFindTransferSendTransaction() {
        // given
        TransactionJpaEntity transaction = new TransactionJpaEntity(
                300, // TRANSFER_SEND
                testAccount,
                targetAccount, // 이체 대상 계좌 엔티티
                new BigDecimal("20000"),
                new BigDecimal("80000"), // 거래 후 잔액
                new BigDecimal("200"), // 이체 수수료 1%
                "김철수에게 이체"
        );

        // when
        TransactionJpaEntity savedTransaction = transactionRepository.save(transaction);

        // 저장된 엔티티 정보 로깅
        log.info("Saved Transaction: id={}, type={}, accountId={}, relatedAccountId={}, amount={}, balanceAfter={}, fee={}, description={}, createdAt={}",
                savedTransaction.getId(), savedTransaction.getType(), savedTransaction.getAccount().getId(),
                savedTransaction.getRelatedAccount().getId(), savedTransaction.getAmount(), savedTransaction.getBalanceAfter(),
                savedTransaction.getFee(), savedTransaction.getDescription(), savedTransaction.getCreatedAt());

        // then
        assertThat(savedTransaction.getId()).isNotNull();
        assertThat(savedTransaction.getType()).isEqualTo(300);
        assertThat(savedTransaction.getAccount().getId()).isEqualTo(testAccount.getId());
        assertThat(savedTransaction.getRelatedAccount().getId()).isEqualTo(targetAccount.getId());
        assertThat(savedTransaction.getAmount()).isEqualTo(new BigDecimal("20000"));
        assertThat(savedTransaction.getBalanceAfter()).isEqualTo(new BigDecimal("80000"));
        assertThat(savedTransaction.getFee()).isEqualTo(new BigDecimal("200"));
    }

    @Test
    @DisplayName("이체 수취 거래를 저장하고 조회할 수 있다")
    void saveAndFindTransferReceiveTransaction() {
        // given
        TransactionJpaEntity transaction = new TransactionJpaEntity(
                400, // TRANSFER_RECEIVE
                targetAccount,
                testAccount, // 이체 송신 계좌 엔티티
                new BigDecimal("20000"),
                new BigDecimal("20000"), // 거래 후 잔액
                BigDecimal.ZERO, // 수금 수수료 없음
                "홍길동으로부터 이체 수금"
        );

        // when
        TransactionJpaEntity savedTransaction = transactionRepository.save(transaction);

        // 저장된 엔티티 정보 로깅
        log.info("Saved Transaction: id={}, type={}, accountId={}, relatedAccountId={}, amount={}, balanceAfter={}, fee={}, description={}, createdAt={}",
                savedTransaction.getId(), savedTransaction.getType(), savedTransaction.getAccount().getId(),
                savedTransaction.getRelatedAccount().getId(), savedTransaction.getAmount(), savedTransaction.getBalanceAfter(),
                savedTransaction.getFee(), savedTransaction.getDescription(), savedTransaction.getCreatedAt());

        // then
        assertThat(savedTransaction.getId()).isNotNull();
        assertThat(savedTransaction.getType()).isEqualTo(400);
        assertThat(savedTransaction.getAccount().getId()).isEqualTo(targetAccount.getId());
        assertThat(savedTransaction.getRelatedAccount().getId()).isEqualTo(testAccount.getId());
        assertThat(savedTransaction.getAmount()).isEqualTo(new BigDecimal("20000"));
        assertThat(savedTransaction.getBalanceAfter()).isEqualTo(new BigDecimal("20000"));
        assertThat(savedTransaction.getFee()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("계좌별 거래내역을 조회할 수 있다")
    void findByAccountIdWithAccount() {
        // given
        TransactionJpaEntity deposit = new TransactionJpaEntity(
                100, testAccount, null, new BigDecimal("10000"), new BigDecimal("10000"), BigDecimal.ZERO, "입금");
        transactionRepository.save(deposit);

        TransactionJpaEntity withdraw = new TransactionJpaEntity(
                200, testAccount, null, new BigDecimal("5000"), new BigDecimal("5000"), BigDecimal.ZERO, "출금");
        transactionRepository.save(withdraw);

        TransactionJpaEntity transfer = new TransactionJpaEntity(
                300, testAccount, targetAccount, new BigDecimal("20000"), new BigDecimal("80000"), new BigDecimal("200"), "이체");
        transactionRepository.save(transfer);


        // when
        List<TransactionJpaEntity> transactions = transactionRepository.findByAccountId(testAccount.getId());

        // 로드한 엔티티 정보 로깅
        transactions.forEach(t -> log.info("Found Transaction: id={}, type={}, accountId={}, accountNormNo = {}, relatedAccountId={}, amount={}, balanceAfter={}, fee={}, description={}, createdAt={}",
                t.getId(), t.getType(), t.getAccount().getId(), t.getAccount().getAccountNoNorm(),
                t.getRelatedAccount() != null ? t.getRelatedAccount().getId() : null,
                t.getAmount(), t.getBalanceAfter(), t.getFee(), t.getDescription(), t.getCreatedAt()));

        // then
        assertThat(transactions).hasSize(3);
        // 생성일 역순으로 정렬되어야 함
        assertThat(transactions.get(0).getType()).isEqualTo(300); // 가장 최근
        assertThat(transactions.get(1).getType()).isEqualTo(200);
        assertThat(transactions.get(2).getType()).isEqualTo(100); // 가장 오래됨
        // Fetch Join 확인
        assertThat(transactions.get(0).getAccount().getUser().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("날짜 범위로 거래내역을 조회할 수 있다")
    void findByAccountIdAndDateRange() {
        // given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);

        TransactionJpaEntity oldTransaction = new TransactionJpaEntity(
                100, testAccount, null, new BigDecimal("5000"), new BigDecimal("15000"), BigDecimal.ZERO, "오래된 거래");
        oldTransaction.setCreatedAt(yesterday);

        TransactionJpaEntity recentTransaction = new TransactionJpaEntity(
                200, testAccount, null, new BigDecimal("3000"), new BigDecimal("12000"), BigDecimal.ZERO, "최근 거래");
        recentTransaction.setCreatedAt(now);

        transactionRepository.save(oldTransaction);
        transactionRepository.save(recentTransaction);

        // when - 오늘 거래만 조회
        List<TransactionJpaEntity> todayTransactions = transactionRepository.findByAccountIdAndDateRange(
                testAccount.getId(), now.minusHours(1), tomorrow);

        // 로드한 엔티티 정보 로깅
        todayTransactions.forEach(t -> log.info("Today Transaction: id={}, type={}, amount={}, description={}, createdAt={}",
                t.getId(), t.getType(), t.getAmount(), t.getDescription(), t.getCreatedAt()));

        // then
        assertThat(todayTransactions).hasSize(1);
        assertThat(todayTransactions.get(0).getDescription()).isEqualTo("최근 거래");
        // Fetch Join 확인
        assertThat(todayTransactions.get(0).getAccount().getUser().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("다른 계좌의 거래내역은 조회되지 않는다")
    void findByAccountIdShouldNotReturnOtherAccountTransactions() {
        // given
        TransactionJpaEntity transaction1 = new TransactionJpaEntity(
                100, testAccount, null, new BigDecimal("10000"), new BigDecimal("10000"), BigDecimal.ZERO, "testAccount 거래");
        transactionRepository.save(transaction1);

        TransactionJpaEntity transaction2 = new TransactionJpaEntity(
                100, targetAccount, null, new BigDecimal("5000"), new BigDecimal("5000"), BigDecimal.ZERO, "targetAccount 거래");
        transactionRepository.save(transaction2);

        // when
        List<TransactionJpaEntity> testAccountTransactions = transactionRepository.findByAccountId(testAccount.getId());
        List<TransactionJpaEntity> targetAccountTransactions = transactionRepository.findByAccountId(targetAccount.getId());

        // then
        assertThat(testAccountTransactions).hasSize(1);
        assertThat(testAccountTransactions.get(0).getDescription()).isEqualTo("testAccount 거래");

        assertThat(targetAccountTransactions).hasSize(1);
        assertThat(targetAccountTransactions.get(0).getDescription()).isEqualTo("targetAccount 거래");
    }

    @Test
    @DisplayName("TRANSFER_SEND 에서 accountId는 송금계좌, relatedAccountId는 수취계좌를 나타낸다")
    void transferSendAccountRole() {
        // given
        TransactionJpaEntity transferSend = new TransactionJpaEntity(
                300, // TRANSFER_SEND
                testAccount,    // 송금 계좌 (주체)
                targetAccount,  // 수취 계좌 (연관)
                new BigDecimal("10000"),
                new BigDecimal("90000"),
                new BigDecimal("100"),
                "이체 송금"
        );

        // when
        TransactionJpaEntity saved = transactionRepository.save(transferSend);

        // then
        assertThat(saved.getType()).isEqualTo(300);
        assertThat(saved.getAccount().getId()).isEqualTo(testAccount.getId()); // 송금 계좌
        assertThat(saved.getRelatedAccount().getId()).isEqualTo(targetAccount.getId()); // 수취 계좌
    }

    @Test
    @DisplayName("TRANSFER_RECEIVE 에서 accountId는 수취계좌, relatedAccountId는 송금계좌를 나타낸다")
    void transferReceiveAccountRole() {
        // given
        TransactionJpaEntity transferReceive = new TransactionJpaEntity(
                400, // TRANSFER_RECEIVE
                targetAccount,  // 송금 계좌 (주체)
                testAccount,    // 수취 계좌 (연관)
                new BigDecimal("10000"),
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                "이체 수취"
        );

        // when
        TransactionJpaEntity saved = transactionRepository.save(transferReceive);

        // then
        assertThat(saved.getType()).isEqualTo(400);
        assertThat(saved.getAccount().getId()).isEqualTo(targetAccount.getId()); // 수취 계좌
        assertThat(saved.getRelatedAccount().getId()).isEqualTo(testAccount.getId()); // 송금 계좌
    }

    @Test
    @DisplayName("relatedAccount가 null인 경우 정상 처리된다")
    void handleNullRelatedAccount() {
        // given
        TransactionJpaEntity deposit = new TransactionJpaEntity(
                100, // DEPOSIT
                testAccount,
                null, // relatedAccount가 null
                new BigDecimal("5000"),
                new BigDecimal("15000"),
                BigDecimal.ZERO,
                "입금"
        );

        // when
        TransactionJpaEntity saved = transactionRepository.save(deposit);

        // then
        assertThat(saved.getRelatedAccount()).isNull();
        assertThat(saved.getAccount()).isNotNull();
    }

    @Test
    @DisplayName("Lazy Loading이 정상 동작한다")
    void testLazyLoading() {
        // given
        TransactionJpaEntity transfer = new TransactionJpaEntity(
                300, testAccount, targetAccount,
                new BigDecimal("5000"), new BigDecimal("95000"),
                new BigDecimal("50"), "Lazy Loading 테스트"
        );
        TransactionJpaEntity savedTransaction = transactionRepository.save(transfer);
        Long savedTransactionId = savedTransaction.getId();

        // 영속성 컨텍스트 클리어하여 캐시 무효화
        entityManager.flush();
        entityManager.clear();

        // when - ID로만 조회 (이제 실제 DB 쿼리 발생)
        TransactionJpaEntity foundTransaction = transactionRepository.findById(savedTransactionId).orElseThrow();

        // then - 연관 엔티티들이 Proxy 객체인지 확인
        assertThat(foundTransaction.getAccount()).isNotNull();
        assertThat(foundTransaction.getRelatedAccount()).isNotNull();

        // 실제 데이터 접근 시 Lazy Loading 발생
        log.info("Accessing account data - should trigger lazy loading");
        assertThat(foundTransaction.getAccount().getUser().getName()).isEqualTo("홍길동");

        log.info("Accessing related account data - should trigger lazy loading");
        assertThat(foundTransaction.getRelatedAccount().getUser().getName()).isEqualTo("김철수");
    }

    @Test
    @DisplayName("페이징으로 거래내역을 조회할 수 있다")
    void findByAccountIdWithPaging() {
        // given - 5개의 거래 생성
        for (int i = 1; i <= 5; i++) {
            TransactionJpaEntity transaction = new TransactionJpaEntity(
                    100, testAccount, null,
                    new BigDecimal(String.valueOf(i * 1000)),
                    new BigDecimal(String.valueOf(i * 1000)),
                    BigDecimal.ZERO,
                    "거래 " + i
            );
            transactionRepository.save(transaction);
        }

        // when - 첫 번째 페이지 (0페이지, 크기 3)
        Pageable pageable = PageRequest.of(0, 3);
        Page<TransactionJpaEntity> firstPage = transactionRepository.findByAccountIdWithPaging(testAccount.getId(), pageable);

        // 로드한 엔티티 정보 로깅
        log.info("First Page - Total: {}, TotalPages: {}, Number: {}, Size: {}",
                firstPage.getTotalElements(), firstPage.getTotalPages(), firstPage.getNumber(), firstPage.getSize());
        firstPage.getContent().forEach(t -> log.info("Page Content: id={}, description={}, createdAt={}",
                t.getId(), t.getDescription(), t.getCreatedAt()));

        // then
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getNumber()).isEqualTo(0);
        assertThat(firstPage.getSize()).isEqualTo(3);
        assertThat(firstPage.getContent()).hasSize(3);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.hasPrevious()).isFalse();

        // 생성일 역순으로 정렬되어야 함 (가장 최근 거래가 먼저)
        assertThat(firstPage.getContent().get(0).getDescription()).isEqualTo("거래 5");
        assertThat(firstPage.getContent().get(1).getDescription()).isEqualTo("거래 4");
        assertThat(firstPage.getContent().get(2).getDescription()).isEqualTo("거래 3");

        // when - 두 번째 페이지 (1페이지, 크기 3)
        Pageable secondPageable = PageRequest.of(1, 3);
        Page<TransactionJpaEntity> secondPage = transactionRepository.findByAccountIdWithPaging(testAccount.getId(), secondPageable);

        log.info("Second Page - Total: {}, TotalPages: {}, Number: {}, Size: {}",
                secondPage.getTotalElements(), secondPage.getTotalPages(), secondPage.getNumber(), secondPage.getSize());
        secondPage.getContent().forEach(t -> log.info("Page Content: id={}, description={}, createdAt={}",
                t.getId(), t.getDescription(), t.getCreatedAt()));

        // then
        assertThat(secondPage.getTotalElements()).isEqualTo(5);
        assertThat(secondPage.getTotalPages()).isEqualTo(2);
        assertThat(secondPage.getNumber()).isEqualTo(1);
        assertThat(secondPage.getSize()).isEqualTo(3);
        assertThat(secondPage.getContent()).hasSize(2); // 마지막 페이지는 2개만
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.hasPrevious()).isTrue();

        assertThat(secondPage.getContent().get(0).getDescription()).isEqualTo("거래 2");
        assertThat(secondPage.getContent().get(1).getDescription()).isEqualTo("거래 1");

        // Fetch Join 확인
        assertThat(firstPage.getContent().get(0).getAccount().getUser().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("날짜 범위와 페이징으로 거래내역을 조회할 수 있다")
    void findByAccountIdAndDateRangeWithPaging() {
        // given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);

        // 어제 거래 2개
        TransactionJpaEntity oldTransaction1 = new TransactionJpaEntity(
                100, testAccount, null, new BigDecimal("1000"), new BigDecimal("1000"), BigDecimal.ZERO, "어제 거래 1");
        oldTransaction1.setCreatedAt(yesterday);
        transactionRepository.save(oldTransaction1);

        TransactionJpaEntity oldTransaction2 = new TransactionJpaEntity(
                200, testAccount, null, new BigDecimal("2000"), new BigDecimal("3000"), BigDecimal.ZERO, "어제 거래 2");
        oldTransaction2.setCreatedAt(yesterday.plusHours(1));
        transactionRepository.save(oldTransaction2);

        // 오늘 거래 3개
        for (int i = 1; i <= 3; i++) {
            TransactionJpaEntity todayTransaction = new TransactionJpaEntity(
                    100, testAccount, null,
                    new BigDecimal(String.valueOf(i * 1000)),
                    new BigDecimal(String.valueOf(i * 1000)),
                    BigDecimal.ZERO,
                    "오늘 거래 " + i
            );
            todayTransaction.setCreatedAt(now.plusMinutes(i * 10));
            transactionRepository.save(todayTransaction);
        }

        // when - 오늘 거래만 페이징 조회 (0페이지, 크기 2)
        Pageable pageable = PageRequest.of(0, 2);
        Page<TransactionJpaEntity> todayPage = transactionRepository.findByAccountIdAndDateRangeWithPaging(
                testAccount.getId(), now.minusHours(1), tomorrow, pageable);

        // 로드한 엔티티 정보 로깅
        log.info("Today Page - Total: {}, TotalPages: {}, Number: {}, Size: {}",
                todayPage.getTotalElements(), todayPage.getTotalPages(), todayPage.getNumber(), todayPage.getSize());
        todayPage.getContent().forEach(t -> log.info("Today Page Content: id={}, description={}, createdAt={}",
                t.getId(), t.getDescription(), t.getCreatedAt()));

        // then
        assertThat(todayPage.getTotalElements()).isEqualTo(3); // 오늘 거래만
        assertThat(todayPage.getTotalPages()).isEqualTo(2); // 3개를 2개씩 나누면 2페이지
        assertThat(todayPage.getNumber()).isEqualTo(0);
        assertThat(todayPage.getSize()).isEqualTo(2);
        assertThat(todayPage.getContent()).hasSize(2);
        assertThat(todayPage.hasNext()).isTrue();
        assertThat(todayPage.hasPrevious()).isFalse();

        // 생성일 역순으로 정렬되어야 함 (가장 최근 거래가 먼저)
        assertThat(todayPage.getContent().get(0).getDescription()).isEqualTo("오늘 거래 3");
        assertThat(todayPage.getContent().get(1).getDescription()).isEqualTo("오늘 거래 2");

        // when - 두 번째 페이지 (1페이지, 크기 2)
        Pageable secondPageable = PageRequest.of(1, 2);
        Page<TransactionJpaEntity> secondPage = transactionRepository.findByAccountIdAndDateRangeWithPaging(
                testAccount.getId(), now.minusHours(1), tomorrow, secondPageable);

        log.info("Second Today Page - Total: {}, TotalPages: {}, Number: {}, Size: {}",
                secondPage.getTotalElements(), secondPage.getTotalPages(), secondPage.getNumber(), secondPage.getSize());
        secondPage.getContent().forEach(t -> log.info("Second Page Content: id={}, description={}, createdAt={}",
                t.getId(), t.getDescription(), t.getCreatedAt()));

        // then
        assertThat(secondPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getTotalPages()).isEqualTo(2);
        assertThat(secondPage.getNumber()).isEqualTo(1);
        assertThat(secondPage.getSize()).isEqualTo(2);
        assertThat(secondPage.getContent()).hasSize(1); // 마지막 페이지는 1개만
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.hasPrevious()).isTrue();

        assertThat(secondPage.getContent().get(0).getDescription()).isEqualTo("오늘 거래 1");

        // Fetch Join 확인
        assertThat(todayPage.getContent().get(0).getAccount().getUser().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("빈 페이지도 정상적으로 처리된다")
    void handleEmptyPage() {
        // given - 거래내역이 없는 상태

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<TransactionJpaEntity> emptyPage = transactionRepository.findByAccountIdWithPaging(testAccount.getId(), pageable);

        log.info("Empty Page - Total: {}, TotalPages: {}, Number: {}, Size: {}",
                emptyPage.getTotalElements(), emptyPage.getTotalPages(), emptyPage.getNumber(), emptyPage.getSize());

        // then
        assertThat(emptyPage.getTotalElements()).isEqualTo(0);
        assertThat(emptyPage.getTotalPages()).isEqualTo(0);
        assertThat(emptyPage.getNumber()).isEqualTo(0);
        assertThat(emptyPage.getSize()).isEqualTo(10);
        assertThat(emptyPage.getContent()).isEmpty();
        assertThat(emptyPage.hasNext()).isFalse();
        assertThat(emptyPage.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("페이지 범위를 벗어난 경우도 정상적으로 처리된다")
    void handleOutOfRangePage() {
        // given - 1개의 거래만 생성
        TransactionJpaEntity transaction = new TransactionJpaEntity(
                100, testAccount, null, new BigDecimal("1000"), new BigDecimal("1000"), BigDecimal.ZERO, "거래 1");
        transactionRepository.save(transaction);

        // when - 2페이지 요청 (존재하지 않는 페이지)
        Pageable pageable = PageRequest.of(1, 10);
        Page<TransactionJpaEntity> outOfRangePage = transactionRepository.findByAccountIdWithPaging(testAccount.getId(), pageable);

        log.info("Out of Range Page - Total: {}, TotalPages: {}, Number: {}, Size: {}",
                outOfRangePage.getTotalElements(), outOfRangePage.getTotalPages(), outOfRangePage.getNumber(), outOfRangePage.getSize());

        // then
        assertThat(outOfRangePage.getTotalElements()).isEqualTo(1);
        assertThat(outOfRangePage.getTotalPages()).isEqualTo(1);
        assertThat(outOfRangePage.getNumber()).isEqualTo(1); // 요청한 페이지 번호
        assertThat(outOfRangePage.getSize()).isEqualTo(10);
        assertThat(outOfRangePage.getContent()).isEmpty(); // 범위를 벗어나므로 빈 리스트
        assertThat(outOfRangePage.hasNext()).isFalse();
        assertThat(outOfRangePage.hasPrevious()).isTrue();
    }
}