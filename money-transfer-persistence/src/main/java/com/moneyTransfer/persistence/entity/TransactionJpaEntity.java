package com.moneyTransfer.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions",
       indexes = {
           @Index(name = "idx_transaction_account_date", columnList = "account_id, created_at DESC")
       })
public class TransactionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", nullable = false)
    private Integer type; // DEPOSIT: 100, WITHDRAW: 200, TRANSFER: 300

    // ToOne 관계 - LAZY 로딩, Fetch Join으로 조회
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountJpaEntity account;

    @Column(name = "account_to_no")
    private String accountToNo;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "description")
    private String description;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Version
    private Integer version = 0;

    // JPA용 기본 생성자
    protected TransactionJpaEntity() {}

    // 생성자
    public TransactionJpaEntity(Integer type, AccountJpaEntity account, String accountToNo,
                               BigDecimal amount, BigDecimal fee, String description) {
        this.type = type;
        this.account = account;
        this.accountToNo = accountToNo;
        this.amount = amount;
        this.fee = fee != null ? fee : BigDecimal.ZERO;
        this.description = description;
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

    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }

    public AccountJpaEntity getAccount() { return account; }
    public void setAccount(AccountJpaEntity account) { this.account = account; }

    public String getAccountToNo() { return accountToNo; }
    public void setAccountToNo(String accountToNo) { this.accountToNo = accountToNo; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}