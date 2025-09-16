package com.moneyTransfer.persistence.repository;

import com.moneyTransfer.persistence.entity.AccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, Long> {
    
    @Query("SELECT a FROM AccountJpaEntity a JOIN FETCH a.user WHERE a.id = :id")
    Optional<AccountJpaEntity> findByIdWithUser(@Param("id") Long id);
    
    @Query("SELECT a FROM AccountJpaEntity a JOIN FETCH a.user WHERE a.user.id = :userId")
    List<AccountJpaEntity> findByUserIdWithUser(@Param("userId") Long userId);
    
    Optional<AccountJpaEntity> findByBankCodeAndAccountNoNorm(String bankCode, String accountNoNorm);

    boolean existsByBankCodeAndAccountNoNorm(String bankCode, String accountNoNorm);
    
    @Query("SELECT a FROM AccountJpaEntity a WHERE a.status = :status")
    List<AccountJpaEntity> findByStatus(@Param("status") Integer status);
}