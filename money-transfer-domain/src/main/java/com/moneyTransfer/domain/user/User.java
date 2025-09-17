package com.moneyTransfer.domain.user;

import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.common.util.StringNormalizer;
import com.moneyTransfer.common.util.ValidationUtils;
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
        user.idCardNoNorm = StringNormalizer.normalizeIdCardNo(idCardNo);
        user.createdAt = LocalDateTime.now();
        return user;
    }

    private static void validateUserData(String name, String email, String idCardNo) {
        if (!ValidationUtils.isNotBlank(name)) {
            throw new IllegalArgumentException(ErrorMessages.USER_NAME_REQUIRED);
        }
        if (!ValidationUtils.isValidEmail(email)) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_EMAIL_FORMAT);
        }
        if (!ValidationUtils.isValidIdCardNo(idCardNo)) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_ID_CARD_FORMAT);
        }
    }

}