package br.com.tauan.ms2_transacoes.adapter.in.web;

import br.com.tauan.ms2_transacoes.application.port.in.ListTransactionsQuery;
import br.com.tauan.ms2_transacoes.application.port.in.ReportResult;
import br.com.tauan.ms2_transacoes.application.port.in.ReportTransactionsUseCase;
import br.com.tauan.ms2_transacoes.domain.model.AuthenticatedUser;
import br.com.tauan.ms2_transacoes.domain.model.Category;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import br.com.tauan.ms2_transacoes.domain.model.TransactionStatus;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportTransactionsUseCase reportTransactions;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReportController(reportTransactions))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Authentication authenticated(long userId) {
        return UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(userId, "Ana", "ana@exemplo.com"), null, List.of());
    }

    private Transaction purchase() {
        return Transaction.restore(null, 1L, TransactionType.PURCHASE, new BigDecimal("30.00"), "Mercado",
                Category.FOOD, null, TransactionStatus.APPROVED, null, LocalDateTime.of(2026, 11, 10, 10, 0));
    }

    @Test
    void report_returnsACsvAttachment_withTheBomAndTheTransactionsOfTheAuthenticatedUser() throws Exception {
        when(reportTransactions.execute(any(ListTransactionsQuery.class))).thenReturn(
                new ReportResult(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), List.of(purchase())));

        MockHttpServletResponse response = mockMvc.perform(get("/reports/transactions").principal(authenticated(7L)))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(response.getContentType()).startsWith("text/csv");
        assertThat(response.getHeader("Content-Disposition"))
                .contains("attachment").contains("transacoes-2026-11-01_2026-11-30.csv");
        byte[] body = response.getContentAsByteArray();
        assertThat(body[0]).isEqualTo((byte) 0xEF);
        assertThat(new String(body, 3, body.length - 3, java.nio.charset.StandardCharsets.UTF_8))
                .contains("Mercado");

        ArgumentCaptor<ListTransactionsQuery> captor = ArgumentCaptor.forClass(ListTransactionsQuery.class);
        verify(reportTransactions).execute(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(7L);
    }

    @Test
    void report_passesThePeriodAndTheType() throws Exception {
        when(reportTransactions.execute(any(ListTransactionsQuery.class))).thenReturn(
                new ReportResult(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 8), List.of()));

        mockMvc.perform(get("/reports/transactions").principal(authenticated(1L))
                        .param("from", "2026-11-01").param("to", "2026-11-08").param("type", "PURCHASE"))
                .andExpect(status().isOk());

        ArgumentCaptor<ListTransactionsQuery> captor = ArgumentCaptor.forClass(ListTransactionsQuery.class);
        verify(reportTransactions).execute(captor.capture());
        assertThat(captor.getValue().from()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(captor.getValue().to()).isEqualTo(LocalDate.of(2026, 11, 8));
        assertThat(captor.getValue().type()).isEqualTo(TransactionType.PURCHASE);
    }

    @Test
    void report_withAnInvalidDate_returns400() throws Exception {
        mockMvc.perform(get("/reports/transactions").principal(authenticated(1L)).param("from", "10/11/2026"))
                .andExpect(status().isBadRequest());
    }
}