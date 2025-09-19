package com.moneyTransfer.domain.transaction;

public enum TransactionType {
    DEPOSIT("입금"),
    WITHDRAW("출금"),
    TRANSFER_SEND("이체 출금"),
    TRANSFER_RECEIVE("이체 입금");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}