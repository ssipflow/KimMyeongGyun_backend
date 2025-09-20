package com.moneyTransfer.application.usecase.transaction;

import com.moneyTransfer.application.dto.transaction.GetTransactionHistoryRequest;
import com.moneyTransfer.application.dto.transaction.TransactionHistoryResponse;
import com.moneyTransfer.application.dto.transaction.TransactionResponse;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.common.util.StringNormalizer;
import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
import com.moneyTransfer.domain.user.User;
import com.moneyTransfer.domain.user.UserPort;
import com.moneyTransfer.domain.common.PageResult;
import com.moneyTransfer.domain.common.PageQuery;
import com.moneyTransfer.domain.transaction.Transaction;
import com.moneyTransfer.domain.transaction.TransactionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTransactionHistoryUseCase {

    private final TransactionPort transactionPort;
    private final AccountPort accountPort;
    private final UserPort userPort;

    public TransactionHistoryResponse execute(GetTransactionHistoryRequest request) {
        // 1. bankCode + accountNo → Account 조회
        String accountNoNorm = StringNormalizer.normalizeAccountNo(request.getAccountNo());
        Account account = accountPort.findByBankCodeAndAccountNoNorm(request.getBankCode(), accountNoNorm)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));

        // 2. 계좌의 사용자 정보 조회
        User user = userPort.findById(account.getUserId())
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.USER_NOT_FOUND));

        // 3. Paging 객체 생성
        PageQuery pageQuery = PageQuery.of(request.getPage(), request.getSize());

        // 4. 거래내역 조회 (날짜 범위 조건 포함)
        PageResult<Transaction> transactionPage;
        if (request.getStartDate() != null && request.getEndDate() != null) {
            transactionPage = transactionPort.findByAccountIdAndDateRangeWithPaging(
                    account.getId(), request.getStartDate(), request.getEndDate(), pageQuery);
        } else {
            transactionPage = transactionPort.findByAccountIdWithPaging(account.getId(), pageQuery);
        }

        // 5. DTO 변환 (현재 계좌 정보 재사용)
        List<TransactionResponse> transactionResponses = transactionPage.getContent().stream()
                .map(transaction -> toTransactionResponse(transaction, account))
                .collect(Collectors.toList());

        // 6. 계좌 정보 생성
        TransactionHistoryResponse.AccountInfo accountInfo = new TransactionHistoryResponse.AccountInfo(
                user.getName(),
                user.getEmail(),
                account.getBalance(),
                account.getBankCode(),
                account.getAccountNo()
        );

        // 7. 페이징 정보 생성
        TransactionHistoryResponse.PageInfo pageInfo = new TransactionHistoryResponse.PageInfo(
                transactionPage.getPageNumber(),
                transactionPage.getPageSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages(),
                transactionPage.hasNext(),
                transactionPage.hasPrevious()
        );

        return new TransactionHistoryResponse(accountInfo, transactionResponses, pageInfo);
    }

    private TransactionResponse toTransactionResponse(Transaction transaction, Account currentAccount) {
        // 현재 조회된 계좌가 transaction의 계좌와 같은지 확인
        TransactionResponse.AccountInfo accountInfo;
        if (currentAccount.getId().equals(transaction.getAccountId())) {
            accountInfo = new TransactionResponse.AccountInfo(
                    currentAccount.getBankCode(),
                    currentAccount.getAccountNo()
            );
        } else {
            // 다른 계좌인 경우 조회 (이런 경우는 일반적으로 발생하지 않음)
            Account account = accountPort.findById(transaction.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));
            accountInfo = new TransactionResponse.AccountInfo(
                    account.getBankCode(),
                    account.getAccountNo()
            );
        }

        // 관련 계좌 정보 조회 (이체인 경우에만)
        TransactionResponse.AccountInfo relatedAccountInfo = null;
        if (transaction.getRelatedAccountId() != null) {
            Account relatedAccount = accountPort.findById(transaction.getRelatedAccountId())
                    .orElse(null); // 관련 계좌가 없어도 예외를 발생시키지 않음
            if (relatedAccount != null) {
                relatedAccountInfo = new TransactionResponse.AccountInfo(
                        relatedAccount.getBankCode(),
                        relatedAccount.getAccountNo()
                );
            }
        }

        return new TransactionResponse(
                transaction.getId(),
                accountInfo,
                relatedAccountInfo,
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getDescription(),
                transaction.getCreatedAt(),
                transaction.getFee()
        );
    }
}