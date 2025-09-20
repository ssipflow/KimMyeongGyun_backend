package com.moneyTransfer.api.mapper;

import com.moneyTransfer.api.dto.request.DepositApiRequest;
import com.moneyTransfer.api.dto.request.TransferApiRequest;
import com.moneyTransfer.api.dto.request.WithdrawApiRequest;
import com.moneyTransfer.api.dto.response.TransactionApiResponse;
import com.moneyTransfer.api.dto.response.TransactionHistoryApiResponse;
import com.moneyTransfer.application.dto.transaction.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class TransactionDtoMapper {

    public DepositRequest toApplicationRequest(DepositApiRequest apiRequest) {
        return new DepositRequest(
                apiRequest.getBankCode(),
                apiRequest.getAccountNo(),
                apiRequest.getAmount(),
                apiRequest.getDescription()
        );
    }

    public WithdrawRequest toApplicationRequest(WithdrawApiRequest apiRequest) {
        return new WithdrawRequest(
                apiRequest.getBankCode(),
                apiRequest.getAccountNo(),
                apiRequest.getAmount(),
                apiRequest.getDescription()
        );
    }

    public TransferRequest toApplicationRequest(TransferApiRequest apiRequest) {
        return new TransferRequest(
                apiRequest.getFromBankCode(),
                apiRequest.getFromAccountNo(),
                apiRequest.getToBankCode(),
                apiRequest.getToAccountNo(),
                apiRequest.getAmount(),
                apiRequest.getDescription()
        );
    }

    public GetTransactionHistoryRequest toApplicationRequest(String bankCode, String accountNo,
                                                           Integer page, Integer size,
                                                           LocalDateTime startDate, LocalDateTime endDate) {
        return new GetTransactionHistoryRequest(
                bankCode,
                accountNo,
                page != null ? page : 0,
                size != null ? size : 10,
                startDate,
                endDate
        );
    }

    public TransactionApiResponse toApiResponse(TransactionResponse applicationResponse) {
        TransactionApiResponse.AccountInfo accountInfo = new TransactionApiResponse.AccountInfo(
                applicationResponse.getAccountInfo().getBankCode(),
                applicationResponse.getAccountInfo().getAccountNo()
        );

        TransactionApiResponse.AccountInfo relatedAccountInfo = null;
        if (applicationResponse.getRelatedAccountInfo() != null) {
            relatedAccountInfo = new TransactionApiResponse.AccountInfo(
                    applicationResponse.getRelatedAccountInfo().getBankCode(),
                    applicationResponse.getRelatedAccountInfo().getAccountNo()
            );
        }

        return new TransactionApiResponse(
                applicationResponse.getTransactionId(),
                accountInfo,
                relatedAccountInfo,
                applicationResponse.getTransactionType(),
                applicationResponse.getAmount(),
                applicationResponse.getBalanceAfter(),
                applicationResponse.getDescription(),
                applicationResponse.getCreatedAt(),
                applicationResponse.getFee()
        );
    }

    public TransactionHistoryApiResponse toApiResponse(TransactionHistoryResponse applicationResponse) {
        TransactionHistoryApiResponse.AccountInfoApiResponse accountInfo =
                new TransactionHistoryApiResponse.AccountInfoApiResponse(
                        applicationResponse.getAccountInfo().getUserName(),
                        applicationResponse.getAccountInfo().getEmail(),
                        applicationResponse.getAccountInfo().getBalance(),
                        applicationResponse.getAccountInfo().getBankCode(),
                        applicationResponse.getAccountInfo().getAccountNo()
                );

        List<TransactionApiResponse> transactions = applicationResponse.getTransactions()
                .stream()
                .map(this::toApiResponse)
                .toList();

        TransactionHistoryApiResponse.PageInfoApiResponse pageInfo =
                new TransactionHistoryApiResponse.PageInfoApiResponse(
                        applicationResponse.getPageInfo().getCurrentPage(),
                        applicationResponse.getPageInfo().getPageSize(),
                        applicationResponse.getPageInfo().getTotalElements(),
                        applicationResponse.getPageInfo().getTotalPages(),
                        applicationResponse.getPageInfo().getHasNext(),
                        applicationResponse.getPageInfo().getHasPrevious()
                );

        return new TransactionHistoryApiResponse(accountInfo, transactions, pageInfo);
    }
}