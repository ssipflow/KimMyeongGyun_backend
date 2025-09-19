package com.moneyTransfer.application.usecase.transaction;

import com.moneyTransfer.application.dto.transaction.TransactionResponse;
import com.moneyTransfer.application.dto.transaction.TransferRequest;
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
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new IllegalArgumentException(ErrorMessages.CANNOT_TRANSFER_TO_SAME_ACCOUNT);
        }

        Account fromAccount = accountPort.findByIdWithLock(request.getFromAccountId())
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));

        Account toAccount = accountPort.findByIdWithLock(request.getToAccountId())
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.TARGET_ACCOUNT_NOT_FOUND));

        BigDecimal fee = calculateFee(request.getAmount());
        BigDecimal totalDeduction = request.getAmount().add(fee);

        // 잔액 및 일일 한도 검증
        if (!fromAccount.canWithdraw(totalDeduction)) {
            throw new IllegalArgumentException(ErrorMessages.INSUFFICIENT_BALANCE);
        }
        validateDailyLimit(fromAccount.getId(), request.getAmount());

        // 출금 계좌에서 이체 금액 + 수수료 차감
        fromAccount.withdraw(totalDeduction);
        accountPort.save(fromAccount);

        // 입금 계좌에 이체 금액 추가
        toAccount.deposit(request.getAmount());
        accountPort.save(toAccount);

        updateDailyLimit(fromAccount.getId(), request.getAmount());

        // 출금 거래 기록 (TRANSFER_SEND)
        Transaction transferSendTransaction = Transaction.createTransferSend(
                fromAccount.getId(),
                toAccount.getId(),
                request.getAmount(),
                fee,
                request.getDescription()
        );
        transferSendTransaction.setBalanceAfter(fromAccount.getBalance());
        Transaction savedSendTransaction = transactionPort.save(transferSendTransaction);

        // 입금 거래 기록 (TRANSFER_RECEIVE)
        Transaction transferReceiveTransaction = Transaction.createTransferReceive(
                toAccount.getId(),
                fromAccount.getId(),
                request.getAmount(),
                request.getDescription()
        );
        transferReceiveTransaction.setBalanceAfter(toAccount.getBalance());
        transactionPort.save(transferReceiveTransaction);

        return new TransactionResponse(
                savedSendTransaction.getId(),
                savedSendTransaction.getAccountId(),
                savedSendTransaction.getAccountToId(),
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


    private void validateDailyLimit(Long accountId, BigDecimal amount) {
        LocalDate today = LocalDate.now();
        DailyLimit dailyLimit = dailyLimitPort.findByAccountIdAndLimitDate(accountId, today)
                .orElse(DailyLimit.createNew(accountId, today));

        BigDecimal newTotalAmount = dailyLimit.getTransferUsed().add(amount);
        if (newTotalAmount.compareTo(BusinessConstants.DAILY_TRANSFER_LIMIT) > 0) {
            throw new IllegalArgumentException(ErrorMessages.DAILY_TRANSFER_LIMIT_EXCEEDED);
        }
    }

    private void updateDailyLimit(Long accountId, BigDecimal amount) {
        LocalDate today = LocalDate.now();
        DailyLimit dailyLimit = dailyLimitPort.findByAccountIdAndLimitDateWithLock(accountId, today)
                .orElse(DailyLimit.createNew(accountId, today));

        DailyLimit updatedLimit = dailyLimit.addTransferUsed(amount);
        dailyLimitPort.save(updatedLimit);
    }
}