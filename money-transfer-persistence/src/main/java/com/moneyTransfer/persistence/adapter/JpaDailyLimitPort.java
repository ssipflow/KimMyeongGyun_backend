package com.moneyTransfer.persistence.adapter;

import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.domain.dailylimit.DailyLimit;
import com.moneyTransfer.domain.dailylimit.DailyLimitPort;
import com.moneyTransfer.persistence.entity.AccountJpaEntity;
import com.moneyTransfer.persistence.entity.DailyLimitJpaEntity;
import com.moneyTransfer.persistence.repository.AccountJpaRepository;
import com.moneyTransfer.persistence.repository.DailyLimitJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.Optional;

@Repository
@Transactional
public class JpaDailyLimitPort implements DailyLimitPort {

    @PersistenceContext
    private EntityManager entityManager;

    private final DailyLimitJpaRepository dailyLimitJpaRepository;
    private final AccountJpaRepository accountJpaRepository;

    public JpaDailyLimitPort(DailyLimitJpaRepository dailyLimitJpaRepository,
                            AccountJpaRepository accountJpaRepository) {
        this.dailyLimitJpaRepository = dailyLimitJpaRepository;
        this.accountJpaRepository = accountJpaRepository;
    }

    @Override
    public DailyLimit save(DailyLimit dailyLimit) {
        DailyLimitJpaEntity entity;

        if (dailyLimit.getId() == null) {
            // 새로운 일일 한도 생성
            AccountJpaEntity account = accountJpaRepository.findById(dailyLimit.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));

            entity = new DailyLimitJpaEntity(account, dailyLimit.getLimitDate());
        } else {
            // 기존 일일 한도 업데이트
            entity = dailyLimitJpaRepository.findById(dailyLimit.getId())
                .orElseThrow(() -> new IllegalArgumentException("일일 한도를 찾을 수 없습니다"));
        }

        // 도메인 객체의 상태를 JPA 엔티티에 반영
        entity.setWithdrawUsed(dailyLimit.getWithdrawUsed());
        entity.setTransferUsed(dailyLimit.getTransferUsed());

        DailyLimitJpaEntity savedEntity = dailyLimitJpaRepository.save(entity);

        // 명시적 flush로 즉시 DB 반영
        entityManager.flush();

        return mapToDomain(savedEntity);
    }

    @Override
    public Optional<DailyLimit> findByAccountIdAndLimitDate(Long accountId, LocalDate limitDate) {
        return dailyLimitJpaRepository.findByAccountIdAndLimitDateWithAccount(accountId, limitDate)
            .map(this::mapToDomain);
    }

    @Override
    public Optional<DailyLimit> findByAccountIdAndLimitDateWithLock(Long accountId, LocalDate limitDate) {
        return dailyLimitJpaRepository.findByAccountIdAndLimitDateWithAccountAndLock(accountId, limitDate)
            .map(this::mapToDomain);
    }

    private DailyLimit mapToDomain(DailyLimitJpaEntity entity) {
        DailyLimit dailyLimit = new DailyLimit();
        dailyLimit.setId(entity.getId());
        dailyLimit.setAccountId(entity.getAccount().getId());
        dailyLimit.setLimitDate(entity.getLimitDate());
        dailyLimit.setWithdrawUsed(entity.getWithdrawUsed());
        dailyLimit.setTransferUsed(entity.getTransferUsed());
        dailyLimit.setUpdatedAt(entity.getUpdatedAt());
        dailyLimit.setCreatedAt(entity.getCreatedAt());
        return dailyLimit;
    }

}