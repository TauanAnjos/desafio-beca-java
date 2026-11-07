package br.com.tauan.ms2_transacoes.application.service;

import br.com.tauan.ms2_transacoes.application.port.in.GetTransactionUseCase;
import br.com.tauan.ms2_transacoes.application.port.in.ListTransactionsQuery;
import br.com.tauan.ms2_transacoes.application.port.in.ListTransactionsUseCase;
import br.com.tauan.ms2_transacoes.application.port.out.TransactionRepositoryPort;
import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;
import br.com.tauan.ms2_transacoes.domain.exception.TransactionNotFoundException;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QueryTransactionsService implements GetTransactionUseCase, ListTransactionsUseCase {

    private final TransactionRepositoryPort repository;

    public QueryTransactionsService(TransactionRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction execute(Long userId, Long transactionId) {
        // Transação de outro usuário responde como "não encontrada".
        return repository.findById(transactionId)
                .filter(transaction -> transaction.getUserId().equals(userId))
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> execute(ListTransactionsQuery query) {
        if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
            throw new InvalidTransactionException("'from' must not be after 'to'");
        }
        LocalDateTime fromInclusive = query.from() == null ? null : query.from().atStartOfDay();
        LocalDateTime toExclusive = query.to() == null ? null : query.to().plusDays(1).atStartOfDay();
        return repository.findByUser(query.userId(), fromInclusive, toExclusive, query.type());
    }
}