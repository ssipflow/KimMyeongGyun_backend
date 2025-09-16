package com.moneyTransfer.persistence.adapter;

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
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
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
        // User.create() 메서드를 사용하여 객체 생성
        User user = User.create(entity.getName(), entity.getEmail(), entity.getIdCardNo());

        // ID와 생성일시는 직접 설정 (JPA에서 로드된 값)
        user.setId(entity.getId());
        user.setCreatedAt(entity.getCreatedAt());

        return user;
    }
}