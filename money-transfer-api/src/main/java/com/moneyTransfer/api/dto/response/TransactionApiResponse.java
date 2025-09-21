package com.moneyTransfer.api.dto.response;

import com.moneyTransfer.domain.transaction.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "거래 API 응답")
public class TransactionApiResponse {

    @Schema(description = "거래 ID", example = "1")
    private Long transactionId;

    @Schema(description = "계좌 정보")
    private AccountInfo accountInfo;

    @Schema(description = "관련 계좌 정보")
    private AccountInfo relatedAccountInfo;

    @Schema(description = "거래 유형")
    private TransactionType transactionType;

    @Schema(description = "거래 금액", example = "100000")
    private BigDecimal amount;

    @Schema(description = "거래 후 잔액", example = "500000")
    private BigDecimal balanceAfter;

    @Schema(description = "거래 사유", example = "친구에게 이체")
    private String description;

    @Schema(description = "거래 시간")
    private LocalDateTime createdAt;

    @Schema(description = "수수료", example = "1000")
    private BigDecimal fee;

    public TransactionApiResponse(Long transactionId, AccountInfo accountInfo, AccountInfo relatedAccountInfo,
                                 TransactionType transactionType, BigDecimal amount, BigDecimal balanceAfter,
                                 String description, LocalDateTime createdAt, BigDecimal fee) {
        this.transactionId = transactionId;
        this.accountInfo = accountInfo;
        this.relatedAccountInfo = relatedAccountInfo;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.createdAt = createdAt;
        this.fee = fee;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "계좌 정보")
    public static class AccountInfo {

        @Schema(description = "은행 코드", example = "001")
        private String bankCode;

        @Schema(description = "계좌 번호", example = "123-456-7891")
        private String accountNo;

        public AccountInfo(String bankCode, String accountNo) {
            this.bankCode = bankCode;
            this.accountNo = accountNo;
        }
    }
}