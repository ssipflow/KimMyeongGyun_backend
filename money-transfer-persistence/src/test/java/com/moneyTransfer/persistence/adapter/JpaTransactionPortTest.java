package com.moneyTransfer.persistence.adapter;

import com.moneyTransfer.domain.transaction.Transaction;
import com.moneyTransfer.domain.transaction.TransactionType;
import com.moneyTransfer.persistence.entity.AccountJpaEntity;
import com.moneyTransfer.persistence.entity.UserJpaEntity;
import com.moneyTransfer.persistence.repository.AccountJpaRepository;
import com.moneyTransfer.persistence.repository.TransactionJpaRepository;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaTransactionPort.class) // Port 구현체를 테스트 컨텍스트에 포함
class JpaTransactionPortTest {

    private static final Logger log = LoggerFactory.getLogger(JpaTransactionPortTest.class);

    @Autowired
    private JpaTransactionPort transactionPort;

    @Autowired
    private AccountJpaRepository accountRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private TransactionJpaRepository transactionJpaRepository;

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
    @DisplayName("입금 거래를 저장할 수 있다")
    void saveDepositTransaction() {
        // given
        Transaction deposit = Transaction.createDeposit(
                testAccount.getId(),
                new BigDecimal("50000"),
                "급여 입금"
        );

        // when
        Transaction savedTransaction = transactionPort.save(deposit);

        // 저장된 도메인 정보 로깅
        log.info("Saved Transaction: id={}, accountId={}, type={}, amount={}, fee={}, description={}",
                savedTransaction.getId(), savedTransaction.getAccountId(), savedTransaction.getType(),
                savedTransaction.getAmount(), savedTransaction.getFee(), savedTransaction.getDescription());

        // then
        assertThat(savedTransaction.getId()).isNotNull();
        assertThat(savedTransaction.getAccountId()).isEqualTo(testAccount.getId());
        assertThat(savedTransaction.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(savedTransaction.getAmount()).isEqualTo(new BigDecimal("50000"));
        assertThat(savedTransaction.getFee()).isEqualTo(BigDecimal.ZERO);
        assertThat(savedTransaction.getAccountToNo()).isNull();
        assertThat(savedTransaction.getDescription()).isEqualTo("급여 입금");
        assertThat(savedTransaction.getCreatedAt()).isNotNull();
        assertThat(savedTransaction.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("출금 거래를 저장할 수 있다")
    void saveWithdrawTransaction() {
        // given
        Transaction withdraw = Transaction.createWithdraw(
                testAccount.getId(),
                new BigDecimal("30000"),
                "ATM 출금"
        );

        // when
        Transaction savedTransaction = transactionPort.save(withdraw);

        // 저장된 도메인 정보 로깅
        log.info("Saved Transaction: id={}, accountId={}, type={}, amount={}, fee={}, description={}",
                savedTransaction.getId(), savedTransaction.getAccountId(), savedTransaction.getType(),
                savedTransaction.getAmount(), savedTransaction.getFee(), savedTransaction.getDescription());

        // then
        assertThat(savedTransaction.getId()).isNotNull();
        assertThat(savedTransaction.getAccountId()).isEqualTo(testAccount.getId());
        assertThat(savedTransaction.getType()).isEqualTo(TransactionType.WITHDRAW);
        assertThat(savedTransaction.getAmount()).isEqualTo(new BigDecimal("30000"));
        assertThat(savedTransaction.getFee()).isEqualTo(BigDecimal.ZERO);
        assertThat(savedTransaction.getAccountToNo()).isNull();
        assertThat(savedTransaction.getDescription()).isEqualTo("ATM 출금");
    }

    @Test
    @DisplayName("이체 거래를 저장할 수 있다")
    void saveTransferTransaction() {
        // given
        Transaction transfer = Transaction.createTransfer(
                testAccount.getId(),
                "002-987654321",
                new BigDecimal("100000"),
                new BigDecimal("1000"),
                "김철수에게 이체"
        );

        // when
        Transaction savedTransaction = transactionPort.save(transfer);

        // 저장된 도메인 정보 로깅
        log.info("Saved Transaction: id={}, accountId={}, type={}, accountToNo={}, amount={}, fee={}, description={}",
                savedTransaction.getId(), savedTransaction.getAccountId(), savedTransaction.getType(),
                savedTransaction.getAccountToNo(), savedTransaction.getAmount(), savedTransaction.getFee(),
                savedTransaction.getDescription());

        // then
        assertThat(savedTransaction.getId()).isNotNull();
        assertThat(savedTransaction.getAccountId()).isEqualTo(testAccount.getId());
        assertThat(savedTransaction.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(savedTransaction.getAccountToNo()).isEqualTo("002-987654321");
        assertThat(savedTransaction.getAmount()).isEqualTo(new BigDecimal("100000"));
        assertThat(savedTransaction.getFee()).isEqualTo(new BigDecimal("1000"));
        assertThat(savedTransaction.getDescription()).isEqualTo("김철수에게 이체");
    }

    @Test
    @DisplayName("존재하지 않는 계좌로 거래 저장 시 예외가 발생한다")
    void saveTransactionWithNonExistentAccount() {
        // given
        Transaction deposit = Transaction.createDeposit(
                9999L, // 존재하지 않는 계좌
                new BigDecimal("10000"),
                "테스트 입금"
        );

        // when & then
        assertThatThrownBy(() -> transactionPort.save(deposit))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("계좌를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("계좌별 거래내역을 조회할 수 있다")
    void findByAccountId() {
        // given
        Transaction deposit = Transaction.createDeposit(testAccount.getId(), new BigDecimal("50000"), "입금");
        Transaction withdraw = Transaction.createWithdraw(testAccount.getId(), new BigDecimal("20000"), "출금");
        Transaction transfer = Transaction.createTransfer(testAccount.getId(), "002-987654321",
                new BigDecimal("30000"), new BigDecimal("300"), "이체");

        transactionPort.save(deposit);
        transactionPort.save(withdraw);
        transactionPort.save(transfer);

        // when
        List<Transaction> transactions = transactionPort.findByAccountId(testAccount.getId());

        // 조회된 도메인 정보 로깅
        transactions.forEach(t -> log.info("Found Transaction: id={}, accountId={}, type={}, amount={}, description={}",
                t.getId(), t.getAccountId(), t.getType(), t.getAmount(), t.getDescription()));

        // then
        assertThat(transactions).hasSize(3);
        // 생성일 역순으로 정렬되어야 함
        assertThat(transactions.get(0).getType()).isEqualTo(TransactionType.TRANSFER); // 가장 최근
        assertThat(transactions.get(1).getType()).isEqualTo(TransactionType.WITHDRAW);
        assertThat(transactions.get(2).getType()).isEqualTo(TransactionType.DEPOSIT); // 가장 오래됨
    }

    @Test
    @DisplayName("계좌별 거래내역을 생성일 역순으로 조회할 수 있다")
    void findByAccountIdOrderByCreatedAtDesc() {
        // given
        Transaction first = Transaction.createDeposit(testAccount.getId(), new BigDecimal("10000"), "첫 번째");
        Transaction second = Transaction.createWithdraw(testAccount.getId(), new BigDecimal("5000"), "두 번째");
        Transaction third = Transaction.createTransfer(testAccount.getId(), "002-987654321",
                new BigDecimal("3000"), new BigDecimal("30"), "세 번째");

        try {
            transactionPort.save(first);
            Thread.sleep(10); // 시간 차이를 두기 위해
            transactionPort.save(second);
            Thread.sleep(10);
            transactionPort.save(third);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // when
        List<Transaction> transactions = transactionPort.findByAccountIdOrderByCreatedAtDesc(testAccount.getId());

        // 조회된 도메인 정보 로깅
        transactions.forEach(t -> log.info("Found Transaction in order: id={}, type={}, description={}, createdAt={}",
                t.getId(), t.getType(), t.getDescription(), t.getCreatedAt()));

        // then
        assertThat(transactions).hasSize(3);
        assertThat(transactions.get(0).getDescription()).isEqualTo("세 번째");
        assertThat(transactions.get(1).getDescription()).isEqualTo("두 번째");
        assertThat(transactions.get(2).getDescription()).isEqualTo("첫 번째");
    }

    @Test
    @DisplayName("날짜 범위로 거래내역을 조회할 수 있다")
    void findByAccountIdAndDateRange() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        Transaction todayTransaction = Transaction.createDeposit(testAccount.getId(), new BigDecimal("10000"), "오늘 거래");
        Transaction savedToday = transactionPort.save(todayTransaction);

        // 어제 거래를 시뮬레이션하기 위해 저장 후 직접 생성일 수정
        transactionJpaRepository.findById(savedToday.getId()).ifPresent(entity -> {
            entity.setCreatedAt(yesterday.atStartOfDay());
            transactionJpaRepository.save(entity);
        });

        Transaction recentTransaction = Transaction.createWithdraw(testAccount.getId(), new BigDecimal("5000"), "최근 거래");
        transactionPort.save(recentTransaction);

        // when - 오늘 거래만 조회
        List<Transaction> todayTransactions = transactionPort.findByAccountIdAndDateRange(
                testAccount.getId(), today, today);

        // 조회된 도메인 정보 로깅
        todayTransactions.forEach(t -> log.info("Today Transaction: id={}, description={}, createdAt={}",
                t.getId(), t.getDescription(), t.getCreatedAt()));

        // then
        assertThat(todayTransactions).hasSize(1);
        assertThat(todayTransactions.get(0).getDescription()).isEqualTo("최근 거래");
    }

    @Test
    @DisplayName("다른 계좌의 거래내역은 조회되지 않는다")
    void findByAccountIdShouldNotReturnOtherAccountTransactions() {
        // given
        Transaction testAccountTransaction = Transaction.createDeposit(testAccount.getId(),
                new BigDecimal("10000"), "testAccount 거래");
        Transaction targetAccountTransaction = Transaction.createDeposit(targetAccount.getId(),
                new BigDecimal("5000"), "targetAccount 거래");

        transactionPort.save(testAccountTransaction);
        transactionPort.save(targetAccountTransaction);

        // when
        List<Transaction> testAccountTransactions = transactionPort.findByAccountId(testAccount.getId());
        List<Transaction> targetAccountTransactions = transactionPort.findByAccountId(targetAccount.getId());

        // then
        assertThat(testAccountTransactions).hasSize(1);
        assertThat(testAccountTransactions.get(0).getDescription()).isEqualTo("testAccount 거래");

        assertThat(targetAccountTransactions).hasSize(1);
        assertThat(targetAccountTransactions.get(0).getDescription()).isEqualTo("targetAccount 거래");
    }

    @Test
    @DisplayName("존재하지 않는 계좌의 거래내역 조회 시 빈 결과를 반환한다")
    void findByAccountIdReturnsEmptyForNonExistentAccount() {
        // when
        List<Transaction> transactions = transactionPort.findByAccountId(9999L);

        // then
        assertThat(transactions).isEmpty();
    }
}