package com.moneyTransfer.persistence.adapter;

import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.domain.user.User;
import com.moneyTransfer.domain.user.UserPort;
import com.moneyTransfer.persistence.entity.UserJpaEntity;
import com.moneyTransfer.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaUserPort implements UserPort {

    private final UserJpaRepository userJpaRepository;

    public JpaUserPort(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity;

        if (user.getId() == null) {
            // 새로운 사용자 생성
            entity = new UserJpaEntity(
                user.getName(),
                user.getEmail(),
                user.getIdCardNo(),
                user.getIdCardNoNorm()
            );
        } else {
            // 기존 사용자 업데이트
            entity = userJpaRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.USER_NOT_FOUND));
        }

        // 도메인 객체의 상태를 JPA 엔티티에 반영
        entity.setName(user.getName());
        entity.setEmail(user.getEmail());
        entity.setIdCardNo(user.getIdCardNo());
        entity.setIdCardNoNorm(user.getIdCardNoNorm());

        UserJpaEntity savedEntity = userJpaRepository.save(entity);
        return mapToDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id)
            .map(this::mapToDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
            .map(this::mapToDomain);
    }

    @Override
    public Optional<User> findByIdCardNoNorm(String idCardNoNorm) {
        return userJpaRepository.findByIdCardNoNorm(idCardNoNorm)
            .map(this::mapToDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByIdCardNoNorm(String idCardNoNorm) {
        return userJpaRepository.existsByIdCardNoNorm(idCardNoNorm);
    }

    private User mapToDomain(UserJpaEntity entity) {
        User user = new User();
        user.setId(entity.getId());
        user.setName(entity.getName());
        user.setEmail(entity.getEmail());
        user.setIdCardNo(entity.getIdCardNo());
        user.setIdCardNoNorm(entity.getIdCardNoNorm());
        user.setCreatedAt(entity.getCreatedAt());
        return user;
    }
}