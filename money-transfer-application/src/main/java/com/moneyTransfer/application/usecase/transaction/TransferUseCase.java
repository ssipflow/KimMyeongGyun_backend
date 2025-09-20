package com.moneyTransfer.application.usecase.transaction;

import com.moneyTransfer.application.dto.transaction.TransactionResponse;
import com.moneyTransfer.application.dto.transaction.TransferRequest;
import com.moneyTransfer.common.constant.BusinessConstants;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.common.util.StringNormalizer;
import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
import com.moneyTransfer.domain.dailylimit.DailyLimit;
import com.moneyTransfer.domain.dailylimit.DailyLimitPort;
import com.moneyTransfer.domain.transaction.Transaction;
import com.moneyTransfer.domain.transaction.TransactionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class TransferUseCase {

    private final AccountPort accountPort;
    private final TransactionPort transactionPort;
    private final DailyLimitPort dailyLimitPort;

    public TransactionResponse execute(TransferRequest request) {
        // 1. bankCode + accountNo → Account 조회
        String fromAccountNoNorm = StringNormalizer.normalizeAccountNo(request.getFromAccountNo());
        String toAccountNoNorm = StringNormalizer.normalizeAccountNo(request.getToAccountNo());

        Account fromAccount = accountPort.findByBankCodeAndAccountNoNorm(request.getFromBankCode(), fromAccountNoNorm)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));
        Account toAccount = accountPort.findByBankCodeAndAccountNoNorm(request.getToBankCode(), toAccountNoNorm)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.TARGET_ACCOUNT_NOT_FOUND));

        // 2. 동일 계좌 체크
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new IllegalArgumentException(ErrorMessages.CANNOT_TRANSFER_TO_SAME_ACCOUNT);
        }

        BigDecimal fee = calculateFee(request.getAmount());
        BigDecimal totalDeduction = request.getAmount().add(fee);

        // 3. 일일 한도 미리 확인 및 Lock (데드락 방지)
        validateAndLockDailyLimit(fromAccount.getId(), request.getAmount());

        // 4. 계좌 Lock - ID 순서대로 Lock하여 데드락 방지
        if (fromAccount.getId() < toAccount.getId()) {
            fromAccount = accountPort.findByIdWithLock(fromAccount.getId())
                    .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));
            toAccount = accountPort.findByIdWithLock(toAccount.getId())
                    .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.TARGET_ACCOUNT_NOT_FOUND));
        } else {
            toAccount = accountPort.findByIdWithLock(toAccount.getId())
                    .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.TARGET_ACCOUNT_NOT_FOUND));
            fromAccount = accountPort.findByIdWithLock(fromAccount.getId())
                    .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));
        }

        // 5. 잔액 검증
        if (!fromAccount.canWithdraw(totalDeduction)) {
            throw new IllegalArgumentException(ErrorMessages.INSUFFICIENT_BALANCE);
        }

        // 6. 계좌 잔액 변경 (원자적 실행)
        fromAccount.withdraw(totalDeduction);
        toAccount.deposit(request.getAmount());

        // 7. 계좌 업데이트
        accountPort.save(fromAccount);
        accountPort.save(toAccount);

        // 8. 거래 기록 생성 (원자적 실행)
        Transaction transferSendTransaction = Transaction.createTransferSend(
                fromAccount.getId(),
                toAccount.getId(),
                request.getAmount(),
                fee,
                request.getDescription()
        );
        transferSendTransaction.setBalanceAfter(fromAccount.getBalance());
        Transaction savedSendTransaction = transactionPort.save(transferSendTransaction);

        Transaction transferReceiveTransaction = Transaction.createTransferReceive(
                toAccount.getId(),
                fromAccount.getId(),
                request.getAmount(),
                request.getDescription()
        );
        transferReceiveTransaction.setBalanceAfter(toAccount.getBalance());
        transactionPort.save(transferReceiveTransaction);

        TransactionResponse.AccountInfo fromAccountInfo = new TransactionResponse.AccountInfo(
                fromAccount.getBankCode(),
                fromAccount.getAccountNo()
        );

        TransactionResponse.AccountInfo toAccountInfo = new TransactionResponse.AccountInfo(
                toAccount.getBankCode(),
                toAccount.getAccountNo()
        );

        return new TransactionResponse(
                savedSendTransaction.getId(),
                fromAccountInfo,
                toAccountInfo,
                savedSendTransaction.getTransactionType(),
                savedSendTransaction.getAmount(),
                savedSendTransaction.getBalanceAfter(),
                savedSendTransaction.getDescription(),
                savedSendTransaction.getCreatedAt(),
                savedSendTransaction.getFee()
        );
    }

    private BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(BusinessConstants.TRANSFER_FEE_RATE)
                .setScale(0, RoundingMode.HALF_UP);
    }


    private void validateAndLockDailyLimit(Long accountId, BigDecimal amount) {
        LocalDate today = LocalDate.now();
        // 미리 Lock을 걸어서 동시성 문제 방지
        DailyLimit dailyLimit = dailyLimitPort.findByAccountIdAndLimitDateWithLock(accountId, today)
                .orElse(DailyLimit.createNew(accountId, today));

        // 한도 검증
        BigDecimal newTotalAmount = dailyLimit.getTransferUsed().add(amount);
        if (newTotalAmount.compareTo(BusinessConstants.DAILY_TRANSFER_LIMIT) > 0) {
            throw new IllegalArgumentException(ErrorMessages.DAILY_TRANSFER_LIMIT_EXCEEDED);
        }

        // 사용량 업데이트 및 저장
        DailyLimit updatedLimit = dailyLimit.addTransferUsed(amount);
        dailyLimitPort.save(updatedLimit);
    }
}