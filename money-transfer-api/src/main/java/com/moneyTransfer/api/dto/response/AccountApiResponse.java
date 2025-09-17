package com.moneyTransfer.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AccountApiResponse {

    private Long id;
    private Long userId;
    private String bankCode;
    private String accountNo;
    private BigDecimal balance;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deactivatedAt;

    public AccountApiResponse(Long id, Long userId, String bankCode, String accountNo,
                             BigDecimal balance, String status, LocalDateTime createdAt,
                             LocalDateTime deactivatedAt) {
        this.id = id;
        this.userId = userId;
        this.bankCode = bankCode;
        this.accountNo = accountNo;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
        this.deactivatedAt = deactivatedAt;
    }
}