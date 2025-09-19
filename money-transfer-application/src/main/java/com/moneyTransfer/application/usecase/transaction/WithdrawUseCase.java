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
import com.moneyTransfer.domain.transaction.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class WithdrawUseCase {

    private final AccountPort accountPort;
    private final TransactionPort transactionPort;
    private final DailyLimitPort dailyLimitPort;

    public TransactionResponse execute(WithdrawRequest request) {
        Account account = accountPort.findByIdWithLock(request.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));

        validateDailyLimit(account.getId(), request.getAmount());

        account.withdraw(request.getAmount());
        accountPort.save(account);

        updateDailyLimit(account.getId(), request.getAmount());

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
                savedTransaction.getAccountToId(),
                savedTransaction.getTransactionType(),
                savedTransaction.getAmount(),
                savedTransaction.getBalanceAfter(),
                savedTransaction.getDescription(),
                savedTransaction.getCreatedAt(),
                savedTransaction.getFee()
        );
    }


    private void validateDailyLimit(Long accountId, BigDecimal amount) {
        LocalDate today = LocalDate.now();
        DailyLimit dailyLimit = dailyLimitPort.findByAccountIdAndLimitDate(accountId, today)
                .orElse(DailyLimit.createNew(accountId, today));

        BigDecimal newTotalAmount = dailyLimit.getWithdrawUsed().add(amount);
        if (newTotalAmount.compareTo(BusinessConstants.DAILY_WITHDRAW_LIMIT) > 0) {
            throw new IllegalArgumentException(ErrorMessages.DAILY_WITHDRAW_LIMIT_EXCEEDED);
        }
    }

    private void updateDailyLimit(Long accountId, BigDecimal amount) {
        LocalDate today = LocalDate.now();
        DailyLimit dailyLimit = dailyLimitPort.findByAccountIdAndLimitDateWithLock(accountId, today)
                .orElse(DailyLimit.createNew(accountId, today));

        DailyLimit updatedLimit = dailyLimit.addWithdrawUsed(amount);
        dailyLimitPort.save(updatedLimit);
    }
}