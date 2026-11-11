package br.com.tauan.ms2_transacoes.application.service;

import br.com.tauan.ms2_transacoes.application.port.in.ExpenseSummaryQuery;
import br.com.tauan.ms2_transacoes.application.port.in.ExpenseSummaryResult;
import br.com.tauan.ms2_transacoes.application.port.in.GetExpenseSummaryUseCase;
import br.com.tauan.ms2_transacoes.application.port.out.TransactionRepositoryPort;
import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;
import br.com.tauan.ms2_transacoes.domain.model.ExpenseSummary;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class GetExpenseSummaryService implements GetExpenseSummaryUseCase {

    private final TransactionRepositoryPort repository;
    private final Clock clock;

    public GetExpenseSummaryService(TransactionRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseSummaryResult execute(ExpenseSummaryQuery query) {
        YearMonth currentMonth = YearMonth.now(clock);
        LocalDate from = query.from() == null ? currentMonth.atDay(1) : query.from();
        LocalDate to = query.to() == null ? currentMonth.atEndOfMonth() : query.to();
        if (from.isAfter(to)) {
            throw new InvalidTransactionException("'from' must not be after 'to'");
        }

        List<Transaction> transactions = repository.findByUser(query.userId(),
                from.atStartOfDay(), to.plusDays(1).atStartOfDay(), null);
        return new ExpenseSummaryResult(from, to, ExpenseSummary.of(transactions));
    }
}