package com.moneyTransfer.application.usecase.transaction;

import com.moneyTransfer.application.dto.transaction.TransactionResponse;
import com.moneyTransfer.application.dto.transaction.WithdrawRequest;
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
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class WithdrawUseCase {

    private final AccountPort accountPort;
    private final TransactionPort transactionPort;
    private final DailyLimitPort dailyLimitPort;

    public TransactionResponse execute(WithdrawRequest request) {
        // 1. bankCode + accountNo → Account 조회
        String accountNoNorm = StringNormalizer.normalizeAccountNo(request.getAccountNo());
        Account account = accountPort.findByBankCodeAndAccountNoNorm(request.getBankCode(), accountNoNorm)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));

        // 2. 일일 한도 미리 확인 및 Lock (데드락 방지)
        validateAndLockDailyLimit(account.getId(), request.getAmount());

        // 3. 비관적 락으로 계좌 다시 조회
        account = accountPort.findByIdWithLock(account.getId())
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

        TransactionResponse.AccountInfo accountInfo = new TransactionResponse.AccountInfo(
                account.getBankCode(),
                account.getAccountNo()
        );

        return new TransactionResponse(
                savedTransaction.getId(),
                accountInfo,
                null, // 출금은 관련 계좌 없음
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