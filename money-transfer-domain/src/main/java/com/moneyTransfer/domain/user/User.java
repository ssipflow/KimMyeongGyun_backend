package com.moneyTransfer.domain.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
public class User {
    private Long id;
    private String name;
    private String email;
    private String idCardNo;
    private String idCardNoNorm;
    private LocalDateTime createdAt;

    public static User create(String name, String email, String idCardNo) {
        validateUserData(name, email, idCardNo);

        User user = new User();
        user.name = name;
        user.email = email;
        user.idCardNo = idCardNo;
        user.idCardNoNorm = normalizeIdCardNo(idCardNo);
        user.createdAt = LocalDateTime.now();
        return user;
    }

    private static void validateUserData(String name, String email, String idCardNo) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자명은 필수입니다");
        }
        if (email == null || !isValidEmail(email)) {
            throw new IllegalArgumentException("유효한 이메일 형식이 아닙니다");
        }
        if (idCardNo == null || !isValidIdCardNo(idCardNo)) {
            throw new IllegalArgumentException("유효한 주민번호 형식이 아닙니다");
        }
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private static boolean isValidIdCardNo(String idCardNo) {
        String normalized = normalizeIdCardNo(idCardNo);
        return normalized.matches("^\\d{13}$");
    }

    private static String normalizeIdCardNo(String idCardNo) {
        if (idCardNo == null) return null;
        return idCardNo.replaceAll("[^0-9]", "");
    }

}