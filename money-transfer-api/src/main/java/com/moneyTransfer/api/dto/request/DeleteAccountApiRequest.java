package com.moneyTransfer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "계좌 삭제 API 요청")
public class DeleteAccountApiRequest {

    @Schema(description = "은행 코드", example = "001")
    @NotBlank(message = "은행 코드는 필수입니다")
    private String bankCode;

    @Schema(description = "계좌 번호", example = "123-456-789")
    @NotBlank(message = "계좌 번호는 필수입니다")
    private String accountNo;

    public DeleteAccountApiRequest(String bankCode, String accountNo) {
        this.bankCode = bankCode;
        this.accountNo = accountNo;
    }
}