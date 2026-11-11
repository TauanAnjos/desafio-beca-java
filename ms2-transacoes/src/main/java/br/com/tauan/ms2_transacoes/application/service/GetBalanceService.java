package br.com.tauan.ms2_transacoes.application.service;

import br.com.tauan.ms2_transacoes.application.port.in.BalanceView;
import br.com.tauan.ms2_transacoes.application.port.in.GetBalanceUseCase;
import br.com.tauan.ms2_transacoes.application.port.out.AccountPort;
import br.com.tauan.ms2_transacoes.application.port.out.TransactionRepositoryPort;
import br.com.tauan.ms2_transacoes.domain.exception.AccountNotFoundException;
import br.com.tauan.ms2_transacoes.domain.model.AccountInfo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class GetBalanceService implements GetBalanceUseCase {

    private final AccountPort accountPort;
    private final TransactionRepositoryPort repository;
    private final Clock clock;

    public GetBalanceService(AccountPort accountPort, TransactionRepositoryPort repository, Clock clock) {
        this.accountPort = accountPort;
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public BalanceView execute(Long userId) {
        AccountInfo account = accountPort.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));

        LocalDate today = LocalDate.now(clock);
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        BigDecimal spentToday = repository.sumApprovedExpenses(userId, dayStart, dayEnd);
        BigDecimal receivedToday = repository.sumApprovedIncome(userId, dayStart, dayEnd);

        return new BalanceView(account, spentToday, receivedToday,
                account.availableBalance(spentToday, receivedToday));
    }
}