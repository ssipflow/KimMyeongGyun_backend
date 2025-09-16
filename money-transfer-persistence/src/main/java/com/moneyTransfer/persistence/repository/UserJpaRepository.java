package com.moneyTransfer.persistence.repository;

import com.moneyTransfer.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByIdCardNoNorm(String idCardNoNorm);

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByIdCardNoNorm(String idCardNoNorm);

    boolean existsByEmail(String email);
}