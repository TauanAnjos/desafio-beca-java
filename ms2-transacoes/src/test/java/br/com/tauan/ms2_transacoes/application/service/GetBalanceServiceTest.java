package br.com.tauan.ms2_transacoes.application.service;

import br.com.tauan.ms2_transacoes.application.port.in.BalanceView;
import br.com.tauan.ms2_transacoes.application.port.out.AccountPort;
import br.com.tauan.ms2_transacoes.application.port.out.TransactionRepositoryPort;
import br.com.tauan.ms2_transacoes.domain.exception.AccountNotFoundException;
import br.com.tauan.ms2_transacoes.domain.exception.AccountServiceUnavailableException;
import br.com.tauan.ms2_transacoes.domain.model.AccountInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBalanceServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"),
            ZoneId.of("America/Sao_Paulo"));
    private static final LocalDateTime DAY_START = LocalDateTime.of(2026, 11, 10, 0, 0);
    private static final LocalDateTime DAY_END = LocalDateTime.of(2026, 11, 11, 0, 0);

    @Mock
    private AccountPort accountPort;

    @Mock
    private TransactionRepositoryPort repository;

    private GetBalanceService service;

    @BeforeEach
    void setUp() {
        service = new GetBalanceService(accountPort, repository, CLOCK);
    }

    @Test
    void execute_returnsTheBalanceOfTheMockApi_andTheAvailableBalanceOfTheDay() {
        when(accountPort.findByUserId(1L)).thenReturn(Optional.of(new AccountInfo(1L,
                new BigDecimal("1000"), new BigDecimal("5000"), new BigDecimal("2000"))));
        when(repository.sumApprovedExpenses(1L, DAY_START, DAY_END)).thenReturn(new BigDecimal("160"));
        when(repository.sumApprovedIncome(1L, DAY_START, DAY_END)).thenReturn(new BigDecimal("500"));

        BalanceView view = service.execute(1L);

        assertThat(view.account().balance()).isEqualByComparingTo("1000");
        assertThat(view.spentToday()).isEqualByComparingTo("160");
        assertThat(view.receivedToday()).isEqualByComparingTo("500");
        assertThat(view.availableBalance()).isEqualByComparingTo("1340");
    }

    @Test
    void execute_withoutAnAccount_throwsNotFound() {
        when(accountPort.findByUserId(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(9L)).isInstanceOf(AccountNotFoundException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void execute_whenTheMockApiIsDown_theErrorPropagates() {
        when(accountPort.findByUserId(1L)).thenThrow(new AccountServiceUnavailableException("down"));

        assertThatThrownBy(() -> service.execute(1L)).isInstanceOf(AccountServiceUnavailableException.class);

        verifyNoInteractions(repository);
    }
}