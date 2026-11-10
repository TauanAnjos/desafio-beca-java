package br.com.tauan.ms2_transacoes.application.service;

import br.com.tauan.ms2_transacoes.application.port.in.ListTransactionsQuery;
import br.com.tauan.ms2_transacoes.application.port.in.ReportResult;
import br.com.tauan.ms2_transacoes.application.port.out.TransactionRepositoryPort;
import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;
import br.com.tauan.ms2_transacoes.domain.model.Category;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import br.com.tauan.ms2_transacoes.domain.model.TransactionStatus;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class ReportTransactionsServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"),
            ZoneId.of("America/Sao_Paulo"));

    @Mock
    private TransactionRepositoryPort repository;

    private ReportTransactionsService service;

    @BeforeEach
    void setUp() {
        service = new ReportTransactionsService(repository, CLOCK);
    }

    private Transaction at(LocalDateTime when) {
        return Transaction.restore(null, 1L, TransactionType.PURCHASE, new BigDecimal("10.00"), "Compra",
                Category.FOOD, null, TransactionStatus.APPROVED, null, when);
    }

    @Test
    void withoutAPeriod_theCurrentMonthIsUsed_andTheOrderIsChronological() {
        Transaction older = at(LocalDateTime.of(2026, 11, 2, 10, 0));
        Transaction newer = at(LocalDateTime.of(2026, 11, 9, 10, 0));
        when(repository.findByUser(1L, LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 12, 1, 0, 0), null)).thenReturn(List.of(newer, older));

        ReportResult result = service.execute(new ListTransactionsQuery(1L, null, null, null));

        assertThat(result.from()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(result.to()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(result.transactions()).containsExactly(older, newer);
    }

    @Test
    void withAPeriodAndAType_theyArePassedToTheRepository() {
        when(repository.findByUser(1L, LocalDateTime.of(2026, 11, 5, 0, 0),
                LocalDateTime.of(2026, 11, 9, 0, 0), TransactionType.PURCHASE)).thenReturn(List.of());

        ReportResult result = service.execute(new ListTransactionsQuery(1L,
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 8), TransactionType.PURCHASE));

        assertThat(result.transactions()).isEmpty();
    }

    @Test
    void fromAfterTo_isInvalid() {
        assertThatThrownBy(() -> service.execute(new ListTransactionsQuery(1L,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1), null)))
                .isInstanceOf(InvalidTransactionException.class);

        verifyNoInteractions(repository);
    }
}