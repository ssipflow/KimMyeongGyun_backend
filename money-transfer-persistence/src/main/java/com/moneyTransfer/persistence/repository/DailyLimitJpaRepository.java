package com.moneyTransfer.persistence.repository;

import com.moneyTransfer.persistence.entity.DailyLimitJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyLimitJpaRepository extends JpaRepository<DailyLimitJpaEntity, Long> {

    @Query("SELECT d FROM DailyLimitJpaEntity d JOIN FETCH d.account WHERE d.account.id = :accountId AND d.limitDate = :limitDate")
    Optional<DailyLimitJpaEntity> findByAccountIdAndLimitDateWithAccount(
            @Param("accountId") Long accountId,
            @Param("limitDate") LocalDate limitDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DailyLimitJpaEntity d JOIN FETCH d.account WHERE d.account.id = :accountId AND d.limitDate = :limitDate")
    Optional<DailyLimitJpaEntity> findByAccountIdAndLimitDateWithAccountAndLock(
            @Param("accountId") Long accountId,
            @Param("limitDate") LocalDate limitDate
    );
}