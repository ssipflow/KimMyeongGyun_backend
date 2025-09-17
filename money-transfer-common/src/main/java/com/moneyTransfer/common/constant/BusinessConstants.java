package com.moneyTransfer.common.constant;

import java.math.BigDecimal;

public class BusinessConstants {

    // 계좌 관련 상수
    public static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO;
    public static final int ACCOUNT_NO_MIN_LENGTH = 10;
    public static final int ACCOUNT_NO_MAX_LENGTH = 14;
    public static final int ID_CARD_LENGTH = 13;

    // 미래 확장을 위한 상수들
    public static final BigDecimal DAILY_WITHDRAW_LIMIT = new BigDecimal("1000000"); // 100만원
    public static final BigDecimal DAILY_TRANSFER_LIMIT = new BigDecimal("3000000"); // 300만원
    public static final BigDecimal TRANSFER_FEE_RATE = new BigDecimal("0.01"); // 1%

    private BusinessConstants() {
    }
}