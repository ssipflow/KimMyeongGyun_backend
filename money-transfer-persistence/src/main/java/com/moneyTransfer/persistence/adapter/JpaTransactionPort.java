package com.moneyTransfer.persistence.adapter;

import com.moneyTransfer.common.constant.ErrorMessages;
import com.moneyTransfer.domain.transaction.Transaction;
import com.moneyTransfer.domain.transaction.TransactionPort;
import com.moneyTransfer.domain.transaction.TransactionType;
import com.moneyTransfer.persistence.entity.AccountJpaEntity;
import com.moneyTransfer.persistence.entity.TransactionJpaEntity;
import com.moneyTransfer.persistence.repository.AccountJpaRepository;
import com.moneyTransfer.persistence.repository.TransactionJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@Transactional
public class JpaTransactionPort implements TransactionPort {

    @PersistenceContext
    private EntityManager entityManager;

    private final TransactionJpaRepository transactionJpaRepository;
    private final AccountJpaRepository accountJpaRepository;

    public JpaTransactionPort(TransactionJpaRepository transactionJpaRepository,
                             AccountJpaRepository accountJpaRepository) {
        this.transactionJpaRepository = transactionJpaRepository;
        this.accountJpaRepository = accountJpaRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        AccountJpaEntity accountEntity = accountJpaRepository.findById(transaction.getAccountId())
            .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));

        AccountJpaEntity accountToEntity = null;
        if (transaction.getAccountToId() != null) {
            accountToEntity = accountJpaRepository.findById(transaction.getAccountToId())
                .orElseThrow(() -> new IllegalArgumentException(ErrorMessages.ACCOUNT_NOT_FOUND));
        }

        TransactionJpaEntity entity = new TransactionJpaEntity(
            mapTypeToInteger(transaction.getTransactionType()),
            accountEntity,
            accountToEntity,
            transaction.getAmount(),
            transaction.getBalanceAfter(),
            transaction.getFee(),
            transaction.getDescription()
        );

        TransactionJpaEntity savedEntity = transactionJpaRepository.save(entity);

        // 명시적 flush로 즉시 DB 반영
        entityManager.flush();

        return mapToDomain(savedEntity);
    }

    @Override
    public List<Transaction> findByAccountId(Long accountId) {
        return transactionJpaRepository.findByAccountId(accountId)
            .stream()
            .map(this::mapToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId) {
        return transactionJpaRepository.findByAccountId(accountId)
            .stream()
            .map(this::mapToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByAccountIdAndDateRange(Long accountId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        return transactionJpaRepository.findByAccountIdAndDateRange(accountId, startDateTime, endDateTime)
            .stream()
            .map(this::mapToDomain)
            .collect(Collectors.toList());
    }

    private Transaction mapToDomain(TransactionJpaEntity entity) {
        Transaction transaction = new Transaction();
        transaction.setId(entity.getId());
        transaction.setAccountId(entity.getAccount().getId());
        transaction.setAccountToId(entity.getAccountTo() != null ? entity.getAccountTo().getId() : null);
        transaction.setTransactionType(mapTypeFromInteger(entity.getType()));
        transaction.setAmount(entity.getAmount());
        transaction.setBalanceAfter(entity.getBalanceAfter());
        transaction.setFee(entity.getFee());
        transaction.setDescription(entity.getDescription());
        transaction.setCreatedAt(entity.getCreatedAt());
        transaction.setUpdatedAt(entity.getUpdatedAt());
        return transaction;
    }

    private Integer mapTypeToInteger(TransactionType type) {
        return switch (type) {
            case DEPOSIT -> 100;
            case WITHDRAW -> 200;
            case TRANSFER_SEND -> 300;
            case TRANSFER_RECEIVE -> 400;
        };
    }

    private TransactionType mapTypeFromInteger(Integer type) {
        return switch (type) {
            case 100 -> TransactionType.DEPOSIT;
            case 200 -> TransactionType.WITHDRAW;
            case 300 -> TransactionType.TRANSFER_SEND;
            case 400 -> TransactionType.TRANSFER_RECEIVE;
            default -> throw new IllegalArgumentException("Unknown transaction type: " + type);
        };
    }
}