package com.moneyTransfer.common.util;

public class ValidationUtils {

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final String ID_CARD_PATTERN = "^\\d{13}$";
    private static final String ACCOUNT_NO_PATTERN = "^\\d{10,14}$";

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches(EMAIL_PATTERN);
    }

    public static boolean isValidIdCardNo(String idCardNo) {
        if (idCardNo == null) return false;
        String normalized = StringNormalizer.normalizeIdCardNo(idCardNo);
        return normalized.matches(ID_CARD_PATTERN);
    }

    public static boolean isValidAccountNo(String accountNo) {
        if (accountNo == null) return false;
        String normalized = StringNormalizer.normalizeAccountNo(accountNo);
        return normalized.matches(ACCOUNT_NO_PATTERN);
    }

    public static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    private ValidationUtils() {
    }
}