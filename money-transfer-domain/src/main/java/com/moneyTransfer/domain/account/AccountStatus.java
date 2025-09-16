package com.moneyTransfer.domain.account;

public enum AccountStatus {
    ACTIVATE(200, "활성"),
    DEACTIVATE(400, "비활성");

    private final int code;
    private final String description;

    AccountStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}