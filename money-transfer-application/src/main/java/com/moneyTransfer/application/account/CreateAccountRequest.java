package com.moneyTransfer.application.account;

public class CreateAccountRequest {
    private Long userId;
    private String bankCode;
    private String accountNo;

    public CreateAccountRequest() {}

    public CreateAccountRequest(Long userId, String bankCode, String accountNo) {
        this.userId = userId;
        this.bankCode = bankCode;
        this.accountNo = accountNo;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

    public void validateRequest() {
        if (this.userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
        if (this.bankCode == null || this.bankCode.trim().isEmpty()) {
            throw new IllegalArgumentException("은행코드는 필수입니다");
        }
        if (this.accountNo == null || this.accountNo.trim().isEmpty()) {
            throw new IllegalArgumentException("계좌번호는 필수입니다");
        }
    }

}