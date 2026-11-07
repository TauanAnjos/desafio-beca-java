package br.com.tauan.ms2_transacoes.adapter.in.web;

import br.com.tauan.ms2_transacoes.application.port.in.GetTransactionUseCase;
import br.com.tauan.ms2_transacoes.application.port.in.ListTransactionsQuery;
import br.com.tauan.ms2_transacoes.application.port.in.ListTransactionsUseCase;
import br.com.tauan.ms2_transacoes.domain.exception.TransactionNotFoundException;
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
class TransactionQueryControllerTest {

    @Mock
    private GetTransactionUseCase getTransaction;

    @Mock
    private ListTransactionsUseCase listTransactions;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TransactionQueryController(getTransaction, listTransactions))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Transaction transaction() {
        return Transaction.restore(10L, 1L, TransactionType.PURCHASE, new BigDecimal("50.00"), "Mercado",
                Category.FOOD, null, TransactionStatus.APPROVED, null, LocalDateTime.of(2026, 11, 5, 10, 0));
    }

    @Test
    void get_returnsTheTransaction() throws Exception {
        when(getTransaction.execute(1L, 10L)).thenReturn(transaction());

        mockMvc.perform(get("/transactions/10").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void get_whenNotFound_returns404() throws Exception {
        when(getTransaction.execute(1L, 99L)).thenThrow(new TransactionNotFoundException(99L));

        mockMvc.perform(get("/transactions/99").header("X-User-Id", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Transaction not found: 99"));
    }

    @Test
    void list_passesTheFiltersToTheUseCase() throws Exception {
        when(listTransactions.execute(any(ListTransactionsQuery.class))).thenReturn(List.of(transaction()));

        mockMvc.perform(get("/transactions")
                        .header("X-User-Id", "1")
                        .param("from", "2026-11-01")
                        .param("to", "2026-11-05")
                        .param("type", "PURCHASE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));

        ArgumentCaptor<ListTransactionsQuery> captor = ArgumentCaptor.forClass(ListTransactionsQuery.class);
        verify(listTransactions).execute(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(1L);
        assertThat(captor.getValue().from()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(captor.getValue().to()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(captor.getValue().type()).isEqualTo(TransactionType.PURCHASE);
    }

    @Test
    void list_withAnInvalidDate_returns400() throws Exception {
        mockMvc.perform(get("/transactions")
                        .header("X-User-Id", "1")
                        .param("from", "05/11/2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_withAnInvalidType_returns400() throws Exception {
        mockMvc.perform(get("/transactions")
                        .header("X-User-Id", "1")
                        .param("type", "ABC"))
                .andExpect(status().isBadRequest());
    }
}