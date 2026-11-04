package br.com.tauan.ms2_transacoes.adapter.out.persistence;

import br.com.tauan.ms2_transacoes.application.port.out.TransactionRepositoryPort;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TransactionPersistenceAdapter implements TransactionRepositoryPort {

    private final TransactionJpaRepository jpaRepository;

    public TransactionPersistenceAdapter(TransactionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        return toDomain(jpaRepository.save(toEntity(transaction)));
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    private TransactionJpaEntity toEntity(Transaction transaction) {
        TransactionJpaEntity entity = new TransactionJpaEntity();
        entity.setId(transaction.getId());
        entity.setUserId(transaction.getUserId());
        entity.setType(transaction.getType());
        entity.setAmount(transaction.getAmount());
        entity.setDescription(transaction.getDescription());
        entity.setCategory(transaction.getCategory());
        entity.setDestinationUserId(transaction.getDestinationUserId());
        entity.setStatus(transaction.getStatus());
        entity.setRejectionReason(transaction.getRejectionReason());
        entity.setOccurredAt(transaction.getOccurredAt());
        return entity;
    }

    private Transaction toDomain(TransactionJpaEntity entity) {
        return Transaction.restore(entity.getId(), entity.getUserId(), entity.getType(), entity.getAmount(),
                entity.getDescription(), entity.getCategory(), entity.getDestinationUserId(),
                entity.getStatus(), entity.getRejectionReason(), entity.getOccurredAt());
    }
}