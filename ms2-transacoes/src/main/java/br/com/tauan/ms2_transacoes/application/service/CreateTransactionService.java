package br.com.tauan.ms2_transacoes.application.service;

import br.com.tauan.ms2_transacoes.application.port.in.CreateTransactionCommand;
import br.com.tauan.ms2_transacoes.application.port.in.CreateTransactionUseCase;
import br.com.tauan.ms2_transacoes.application.port.out.AccountPort;
import br.com.tauan.ms2_transacoes.application.port.out.TransactionRepositoryPort;
import br.com.tauan.ms2_transacoes.domain.model.AccountInfo;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import br.com.tauan.ms2_transacoes.domain.model.TransactionDecision;
import br.com.tauan.ms2_transacoes.domain.model.TransactionPolicy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Sem @Transactional de propósito: a chamada HTTP à API Mock não deve segurar uma
 * conexão do banco. Cada gravação já roda na própria transação do repositório.
 */
@Service
public class CreateTransactionService implements CreateTransactionUseCase {

    private final TransactionRepositoryPort repository;
    private final AccountPort accountPort;
    private final TransactionPolicy policy;
    private final Clock clock;

    public CreateTransactionService(TransactionRepositoryPort repository, AccountPort accountPort,
                                    TransactionPolicy policy, Clock clock) {
        this.repository = repository;
        this.accountPort = accountPort;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public Transaction execute(CreateTransactionCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);

        // Valida o pedido ANTES de chamar a API Mock: pedido inválido vira 400
        // sem depender de um serviço externo estar no ar.
        Transaction candidate = Transaction.approved(command.userId(), command.type(), command.amount(),
                command.description(), command.category(), command.destinationUserId(), now);

        AccountInfo account = null;
        BigDecimal spentToday = null;
        BigDecimal receivedToday = null;
        if (candidate.getType().isExpense()) {
            account = accountPort.findByUserId(candidate.getUserId()).orElse(null);
            if (account != null) {
                LocalDate today = now.toLocalDate();
                LocalDateTime dayStart = today.atStartOfDay();
                LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
                spentToday = repository.sumApprovedExpenses(candidate.getUserId(), dayStart, dayEnd);
                receivedToday = repository.sumApprovedIncome(candidate.getUserId(), dayStart, dayEnd);
            }
        }

        TransactionDecision decision = policy.evaluate(candidate.getType(), candidate.getAmount(),
                account, spentToday, receivedToday);
        if (decision.approved()) {
            return repository.save(candidate);
        }
        return repository.save(Transaction.rejected(candidate.getUserId(), candidate.getType(),
                candidate.getAmount(), candidate.getDescription(), candidate.getCategory(),
                candidate.getDestinationUserId(), now, decision.reason()));
    }
}