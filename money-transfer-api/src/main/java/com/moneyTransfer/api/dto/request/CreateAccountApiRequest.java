package com.moneyTransfer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "계좌 등록 API 요청")
public class CreateAccountApiRequest {

    @Schema(description = "사용자명", example = "홍길동")
    @NotBlank(message = "사용자명은 필수입니다")
    private String userName;

    @Schema(description = "이메일", example = "hong@example.com")
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    private String email;

    @Schema(description = "주민번호", example = "123456-1234567")
    @NotBlank(message = "주민번호는 필수입니다")
    @Pattern(regexp = "^\\d{13}$|^\\d{6}-\\d{7}$", message = "유효한 주민번호 형식이 아닙니다")
    private String idCardNo;

    @Schema(description = "은행 코드", example = "001")
    @NotBlank(message = "은행코드는 필수입니다")
    private String bankCode;

    @Schema(description = "계좌 번호", example = "123-456-789")
    @NotBlank(message = "계좌번호는 필수입니다")
    @Pattern(regexp = "^\\d{10,14}$|^\\d{3,4}-\\d{3,4}-\\d{4,6}$", message = "유효한 계좌번호 형식이 아닙니다")
    private String accountNo;

    public CreateAccountApiRequest(String userName, String email, String idCardNo,
                                  String bankCode, String accountNo) {
        this.userName = userName;
        this.email = email;
        this.idCardNo = idCardNo;
        this.bankCode = bankCode;
        this.accountNo = accountNo;
    }
}