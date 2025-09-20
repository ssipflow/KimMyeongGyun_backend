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
@Schema(description = "이체 API 요청")
public class TransferApiRequest {

    @Schema(description = "송금 계좌 은행 코드", example = "001")
    @NotBlank(message = "송금 계좌 은행 코드는 필수입니다")
    private String fromBankCode;

    @Schema(description = "송금 계좌 번호", example = "123-456-789")
    @NotBlank(message = "송금 계좌 번호는 필수입니다")
    private String fromAccountNo;

    @Schema(description = "수취 계좌 은행 코드", example = "002")
    @NotBlank(message = "수취 계좌 은행 코드는 필수입니다")
    private String toBankCode;

    @Schema(description = "수취 계좌 번호", example = "987-654-321")
    @NotBlank(message = "수취 계좌 번호는 필수입니다")
    private String toAccountNo;

    @Schema(description = "이체 금액", example = "100000")
    @NotNull(message = "이체 금액은 필수입니다")
    @DecimalMin(value = "1", message = "이체 금액은 1원 이상이어야 합니다")
    private BigDecimal amount;

    @Schema(description = "이체 사유", example = "친구에게 이체")
    @NotNull(message = "이체 사유는 필수입니다")
    @Size(min = 1, max = 200, message = "이체 사유는 1자 이상 200자 이하여야 합니다")
    private String description;

    public TransferApiRequest(String fromBankCode, String fromAccountNo, String toBankCode, String toAccountNo,
                             BigDecimal amount, String description) {
        this.fromBankCode = fromBankCode;
        this.fromAccountNo = fromAccountNo;
        this.toBankCode = toBankCode;
        this.toAccountNo = toAccountNo;
        this.amount = amount;
        this.description = description;
    }
}