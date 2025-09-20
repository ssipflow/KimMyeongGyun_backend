package com.moneyTransfer.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "계좌 API 응답")
public class AccountApiResponse {

    @Schema(description = "계좌 ID", example = "1")
    private Long id;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "은행 코드", example = "001")
    private String bankCode;

    @Schema(description = "계좌 번호", example = "123-456-789")
    private String accountNo;

    @Schema(description = "계좌 잔액", example = "1000000")
    private BigDecimal balance;

    @Schema(description = "계좌 상태", example = "ACTIVATE")
    private String status;

    @Schema(description = "계좌 생성 일시", example = "2024-01-01 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "계좌 비활성화 일시", example = "2024-12-31 15:30:00")
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