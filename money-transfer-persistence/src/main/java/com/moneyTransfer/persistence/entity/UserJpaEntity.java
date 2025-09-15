package com.moneyTransfer.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class UserJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "id_card_no", nullable = false)
    private String idCardNo;
    
    @Column(name = "id_card_no_norm", nullable = false, unique = true)
    private String idCardNoNorm;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // ToMany 관계 - LAZY + BatchSize
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @BatchSize(size = 100)
    private List<AccountJpaEntity> accounts = new ArrayList<>();
    
    // JPA용 기본 생성자
    protected UserJpaEntity() {}
    
    // 생성자
    public UserJpaEntity(String name, String idCardNo, String idCardNoNorm) {
        this.name = name;
        this.idCardNo = idCardNo;
        this.idCardNoNorm = idCardNoNorm;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getIdCardNo() { return idCardNo; }
    public void setIdCardNo(String idCardNo) { this.idCardNo = idCardNo; }
    
    public String getIdCardNoNorm() { return idCardNoNorm; }
    public void setIdCardNoNorm(String idCardNoNorm) { this.idCardNoNorm = idCardNoNorm; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public List<AccountJpaEntity> getAccounts() { return accounts; }
    public void setAccounts(List<AccountJpaEntity> accounts) { this.accounts = accounts; }
}