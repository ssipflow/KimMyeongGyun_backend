package com.moneyTransfer.application.usecase.transaction;

import com.moneyTransfer.application.config.TestApplication;
import com.moneyTransfer.application.dto.account.CreateAccountRequest;
import com.moneyTransfer.application.dto.transaction.DepositRequest;
import com.moneyTransfer.application.dto.transaction.TransactionResponse;
import com.moneyTransfer.application.dto.transaction.TransferRequest;
import com.moneyTransfer.application.dto.transaction.WithdrawRequest;
import com.moneyTransfer.application.usecase.account.CreateAccountUseCase;
import com.moneyTransfer.application.usecase.account.GetAccountByBankCodeAndAccountNoUseCase;
import com.moneyTransfer.domain.account.AccountPort;
import com.moneyTransfer.domain.dailylimit.DailyLimitPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@DisplayName("Transaction UseCase 동시성 통합 테스트")
class TransactionUseCaseConcurrencyIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionUseCaseConcurrencyIntegrationTest.class);

    @Autowired
    private CreateAccountUseCase createAccountUseCase;

    @Autowired
    private DepositUseCase depositUseCase;

    @Autowired
    private WithdrawUseCase withdrawUseCase;

    @Autowired
    private TransferUseCase transferUseCase;

    @Autowired
    private GetAccountByBankCodeAndAccountNoUseCase getAccountByBankCodeAndAccountNoUseCase;

    @Autowired
    private AccountPort accountPort;

    @Autowired
    private DailyLimitPort dailyLimitPort;

    private final String testBankCode = "001";
    private String testAccountNo;
    private final String targetBankCode = "002";
    private String targetAccountNo;

    @BeforeEach
    void setUp() {
        // 각 테스트마다 고유한 계좌번호 생성
        long timestamp = System.currentTimeMillis();
        this.testAccountNo = "11" + (timestamp % 100000000L);
        this.targetAccountNo = "98" + (timestamp % 100000000L + 1);

        // 테스트용 계좌 생성
        CreateAccountRequest sourceAccountRequest = new CreateAccountRequest(
            "홍길동",
            "source" + timestamp + "@example.com",
            "1234567890" + (timestamp % 1000L),
            testBankCode,
            testAccountNo
        );

        CreateAccountRequest targetAccountRequest = new CreateAccountRequest(
            "김철수",
            "target" + timestamp + "@example.com",
            "9876543210" + (timestamp % 1000L),
            targetBankCode,
            targetAccountNo
        );

        createAccountUseCase.execute(sourceAccountRequest);
        createAccountUseCase.execute(targetAccountRequest);

        // 초기 잔액 설정 (200만원)
        DepositRequest depositRequest = new DepositRequest(
            testBankCode,
            testAccountNo,
            new BigDecimal("2000000"),
            "초기 잔액"
        );

        depositUseCase.execute(depositRequest);
    }

    @Test
    @DisplayName("동시 출금 시 낙관적 락과 일일 한도 제어가 작동하며, 실패 트랜잭션은 롤백된다")
    void withdraw_ConcurrentExecution_LockingAndDailyLimitWork() {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(3);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger optimisticLockFailures = new AtomicInteger(0);
        AtomicInteger dataIntegrityFailures = new AtomicInteger(0);
        AtomicInteger businessLogicFailures = new AtomicInteger(0);
        AtomicInteger successfullyWithdrawnAmount = new AtomicInteger(0); // 성공적으로 출금된 금액 합계

        // 초기 잔액 조회
        BigDecimal initialBalance = getAccountByBankCodeAndAccountNoUseCase.execute(testBankCode, testAccountNo)
            .orElseThrow(() -> new IllegalStateException("Test account not found")).getBalance();
        log.info("Initial balance: {}", initialBalance);

        WithdrawRequest withdrawRequest1 = new WithdrawRequest(
            testBankCode,
            testAccountNo,
            new BigDecimal("400000"), // 40만원
            "동시 출금 테스트 1"
        );

        WithdrawRequest withdrawRequest2 = new WithdrawRequest(
            testBankCode,
            testAccountNo,
            new BigDecimal("500000"), // 50만원
            "동시 출금 테스트 2"
        );

        WithdrawRequest withdrawRequest3 = new WithdrawRequest(
            testBankCode,
            testAccountNo,
            new BigDecimal("300000"), // 30만원
            "동시 출금 테스트 3"
        );

        // when - 동시에 3개의 출금 시도 (총 130만원, 한도 100만원)
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            try {
                withdrawUseCase.execute(withdrawRequest1);
                successCount.incrementAndGet();
                successfullyWithdrawnAmount.addAndGet(withdrawRequest1.getAmount().intValue());
                log.info("Withdraw 1 success: {} amount: {}", withdrawRequest1.getAmount());
            } catch (ObjectOptimisticLockingFailureException e) {
                optimisticLockFailures.incrementAndGet();
                log.info("Withdraw 1 optimistic lock failure: {}", e.getMessage());
            } catch (DataIntegrityViolationException e) {
                dataIntegrityFailures.incrementAndGet();
                log.info("Withdraw 1 data integrity failure: {}", e.getMessage());
            } catch (IllegalArgumentException e) {
                businessLogicFailures.incrementAndGet();
                log.info("Withdraw 1 business logic failure: {}", e.getMessage());
            }
        }, executor);

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            try {
                withdrawUseCase.execute(withdrawRequest2);
                successCount.incrementAndGet();
                successfullyWithdrawnAmount.addAndGet(withdrawRequest2.getAmount().intValue());
                log.info("Withdraw 2 success: {} amount: {}", withdrawRequest2.getAmount());
            } catch (ObjectOptimisticLockingFailureException e) {
                optimisticLockFailures.incrementAndGet();
                log.info("Withdraw 2 optimistic lock failure: {}", e.getMessage());
            } catch (DataIntegrityViolationException e) {
                dataIntegrityFailures.incrementAndGet();
                log.info("Withdraw 2 data integrity failure: {}", e.getMessage());
            } catch (IllegalArgumentException e) {
                businessLogicFailures.incrementAndGet();
                log.info("Withdraw 2 business logic failure: {}", e.getMessage());
            }
        }, executor);

        CompletableFuture<Void> future3 = CompletableFuture.runAsync(() -> {
            try {
                withdrawUseCase.execute(withdrawRequest3);
                successCount.incrementAndGet();
                successfullyWithdrawnAmount.addAndGet(withdrawRequest3.getAmount().intValue());
                log.info("Withdraw 3 success: {} amount: {}", withdrawRequest3.getAmount());
            } catch (ObjectOptimisticLockingFailureException e) {
                optimisticLockFailures.incrementAndGet();
                log.info("Withdraw 3 optimistic lock failure: {}", e.getMessage());
            } catch (DataIntegrityViolationException e) {
                dataIntegrityFailures.incrementAndGet();
                log.info("Withdraw 3 data integrity failure: {}", e.getMessage());
            } catch (IllegalArgumentException e) {
                businessLogicFailures.incrementAndGet();
                log.info("Withdraw 3 business logic failure: {}", e.getMessage());
            }
        }, executor);

        // then
        CompletableFuture.allOf(future1, future2, future3).join();

        int totalAttempts = successCount.get() + optimisticLockFailures.get() +
                           dataIntegrityFailures.get() + businessLogicFailures.get();

        log.info("=== 동시 출금 테스트 결과 ===");
        log.info("총 시도: {}", totalAttempts);
        log.info("성공: {}", successCount.get());
        log.info("낙관적 락 실패: {}", optimisticLockFailures.get());
        log.info("데이터 무결성 실패: {}", dataIntegrityFailures.get());
        log.info("비즈니스 로직 실패: {}", businessLogicFailures.get());
        log.info("성공적으로 출금된 금액 합계: {}", successfullyWithdrawnAmount.get());

        // 검증
        assertThat(totalAttempts).isEqualTo(3);

        // 일일 한도(100만원) 때문에 모든 거래가 성공할 수는 없음
        // 하지만 적어도 하나는 성공해야 함
        assertThat(successCount.get()).isGreaterThan(0);
        assertThat(successCount.get()).isLessThanOrEqualTo(2); // 최대 2개만 성공 가능 (40+50=90만원)

        // 동시성 제어가 작동했는지 확인 (실패가 발생해야 함)
        int totalFailures = optimisticLockFailures.get() + dataIntegrityFailures.get() + businessLogicFailures.get();
        assertThat(totalFailures).isGreaterThan(0);

        // 최종 잔액 검증: 초기 잔액 - 성공적으로 출금된 금액
        BigDecimal finalBalance = getAccountByBankCodeAndAccountNoUseCase.execute(testBankCode, testAccountNo)
            .orElseThrow(() -> new IllegalStateException("Test account not found")).getBalance();
        BigDecimal expectedFinalBalance = initialBalance.subtract(new BigDecimal(successfullyWithdrawnAmount.get()));
        log.info("Final balance: {}", finalBalance);
        log.info("Expected final balance: {}", expectedFinalBalance);
        assertThat(finalBalance).isEqualByComparingTo(expectedFinalBalance);

        // DailyLimit 롤백 검증: 실패한 트랜잭션의 DailyLimit는 롤백되어야 함
        Long accountId = getAccountByBankCodeAndAccountNoUseCase.execute(testBankCode, testAccountNo)
            .orElseThrow(() -> new IllegalStateException("Test account not found")).getId();

        BigDecimal finalDailyWithdrawAmount = dailyLimitPort.findByAccountIdAndLimitDate(accountId, java.time.LocalDate.now())
            .map(dailyLimit -> dailyLimit.getWithdrawUsed())
            .orElse(BigDecimal.ZERO);

        log.info("Final daily withdraw amount: {}", finalDailyWithdrawAmount);
        log.info("Expected daily withdraw amount: {}", successfullyWithdrawnAmount.get());

        // 성공적으로 출금된 금액만큼만 일일 한도에 반영되어야 함 (실패한 트랜잭션은 롤백)
        assertThat(finalDailyWithdrawAmount).isEqualByComparingTo(new BigDecimal(successfullyWithdrawnAmount.get()));

        executor.shutdown();
    }

    @Test
    @DisplayName("동시 입금 시 낙관적 락 제어가 작동한다")
    void deposit_ConcurrentExecution_OptimisticLockingWorks() {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger lockFailures = new AtomicInteger(0);

        DepositRequest depositRequest1 = new DepositRequest(
            testBankCode,
            testAccountNo,
            new BigDecimal("100000"),
            "동시 입금 테스트 1"
        );

        DepositRequest depositRequest2 = new DepositRequest(
            testBankCode,
            testAccountNo,
            new BigDecimal("150000"),
            "동시 입금 테스트 2"
        );

        // when - 동시에 입금 시도
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            try {
                TransactionResponse response = depositUseCase.execute(depositRequest1);
                successCount.incrementAndGet();
                log.info("Deposit 1 success: {}", response.getTransactionId());
            } catch (RuntimeException e) {
                lockFailures.incrementAndGet();
                log.info("Deposit 1 lock/runtime failure: {}", e.getMessage());
            }
        }, executor);

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            try {
                TransactionResponse response = depositUseCase.execute(depositRequest2);
                successCount.incrementAndGet();
                log.info("Deposit 2 success: {}", response.getTransactionId());
            } catch (RuntimeException e) {
                lockFailures.incrementAndGet();
                log.info("Deposit 2 lock/runtime failure: {}", e.getMessage());
            }
        }, executor);

        // then
        CompletableFuture.allOf(future1, future2).join();

        log.info("=== 동시 입금 테스트 결과 ===");
        log.info("성공: {}", successCount.get());
        log.info("락/런타임 실패: {}", lockFailures.get());

        // 입금은 한도 제한이 없으므로 둘 다 성공하거나, 하나는 낙관적 락으로 실패
        assertThat(successCount.get() + lockFailures.get()).isEqualTo(2);
        assertThat(successCount.get()).isGreaterThan(0); // 적어도 하나는 성공

        executor.shutdown();
    }

    @Test
    @DisplayName("동시 이체 시 복합적인 동시성 제어가 작동하며, DailyLimit 롤백이 정상 작동한다")
    void transfer_ConcurrentExecution_ComplexConcurrencyControlWorks() {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger successfulTransferAmount = new AtomicInteger(0);

        TransferRequest transferRequest1 = new TransferRequest(
            testBankCode,
            testAccountNo,
            targetBankCode,
            targetAccountNo,
            new BigDecimal("800000"), // 80만원
            "동시 이체 테스트 1"
        );

        TransferRequest transferRequest2 = new TransferRequest(
            testBankCode,
            testAccountNo,
            targetBankCode,
            targetAccountNo,
            new BigDecimal("1000000"), // 100만원
            "동시 이체 테스트 2"
        );

        // when - 동시에 이체 시도 (총 180만원 + 수수료, 잔액 200만원)
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            try {
                TransactionResponse response = transferUseCase.execute(transferRequest1);
                successCount.incrementAndGet();
                successfulTransferAmount.addAndGet(transferRequest1.getAmount().intValue());
                log.info("Transfer 1 success: {}", response.getTransactionId());
            } catch (Exception e) {
                failureCount.incrementAndGet();
                log.info("Transfer 1 failure: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            }
        }, executor);

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            try {
                TransactionResponse response = transferUseCase.execute(transferRequest2);
                successCount.incrementAndGet();
                successfulTransferAmount.addAndGet(transferRequest2.getAmount().intValue());
                log.info("Transfer 2 success: {}", response.getTransactionId());
            } catch (Exception e) {
                failureCount.incrementAndGet();
                log.info("Transfer 2 failure: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            }
        }, executor);

        // then
        CompletableFuture.allOf(future1, future2).join();

        log.info("=== 동시 이체 테스트 결과 ===");
        log.info("성공: {}", successCount.get());
        log.info("실패: {}", failureCount.get());

        // 검증: 동시성 제어가 올바르게 작동했는지
        int totalAttempts = successCount.get() + failureCount.get();
        assertThat(totalAttempts).isEqualTo(2);

        // 둘 다 성공하거나, 하나만 성공해야 함 (잔액과 일일 한도 고려)
        assertThat(successCount.get()).isLessThanOrEqualTo(1); // 최대 1개만 성공 가능

        // DailyLimit 이체 롤백 검증: 실패한 트랜잭션의 transferUsed는 롤백되어야 함
        Long accountId = getAccountByBankCodeAndAccountNoUseCase.execute(testBankCode, testAccountNo)
            .orElseThrow(() -> new IllegalStateException("Test account not found")).getId();

        BigDecimal finalDailyTransferAmount = dailyLimitPort.findByAccountIdAndLimitDate(accountId, java.time.LocalDate.now())
            .map(dailyLimit -> dailyLimit.getTransferUsed())
            .orElse(BigDecimal.ZERO);

        log.info("Final daily transfer amount: {}", finalDailyTransferAmount);
        log.info("Expected daily transfer amount: {}", successfulTransferAmount.get());

        // 성공적으로 이체된 금액만큼만 일일 한도에 반영되어야 함 (실패한 트랜잭션은 롤백)
        assertThat(finalDailyTransferAmount).isEqualByComparingTo(new BigDecimal(successfulTransferAmount.get()));

        executor.shutdown();
    }

    @Test
    @DisplayName("트랜잭션 경계 검증 - UseCase 레벨에서 원자성 보장")
    void transactionBoundary_UseCaseLevel_AtomicityGuaranteed() {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(5);
        AtomicInteger totalAttempts = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        // when - 동시에 많은 출금 시도 (트랜잭션 경계 테스트)
        CompletableFuture<Void>[] futures = new CompletableFuture[5];

        for (int i = 0; i < 5; i++) {
            final int index = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                totalAttempts.incrementAndGet();
                WithdrawRequest request = new WithdrawRequest(
                    testBankCode,
                    testAccountNo,
                    new BigDecimal("500000"), // 50만원씩 5번 시도
                    "트랜잭션 경계 테스트 " + index
                );

                try {
                    TransactionResponse response = withdrawUseCase.execute(request);
                    successCount.incrementAndGet();
                    log.info("Transaction boundary test {} success: {}", index, response.getTransactionId());
                } catch (Exception e) {
                    log.info("Transaction boundary test {} failure: {} - {}",
                            index, e.getClass().getSimpleName(), e.getMessage());
                }
            }, executor);
        }

        // then
        CompletableFuture.allOf(futures).join();

        log.info("=== 트랜잭션 경계 테스트 결과 ===");
        log.info("총 시도: {}", totalAttempts.get());
        log.info("성공: {}", successCount.get());
        log.info("실패: {}", totalAttempts.get() - successCount.get());

        // 검증: 트랜잭션 경계가 올바르게 작동했는지
        assertThat(totalAttempts.get()).isEqualTo(5);
        assertThat(successCount.get()).isLessThanOrEqualTo(2); // 일일 한도로 인해 최대 2개만 성공 가능
        assertThat(successCount.get()).isGreaterThan(0); // 적어도 하나는 성공해야 함

        executor.shutdown();
    }
}