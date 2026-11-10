package br.com.tauan.ms2_transacoes.application.port.out;

import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepositoryPort {

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(Long id);

    /**
     * Soma das despesas aprovadas (retirada, transferência e compra) do usuário no período.
     * Devolve zero se não houver nenhuma.
     */
    BigDecimal sumApprovedExpenses(Long userId, LocalDateTime fromInclusive, LocalDateTime toExclusive);

    /**
     * Soma das entradas aprovadas (depósitos) do usuário no período.
     * Devolve zero se não houver nenhuma.
     */
    BigDecimal sumApprovedIncome(Long userId, LocalDateTime fromInclusive, LocalDateTime toExclusive);

    /**
     * Transações do usuário, da mais recente para a mais antiga. Filtros nulos são ignorados.
     */
    List<Transaction> findByUser(Long userId, LocalDateTime fromInclusive, LocalDateTime toExclusive,
                                 TransactionType type);
}