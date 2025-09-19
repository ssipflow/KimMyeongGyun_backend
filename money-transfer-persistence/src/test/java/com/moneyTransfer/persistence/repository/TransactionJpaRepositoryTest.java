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
    @DisplayName("이체 수금 거래를 저장하고 조회할 수 있다")
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
    @DisplayName("TRANSFER_SEND에서 accountId는 출금계좌, relatedAccountId는 입금계좌를 나타낸다")
    void transferSendAccountRole() {
        // given
        TransactionJpaEntity transferSend = new TransactionJpaEntity(
                300, // TRANSFER_SEND
                testAccount,    // 출금 계좌 (주체)
                targetAccount,  // 입금 계좌 (연관)
                new BigDecimal("10000"),
                new BigDecimal("90000"),
                new BigDecimal("100"),
                "이체 송금"
        );

        // when
        TransactionJpaEntity saved = transactionRepository.save(transferSend);

        // then
        assertThat(saved.getType()).isEqualTo(300);
        assertThat(saved.getAccount().getId()).isEqualTo(testAccount.getId()); // 출금 계좌
        assertThat(saved.getRelatedAccount().getId()).isEqualTo(targetAccount.getId()); // 입금 계좌
    }

    @Test
    @DisplayName("TRANSFER_RECEIVE에서 accountId는 입금계좌, relatedAccountId는 출금계좌를 나타낸다")
    void transferReceiveAccountRole() {
        // given
        TransactionJpaEntity transferReceive = new TransactionJpaEntity(
                400, // TRANSFER_RECEIVE
                targetAccount,  // 입금 계좌 (주체)
                testAccount,    // 출금 계좌 (연관)
                new BigDecimal("10000"),
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                "이체 수금"
        );

        // when
        TransactionJpaEntity saved = transactionRepository.save(transferReceive);

        // then
        assertThat(saved.getType()).isEqualTo(400);
        assertThat(saved.getAccount().getId()).isEqualTo(targetAccount.getId()); // 입금 계좌
        assertThat(saved.getRelatedAccount().getId()).isEqualTo(testAccount.getId()); // 출금 계좌
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
        TransactionJpaEntity saved = transactionRepository.save(transfer);

        // when - ID로만 조회
        TransactionJpaEntity found = transactionRepository.findById(saved.getId()).orElseThrow();

        // then - 연관 엔티티들이 Lazy Loading으로 처리되는지 확인
        assertThat(found.getAccount()).isNotNull();
        assertThat(found.getRelatedAccount()).isNotNull();

        // 실제 데이터 접근 시 로딩 확인
        assertThat(found.getAccount().getUser().getName()).isEqualTo("홍길동");
        assertThat(found.getRelatedAccount().getUser().getName()).isEqualTo("김철수");
    }
}