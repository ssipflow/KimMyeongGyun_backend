package com.moneyTransfer.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "bank_code", "account_no_norm"}),
       indexes = {
           @Index(name = "idx_account_user", columnList = "user_id")
       })
public class AccountJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ToOne 관계 - LAZY 로딩, Fetch Join으로 조회
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;
    
    @Column(name = "bank_code", nullable = false)
    private String bankCode;
    
    @Column(name = "account_no", nullable = false)
    private String accountNo;
    
    @Column(name = "account_no_norm", nullable = false)
    private String accountNoNorm;
    
    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Column(name = "status", nullable = false)
    private Integer status = 200; // ACTIVATE: 200
    
    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Version
    private Integer version = 0;
    
    // JPA용 기본 생성자
    protected AccountJpaEntity() {}
    
    // 생성자
    public AccountJpaEntity(UserJpaEntity user, String bankCode, String accountNo, String accountNoNorm) {
        this.user = user;
        this.bankCode = bankCode;
        this.accountNo = accountNo;
        this.accountNoNorm = accountNoNorm;
        this.balance = BigDecimal.ZERO;
        this.status = 200;
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
    
    public UserJpaEntity getUser() { return user; }
    public void setUser(UserJpaEntity user) { this.user = user; }
    
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    
    public String getAccountNoNorm() { return accountNoNorm; }
    public void setAccountNoNorm(String accountNoNorm) { this.accountNoNorm = accountNoNorm; }
    
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    
    public LocalDateTime getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(LocalDateTime deactivatedAt) { this.deactivatedAt = deactivatedAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}