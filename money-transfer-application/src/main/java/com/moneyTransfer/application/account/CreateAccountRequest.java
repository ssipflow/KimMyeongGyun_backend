package com.moneyTransfer.application.account;

public class CreateAccountRequest {
    // User 정보
    private String userName;
    private String email;
    private String idCardNo;

    // Account 정보
    private String bankCode;
    private String accountNo;

    public CreateAccountRequest() {}

    public CreateAccountRequest(String userName, String email, String idCardNo,
                               String bankCode, String accountNo) {
        this.userName = userName;
        this.email = email;
        this.idCardNo = idCardNo;
        this.bankCode = bankCode;
        this.accountNo = accountNo;
    }

    // User 관련 getters/setters
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getIdCardNo() { return idCardNo; }
    public void setIdCardNo(String idCardNo) { this.idCardNo = idCardNo; }

    // Account 관련 getters/setters
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
}