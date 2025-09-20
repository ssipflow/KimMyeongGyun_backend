package com.moneyTransfer.persistence.entity;

import com.moneyTransfer.common.constant.BusinessConstants;
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
    private Integer type; // DEPOSIT: 100, WITHDRAW: 200, TRANSFER_SEND: 300, TRANSFER_RECEIVE: 400

    // ToOne 관계 - LAZY 로딩, Fetch Join으로 조회
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountJpaEntity account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_account_id")
    private AccountJpaEntity relatedAccount;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal fee = BusinessConstants.ZERO_AMOUNT;

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
    public TransactionJpaEntity(Integer type, AccountJpaEntity account, AccountJpaEntity relatedAccount,
                               BigDecimal amount, BigDecimal balanceAfter, BigDecimal fee, String description) {
        this.type = type;
        this.account = account;
        this.relatedAccount = relatedAccount;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.fee = fee != null ? fee : BusinessConstants.ZERO_AMOUNT;
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

    public AccountJpaEntity getRelatedAccount() { return relatedAccount; }
    public void setRelatedAccount(AccountJpaEntity relatedAccount) { this.relatedAccount = relatedAccount; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

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