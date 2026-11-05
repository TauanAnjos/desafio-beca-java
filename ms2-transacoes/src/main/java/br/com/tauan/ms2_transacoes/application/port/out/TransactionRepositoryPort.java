package br.com.tauan.ms2_transacoes.application.port.out;

import br.com.tauan.ms2_transacoes.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionRepositoryPort {

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(Long id);

    BigDecimal sumApprovedExpenses(Long userId, LocalDateTime fromInclusive, LocalDateTime toExclusive);
}