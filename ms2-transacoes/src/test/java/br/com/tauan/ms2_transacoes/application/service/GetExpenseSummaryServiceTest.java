package br.com.tauan.ms2_transacoes.application.service;

import br.com.tauan.ms2_transacoes.application.port.in.ExpenseSummaryQuery;
import br.com.tauan.ms2_transacoes.application.port.in.ExpenseSummaryResult;
import br.com.tauan.ms2_transacoes.application.port.out.TransactionRepositoryPort;
import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetExpenseSummaryServiceTest {

    // 2026-11-10 12:00 em Brasília
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"),
            ZoneId.of("America/Sao_Paulo"));

    @Mock
    private TransactionRepositoryPort repository;

    private GetExpenseSummaryService service;

    @BeforeEach
    void setUp() {
        service = new GetExpenseSummaryService(repository, CLOCK);
    }

    @Test
    void withoutAPeriod_theCurrentMonthIsUsed() {
        when(repository.findByUser(1L, LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 12, 1, 0, 0), null)).thenReturn(List.of());

        ExpenseSummaryResult result = service.execute(new ExpenseSummaryQuery(1L, null, null));

        assertThat(result.from()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(result.to()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(result.summary().total()).isEqualByComparingTo("0");
    }

    @Test
    void withAPeriod_theLastDayEntersCompletely() {
        when(repository.findByUser(1L, LocalDateTime.of(2026, 11, 5, 0, 0),
                LocalDateTime.of(2026, 11, 9, 0, 0), null)).thenReturn(List.of());

        ExpenseSummaryResult result = service.execute(new ExpenseSummaryQuery(1L,
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 8)));

        assertThat(result.from()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(result.to()).isEqualTo(LocalDate.of(2026, 11, 8));
    }

    @Test
    void fromAfterTo_isInvalid() {
        assertThatThrownBy(() -> service.execute(new ExpenseSummaryQuery(1L,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1))))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("'from' must not be after 'to'");

        verifyNoInteractions(repository);
    }
}