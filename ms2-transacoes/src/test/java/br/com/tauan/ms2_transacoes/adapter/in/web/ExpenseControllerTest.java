package br.com.tauan.ms2_transacoes.adapter.in.web;

import br.com.tauan.ms2_transacoes.application.port.in.ExpenseSummaryQuery;
import br.com.tauan.ms2_transacoes.application.port.in.ExpenseSummaryResult;
import br.com.tauan.ms2_transacoes.application.port.in.GetExpenseSummaryUseCase;
import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;
import br.com.tauan.ms2_transacoes.domain.model.AuthenticatedUser;
import br.com.tauan.ms2_transacoes.domain.model.Category;
import br.com.tauan.ms2_transacoes.domain.model.ExpenseSummary;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import br.com.tauan.ms2_transacoes.domain.model.TransactionStatus;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExpenseControllerTest {

    @Mock
    private GetExpenseSummaryUseCase getExpenseSummary;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExpenseController(getExpenseSummary))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Authentication authenticated(long userId) {
        return UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(userId, "Ana", "ana@exemplo.com"), null, List.of());
    }

    private Transaction purchase(String amount, Category category) {
        return Transaction.restore(null, 1L, TransactionType.PURCHASE, new BigDecimal(amount), "Compra",
                category, null, TransactionStatus.APPROVED, null, LocalDateTime.of(2026, 11, 10, 10, 0));
    }

    @Test
    void summary_returnsTheTotalsByCategoryDayAndMonth_biggestCategoryFirst() throws Exception {
        ExpenseSummary summary = ExpenseSummary.of(List.of(
                purchase("30.00", Category.FOOD), purchase("20.00", Category.FOOD),
                purchase("40.00", Category.TRANSPORT), purchase("70.00", Category.HEALTH)));
        when(getExpenseSummary.execute(any(ExpenseSummaryQuery.class))).thenReturn(
                new ExpenseSummaryResult(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), summary));

        mockMvc.perform(get("/expenses/summary").principal(authenticated(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(160.0))
                .andExpect(jsonPath("$.byCategory.length()").value(3))
                .andExpect(jsonPath("$.byCategory[0].category").value("HEALTH"))
                .andExpect(jsonPath("$.byCategory[1].category").value("FOOD"))
                .andExpect(jsonPath("$.byDay[0].date").value("2026-11-10"))
                .andExpect(jsonPath("$.byMonth[0].month").value("2026-11"));
    }

    @Test
    void summary_usesTheAuthenticatedUser_andPassesThePeriod() throws Exception {
        when(getExpenseSummary.execute(any(ExpenseSummaryQuery.class))).thenReturn(
                new ExpenseSummaryResult(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 8),
                        ExpenseSummary.of(List.of())));

        mockMvc.perform(get("/expenses/summary").principal(authenticated(7L))
                        .param("from", "2026-11-01").param("to", "2026-11-08"))
                .andExpect(status().isOk());

        ArgumentCaptor<ExpenseSummaryQuery> captor = ArgumentCaptor.forClass(ExpenseSummaryQuery.class);
        verify(getExpenseSummary).execute(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(7L);
        assertThat(captor.getValue().from()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(captor.getValue().to()).isEqualTo(LocalDate.of(2026, 11, 8));
    }

    @Test
    void summary_withFromAfterTo_returns400() throws Exception {
        when(getExpenseSummary.execute(any(ExpenseSummaryQuery.class)))
                .thenThrow(new InvalidTransactionException("'from' must not be after 'to'"));

        mockMvc.perform(get("/expenses/summary").principal(authenticated(1L))
                        .param("from", "2026-11-10").param("to", "2026-11-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void summary_withAnInvalidDate_returns400() throws Exception {
        mockMvc.perform(get("/expenses/summary").principal(authenticated(1L)).param("from", "10/11/2026"))
                .andExpect(status().isBadRequest());
    }
}