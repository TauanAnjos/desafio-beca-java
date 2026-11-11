package br.com.tauan.ms2_transacoes.application.service;

import br.com.tauan.ms2_transacoes.application.port.in.CreateTransactionCommand;
import br.com.tauan.ms2_transacoes.application.port.in.CreateTransactionUseCase;
import br.com.tauan.ms2_transacoes.application.port.out.AccountPort;
import br.com.tauan.ms2_transacoes.application.port.out.ExchangeRatePort;
import br.com.tauan.ms2_transacoes.application.port.out.TransactionRepositoryPort;
import br.com.tauan.ms2_transacoes.domain.model.AccountInfo;
import br.com.tauan.ms2_transacoes.domain.model.ExchangeRate;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import br.com.tauan.ms2_transacoes.domain.model.TransactionDecision;
import br.com.tauan.ms2_transacoes.domain.model.TransactionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Sem @Transactional de propósito: as chamadas HTTP (API Mock e BrasilAPI) não devem segurar uma
 * conexão do banco. Cada gravação já roda na própria transação do repositório.
 */
@Service
public class CreateTransactionService implements CreateTransactionUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateTransactionService.class);
    private static final List<String> CURRENCIES = List.of("USD", "EUR");

    private final TransactionRepositoryPort repository;
    private final AccountPort accountPort;
    private final TransactionPolicy policy;
    private final Clock clock;
    private final ExchangeRatePort exchangeRatePort;

    public CreateTransactionService(TransactionRepositoryPort repository, AccountPort accountPort,
                                    TransactionPolicy policy, Clock clock, ExchangeRatePort exchangeRatePort) {
        this.repository = repository;
        this.accountPort = accountPort;
        this.policy = policy;
        this.clock = clock;
        this.exchangeRatePort = exchangeRatePort;
    }

    @Override
    public Transaction execute(CreateTransactionCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);

        // Valida o pedido ANTES de chamar serviços externos: pedido inválido vira 400
        // sem depender da API Mock nem da BrasilAPI estarem no ar.
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
        Transaction decided = decision.approved()
                ? candidate
                : Transaction.rejected(candidate.getUserId(), candidate.getType(), candidate.getAmount(),
                candidate.getDescription(), candidate.getCategory(), candidate.getDestinationUserId(),
                now, decision.reason());

        List<ExchangeRate> rates = findRates();
        return repository.save(rates.isEmpty() ? decided : decided.withExchangeRates(rates));
    }

    /**
     * A cotação é guardada na transação (snapshot). Se não estiver disponível, a transação é criada
     * mesmo assim, sem taxas.
     */
    private List<ExchangeRate> findRates() {
        try {
            return CURRENCIES.stream()
                    .map(exchangeRatePort::findLatest)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (RuntimeException e) {
            log.warn("Exchange rates could not be retrieved: {}", e.getMessage());
            return List.of();
        }
    }
}