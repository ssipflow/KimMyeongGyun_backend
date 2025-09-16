package com.moneyTransfer.domain.user;

import java.util.Optional;

public interface UserPort {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByIdCardNoNorm(String idCardNoNorm);

    boolean existsByEmail(String email);

    boolean existsByIdCardNoNorm(String idCardNoNorm);
}