package br.com.tauan.ms2_transacoes.application.service;

import br.com.tauan.ms2_transacoes.application.port.in.ListTransactionsQuery;
import br.com.tauan.ms2_transacoes.application.port.in.ReportResult;
import br.com.tauan.ms2_transacoes.application.port.in.ReportTransactionsUseCase;
import br.com.tauan.ms2_transacoes.application.port.out.TransactionRepositoryPort;
import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ReportTransactionsService implements ReportTransactionsUseCase {

    private final TransactionRepositoryPort repository;
    private final Clock clock;

    public ReportTransactionsService(TransactionRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResult execute(ListTransactionsQuery query) {
        YearMonth currentMonth = YearMonth.now(clock);
        LocalDate from = query.from() == null ? currentMonth.atDay(1) : query.from();
        LocalDate to = query.to() == null ? currentMonth.atEndOfMonth() : query.to();
        if (from.isAfter(to)) {
            throw new InvalidTransactionException("'from' must not be after 'to'");
        }

        // O repositório devolve da mais recente para a mais antiga; o relatório é cronológico.
        List<Transaction> chronological = new ArrayList<>(repository.findByUser(query.userId(),
                from.atStartOfDay(), to.plusDays(1).atStartOfDay(), query.type()));
        Collections.reverse(chronological);
        return new ReportResult(from, to, List.copyOf(chronological));
    }
}