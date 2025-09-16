package com.moneyTransfer.persistence.repository;

import com.moneyTransfer.domain.account.Account;
import com.moneyTransfer.domain.account.AccountRepository;
import com.moneyTransfer.domain.account.AccountStatus;
import com.moneyTransfer.persistence.entity.AccountJpaEntity;
import com.moneyTransfer.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Transactional
public class AccountRepositoryImpl implements AccountRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final AccountJpaRepository accountJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public AccountRepositoryImpl(AccountJpaRepository accountJpaRepository,
                                UserJpaRepository userJpaRepository) {
        this.accountJpaRepository = accountJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Account save(Account account) {
        AccountJpaEntity entity;

        if (account.getId() == null) {
            // 새로운 계좌 생성
            UserJpaEntity user = userJpaRepository.findById(account.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

            entity = new AccountJpaEntity(
                user,
                account.getBankCode(),
                account.getAccountNo(),
                account.getAccountNoNorm()
            );
        } else {
            // 기존 계좌 업데이트
            entity = accountJpaRepository.findById(account.getId())
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다"));
        }

        // 도메인 객체의 상태를 JPA 엔티티에 반영
        entity.setBalance(account.getBalance());
        entity.setStatus(account.getStatus().getCode());
        entity.setDeactivatedAt(account.getDeactivatedAt());
        entity.setVersion(account.getVersion());

        AccountJpaEntity savedEntity = accountJpaRepository.save(entity);
        return mapToDomain(savedEntity);
    }

    @Override
    public Optional<Account> findById(Long id) {
        return accountJpaRepository.findByIdWithUser(id)
            .map(this::mapToDomain);
    }

    @Override
    public Optional<Account> findByIdWithLock(Long id) {
        AccountJpaEntity entity = entityManager.find(AccountJpaEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        return Optional.ofNullable(entity).map(this::mapToDomain);
    }

    @Override
    public List<Account> findByUserId(Long userId) {
        return accountJpaRepository.findByUserIdWithUser(userId)
            .stream()
            .map(this::mapToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Account> findByBankCodeAndAccountNoNorm(String bankCode, String accountNoNorm) {
        return accountJpaRepository.findByBankCodeAndAccountNoNorm(bankCode, accountNoNorm)
            .map(this::mapToDomain);
    }

    @Override
    public void delete(Account account) {
        if (account.getId() != null) {
            accountJpaRepository.deleteById(account.getId());
        }
    }

    @Override
    public boolean existsByBankCodeAndAccountNoNorm(String bankCode, String accountNoNorm) {
        return accountJpaRepository.existsByBankCodeAndAccountNoNorm(bankCode, accountNoNorm);
    }

    private Account mapToDomain(AccountJpaEntity entity) {
        Account account = new Account();
        account.setId(entity.getId());
        account.setUserId(entity.getUser().getId());
        account.setBankCode(entity.getBankCode());
        account.setAccountNo(entity.getAccountNo());
        account.setAccountNoNorm(entity.getAccountNoNorm());
        account.setBalance(entity.getBalance());
        account.setStatus(mapStatusToDomain(entity.getStatus()));
        account.setDeactivatedAt(entity.getDeactivatedAt());
        account.setVersion(entity.getVersion());
        account.setCreatedAt(entity.getCreatedAt());
        return account;
    }

    private AccountStatus mapStatusToDomain(Integer statusCode) {
        return statusCode == 200 ? AccountStatus.ACTIVATE : AccountStatus.DEACTIVATE;
    }
}