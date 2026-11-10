package br.com.tauan.ms2_transacoes.adapter.in.web;

import br.com.tauan.ms2_transacoes.application.port.in.CreateTransactionCommand;
import br.com.tauan.ms2_transacoes.application.port.in.CreateTransactionUseCase;
import br.com.tauan.ms2_transacoes.domain.exception.AccountServiceUnavailableException;
import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private static final String VALID_BODY = """
            {"type":"DEPOSIT","amount":100.00,"description":"Salario"}
            """;

    @Mock
    private CreateTransactionUseCase createTransaction;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TransactionController(createTransaction))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Authentication authenticated(long userId) {
        return UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(userId, "Ana", "ana@exemplo.com"), null, List.of());
    }

    private Transaction savedDeposit() {
        return Transaction.restore(10L, 1L, TransactionType.DEPOSIT, new BigDecimal("100.00"), "Salario",
                Category.OTHER, null, TransactionStatus.APPROVED, null, LocalDateTime.of(2026, 11, 5, 10, 0));
    }

    @Test
    void create_returns201WithIdAndStatus_andTheOwnerIsTheAuthenticatedUser() throws Exception {
        when(createTransaction.execute(any(CreateTransactionCommand.class))).thenReturn(savedDeposit());

        mockMvc.perform(post("/transactions")
                        .principal(authenticated(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        ArgumentCaptor<CreateTransactionCommand> captor = ArgumentCaptor.forClass(CreateTransactionCommand.class);
        verify(createTransaction).execute(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(1L);
        assertThat(captor.getValue().type()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    void create_ignoresAnyUserHeader_theOwnerComesFromTheAuthentication() throws Exception {
        when(createTransaction.execute(any(CreateTransactionCommand.class))).thenReturn(savedDeposit());

        mockMvc.perform(post("/transactions")
                        .principal(authenticated(1L))
                        .header("X-User-Id", "99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateTransactionCommand> captor = ArgumentCaptor.forClass(CreateTransactionCommand.class);
        verify(createTransaction).execute(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(1L);
    }

    @Test
    void create_withInvalidBody_returns400ListingTheFields() throws Exception {
        mockMvc.perform(post("/transactions")
                        .principal(authenticated(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"DEPOSIT","amount":-5,"description":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("amount")))
                .andExpect(content().string(containsString("description")));
    }

    @Test
    void create_withDomainRuleViolation_returns400() throws Exception {
        when(createTransaction.execute(any(CreateTransactionCommand.class)))
                .thenThrow(new InvalidTransactionException("destination user is required for transfers"));

        mockMvc.perform(post("/transactions")
                        .principal(authenticated(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"TRANSFER","amount":50,"description":"Pix"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("destination user is required for transfers"));
    }

    @Test
    void create_whenTheAccountServiceIsDown_returns503() throws Exception {
        when(createTransaction.execute(any(CreateTransactionCommand.class)))
                .thenThrow(new AccountServiceUnavailableException("Account service is unavailable"));

        mockMvc.perform(post("/transactions")
                        .principal(authenticated(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Account service is unavailable"));
    }
}