package com.moneyTransfer.application.usecase.transaction;

import com.moneyTransfer.application.dto.transaction.GetTransactionHistoryRequest;
import com.moneyTransfer.application.dto.transaction.TransactionHistoryResponse;
import com.moneyTransfer.application.dto.transaction.TransactionResponse;
import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.common.util.StringNormalizer;
import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountPort;
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

    public TransactionHistoryResponse execute(GetTransactionHistoryRequest request) {
        // 1. bankCode + accountNo → Account 조회
        String accountNoNorm = StringNormalizer.normalizeAccountNo(request.getAccountNo());
        Account account = accountPort.findByBankCodeAndAccountNoNorm(request.getBankCode(), accountNoNorm)
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));

        // 2. Paging 객체 생성
        PageQuery pageQuery = PageQuery.of(request.getPage(), request.getSize());

        // 3. 거래내역 조회 (날짜 범위 조건 포함)
        PageResult<Transaction> transactionPage;
        if (request.getStartDate() != null && request.getEndDate() != null) {
            transactionPage = transactionPort.findByAccountIdAndDateRangeWithPaging(
                    account.getId(), request.getStartDate(), request.getEndDate(), pageQuery);
        } else {
            transactionPage = transactionPort.findByAccountIdWithPaging(account.getId(), pageQuery);
        }

        // 4. DTO 변환
        List<TransactionResponse> transactionResponses = transactionPage.getContent().stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());

        // 5. 페이징 정보 생성
        TransactionHistoryResponse.PageInfo pageInfo = new TransactionHistoryResponse.PageInfo(
                transactionPage.getPageNumber(),
                transactionPage.getPageSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages(),
                transactionPage.hasNext(),
                transactionPage.hasPrevious()
        );

        return new TransactionHistoryResponse(transactionResponses, pageInfo);
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getRelatedAccountId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getDescription(),
                transaction.getCreatedAt(),
                transaction.getFee()
        );
    }
}