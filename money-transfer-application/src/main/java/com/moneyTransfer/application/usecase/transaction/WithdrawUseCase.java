package com.moneyTransfer.application.usecase.transaction;

import com.moneyTransfer.application.dto.transaction.TransactionResponse;
import com.moneyTransfer.application.dto.transaction.WithdrawRequest;
import com.moneyTransfer.common.constant.BusinessConstants;
import com.moneyTransfer.common.constant.ErrorMessages;
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
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class WithdrawUseCase {

    private final AccountPort accountPort;
    private final TransactionPort transactionPort;
    private final DailyLimitPort dailyLimitPort;

    public TransactionResponse execute(WithdrawRequest request) {
        // 1. 일일 한도 미리 확인 및 Lock (데드락 방지)
        validateAndLockDailyLimit(request.getAccountId(), request.getAmount());

        // 2. 계좌 Lock 및 잔액 검증
        Account account = accountPort.findByIdWithLock(request.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));

        // 3. 출금 실행
        account.withdraw(request.getAmount());
        accountPort.save(account);

        // 4. 거래 기록 생성
        Transaction transaction = Transaction.createWithdraw(
                account.getId(),
                request.getAmount(),
                request.getDescription()
        );
        transaction.setBalanceAfter(account.getBalance());
        Transaction savedTransaction = transactionPort.save(transaction);

        return new TransactionResponse(
                savedTransaction.getId(),
                savedTransaction.getAccountId(),
                savedTransaction.getRelatedAccountId(),
                savedTransaction.getTransactionType(),
                savedTransaction.getAmount(),
                savedTransaction.getBalanceAfter(),
                savedTransaction.getDescription(),
                savedTransaction.getCreatedAt(),
                savedTransaction.getFee()
        );
    }


    private void validateAndLockDailyLimit(Long accountId, BigDecimal amount) {
        LocalDate today = LocalDate.now();
        // 미리 Lock을 걸어서 동시성 문제 방지
        DailyLimit dailyLimit = dailyLimitPort.findByAccountIdAndLimitDateWithLock(accountId, today)
                .orElse(DailyLimit.createNew(accountId, today));

        // 한도 검증
        BigDecimal newTotalAmount = dailyLimit.getWithdrawUsed().add(amount);
        if (newTotalAmount.compareTo(BusinessConstants.DAILY_WITHDRAW_LIMIT) > 0) {
            throw new IllegalArgumentException(ErrorMessages.DAILY_WITHDRAW_LIMIT_EXCEEDED);
        }

        // 사용량 업데이트 및 저장
        DailyLimit updatedLimit = dailyLimit.addWithdrawUsed(amount);
        dailyLimitPort.save(updatedLimit);
    }
}