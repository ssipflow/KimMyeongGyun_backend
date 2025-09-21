package com.moneyTransfer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "입금 API 요청")
public class DepositApiRequest {

    @Schema(description = "은행 코드", example = "001")
    @NotBlank(message = "은행 코드는 필수입니다")
    private String bankCode;

    @Schema(description = "계좌 번호", example = "123-456-7891")
    @NotBlank(message = "계좌 번호는 필수입니다")
    private String accountNo;

    @Schema(description = "입금 금액", example = "50000")
    @NotNull(message = "입금 금액은 필수입니다")
    @DecimalMin(value = "1", message = "입금 금액은 1원 이상이어야 합니다")
    private BigDecimal amount;

    @Schema(description = "입금 사유", example = "월급 입금")
    @NotNull(message = "입금 사유는 필수입니다")
    @Size(min = 1, max = 200, message = "입금 사유는 1자 이상 200자 이하여야 합니다")
    private String description;

    public DepositApiRequest(String bankCode, String accountNo, BigDecimal amount, String description) {
        this.bankCode = bankCode;
        this.accountNo = accountNo;
        this.amount = amount;
        this.description = description;
    }
}