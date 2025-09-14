package com.moneyTransfer.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_limits")
public class DailyLimitJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(name = "limit_date", nullable = false)
    private LocalDate limitDate;
    
    @Column(name = "withdraw_used", nullable = false, precision = 15, scale = 2)
    private BigDecimal withdrawUsed = BigDecimal.ZERO;
    
    @Column(name = "transfer_used", nullable = false, precision = 15, scale = 2)
    private BigDecimal transferUsed = BigDecimal.ZERO;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Version
    private Integer version = 0;
    
    // JPA용 기본 생성자
    protected DailyLimitJpaEntity() {}
    
    // 생성자
    public DailyLimitJpaEntity(Long accountId, LocalDate limitDate) {
        this.accountId = accountId;
        this.limitDate = limitDate;
        this.withdrawUsed = BigDecimal.ZERO;
        this.transferUsed = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // JPA 라이프사이클 콜백
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    
    public LocalDate getLimitDate() { return limitDate; }
    public void setLimitDate(LocalDate limitDate) { this.limitDate = limitDate; }
    
    public BigDecimal getWithdrawUsed() { return withdrawUsed; }
    public void setWithdrawUsed(BigDecimal withdrawUsed) { this.withdrawUsed = withdrawUsed; }
    
    public BigDecimal getTransferUsed() { return transferUsed; }
    public void setTransferUsed(BigDecimal transferUsed) { this.transferUsed = transferUsed; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}