package com.moneyTransfer.common.constant;

public class ErrorMessages {

    // User 관련 에러 메시지
    public static final String USER_NAME_REQUIRED = "사용자명은 필수입니다";
    public static final String INVALID_EMAIL_FORMAT = "유효한 이메일 형식이 아닙니다";
    public static final String INVALID_ID_CARD_FORMAT = "유효한 주민번호 형식이 아닙니다";
    public static final String DUPLICATE_EMAIL = "이미 존재하는 이메일입니다";
    public static final String DUPLICATE_ID_CARD = "이미 존재하는 주민번호입니다";
    public static final String USER_NOT_FOUND = "사용자를 찾을 수 없습니다";

    // Account 관련 에러 메시지
    public static final String USER_ID_REQUIRED = "사용자 ID는 필수입니다";
    public static final String BANK_CODE_REQUIRED = "은행코드는 필수입니다";
    public static final String ACCOUNT_NO_REQUIRED = "계좌번호는 필수입니다";
    public static final String INVALID_ACCOUNT_NO_FORMAT = "유효한 계좌번호 형식이 아닙니다";
    public static final String DUPLICATE_ACCOUNT_NO = "이미 존재하는 계좌번호입니다";
    public static final String ACCOUNT_NOT_FOUND = "계좌를 찾을 수 없습니다";
    public static final String ACCOUNT_ALREADY_DEACTIVATED = "이미 비활성화된 계좌입니다";
    public static final String ACCOUNT_HAS_BALANCE = "잔액이 있는 계좌는 삭제할 수 없습니다";

    // Transaction 관련 에러 메시지
    public static final String INVALID_AMOUNT = "금액은 0보다 커야 합니다";
    public static final String DEPOSIT_AMOUNT_INVALID = "입금 금액은 0보다 커야 합니다";
    public static final String WITHDRAW_AMOUNT_INVALID = "출금 금액은 0보다 커야 합니다";
    public static final String INSUFFICIENT_BALANCE = "잔액이 부족합니다";
    public static final String INACTIVE_ACCOUNT_DEPOSIT = "비활성 계좌에는 입금할 수 없습니다";
    public static final String INACTIVE_ACCOUNT_WITHDRAW = "비활성 계좌에서는 출금할 수 없습니다";

    // User 일관성 검증 에러 메시지
    public static final String USER_NAME_MISMATCH = "제공된 사용자명이 기존 정보와 일치하지 않습니다";
    public static final String USER_EMAIL_MISMATCH = "제공된 이메일이 기존 정보와 일치하지 않습니다";

    // API 관련 에러 메시지
    public static final String MISSING_QUERY_PARAMETERS = "userId 또는 (bankCode와 accountNo) 파라미터가 필요합니다";

    // 동시성 제어 관련 에러 메시지
    public static final String OPTIMISTIC_LOCK_CONFLICT = "다른 사용자가 계좌를 수정 중입니다. 잠시 후 다시 시도해주세요.";

    // 이체 관련 에러 메시지
    public static final String CANNOT_TRANSFER_TO_SAME_ACCOUNT = "같은 계좌로는 이체할 수 없습니다";
    public static final String TARGET_ACCOUNT_NOT_FOUND = "이체 대상 계좌를 찾을 수 없습니다";

    // 일일 한도 관련 에러 메시지
    public static final String DAILY_WITHDRAW_LIMIT_EXCEEDED = "일일 출금 한도를 초과했습니다";
    public static final String DAILY_TRANSFER_LIMIT_EXCEEDED = "일일 이체 한도를 초과했습니다";

    private ErrorMessages() {
    }
}