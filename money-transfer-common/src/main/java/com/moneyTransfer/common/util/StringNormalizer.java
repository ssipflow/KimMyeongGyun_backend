package com.moneyTransfer.common.util;

public class StringNormalizer {

    public static String normalizeIdCardNo(String idCardNo) {
        if (idCardNo == null) return null;
        return idCardNo.replaceAll("[^0-9]", "");
    }

    public static String normalizeAccountNo(String accountNo) {
        if (accountNo == null) return null;
        return accountNo.replaceAll("[^0-9]", "");
    }

    public static String normalizePhoneNo(String phoneNo) {
        if (phoneNo == null) return null;
        return phoneNo.replaceAll("[^0-9]", "");
    }

    private StringNormalizer() {
    }
}