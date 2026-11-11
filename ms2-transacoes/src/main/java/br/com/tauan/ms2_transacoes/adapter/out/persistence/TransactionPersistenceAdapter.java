package br.com.tauan.ms2_transacoes.adapter.out.persistence;

import br.com.tauan.ms2_transacoes.application.port.out.TransactionRepositoryPort;
import br.com.tauan.ms2_transacoes.domain.model.ExchangeRate;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import br.com.tauan.ms2_transacoes.domain.model.TransactionStatus;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Component
public class TransactionPersistenceAdapter implements TransactionRepositoryPort {

    private static final LocalDateTime EARLIEST = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime LATEST = LocalDateTime.of(9999, 12, 31, 23, 59);

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

    @Override
    public BigDecimal sumApprovedExpenses(Long userId, LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        return sumApproved(userId, TransactionType::isExpense, fromInclusive, toExclusive);
    }

    @Override
    public BigDecimal sumApprovedIncome(Long userId, LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        return sumApproved(userId, TransactionType::isIncome, fromInclusive, toExclusive);
    }

    @Override
    public List<Transaction> findByUser(Long userId, LocalDateTime fromInclusive, LocalDateTime toExclusive,
                                        TransactionType type) {
        Collection<TransactionType> types = type == null ? List.of(TransactionType.values()) : List.of(type);
        return jpaRepository.search(userId, types,
                        fromInclusive == null ? EARLIEST : fromInclusive,
                        toExclusive == null ? LATEST : toExclusive)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private BigDecimal sumApproved(Long userId, Predicate<TransactionType> typeFilter,
                                   LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        List<TransactionType> types = Arrays.stream(TransactionType.values()).filter(typeFilter).toList();
        BigDecimal total = jpaRepository.sumAmount(userId, TransactionStatus.APPROVED, types,
                fromInclusive, toExclusive);
        return total == null ? BigDecimal.ZERO : total;
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
        List<ExchangeRateEmbeddable> rates = new ArrayList<>();
        for (ExchangeRate rate : transaction.getExchangeRates()) {
            rates.add(new ExchangeRateEmbeddable(rate.currency(), rate.rate(), rate.rateDate()));
        }
        entity.setExchangeRates(rates);
        return entity;
    }

    private Transaction toDomain(TransactionJpaEntity entity) {
        List<ExchangeRate> rates = entity.getExchangeRates().stream()
                .map(rate -> new ExchangeRate(rate.getCurrency(), rate.getRate(), rate.getRateDate()))
                .toList();
        return Transaction.restore(entity.getId(), entity.getUserId(), entity.getType(), entity.getAmount(),
                entity.getDescription(), entity.getCategory(), entity.getDestinationUserId(),
                entity.getStatus(), entity.getRejectionReason(), entity.getOccurredAt(), rates);
    }
}