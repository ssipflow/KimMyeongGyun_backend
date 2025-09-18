package com.moneyTransfer.persistence.repository;

import com.moneyTransfer.persistence.entity.TransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, Long> {

    @Query("SELECT t FROM TransactionJpaEntity t " +
           "JOIN FETCH t.account " +
           "WHERE t.account.id = :accountId " +
           "ORDER BY t.createdAt DESC")
    List<TransactionJpaEntity> findByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT t FROM TransactionJpaEntity t " +
           "JOIN FETCH t.account " +
           "WHERE t.account.id = :accountId " +
           "AND t.createdAt >= :startDate AND t.createdAt <= :endDate " +
           "ORDER BY t.createdAt DESC")
    List<TransactionJpaEntity> findByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}