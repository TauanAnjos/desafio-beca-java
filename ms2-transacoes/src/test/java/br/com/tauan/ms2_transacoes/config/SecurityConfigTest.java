package br.com.tauan.ms2_transacoes.config;

import br.com.tauan.ms2_transacoes.adapter.in.web.TransactionController;
import br.com.tauan.ms2_transacoes.adapter.in.web.TransactionQueryController;
import br.com.tauan.ms2_transacoes.adapter.in.web.security.ApiAuthenticationEntryPoint;
import br.com.tauan.ms2_transacoes.adapter.in.web.security.UsersServiceAuthenticationProvider;
import br.com.tauan.ms2_transacoes.application.port.in.CreateTransactionCommand;
import br.com.tauan.ms2_transacoes.application.port.in.CreateTransactionUseCase;
import br.com.tauan.ms2_transacoes.application.port.in.GetTransactionUseCase;
import br.com.tauan.ms2_transacoes.application.port.in.ListTransactionsQuery;
import br.com.tauan.ms2_transacoes.application.port.in.ListTransactionsUseCase;
import br.com.tauan.ms2_transacoes.application.port.out.UserAuthenticationPort;
import br.com.tauan.ms2_transacoes.domain.exception.UserServiceUnavailableException;
import br.com.tauan.ms2_transacoes.domain.model.AuthenticatedUser;
import br.com.tauan.ms2_transacoes.domain.model.Category;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import br.com.tauan.ms2_transacoes.domain.model.TransactionStatus;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {TransactionController.class, TransactionQueryController.class})
@Import({SecurityConfig.class, ApiAuthenticationEntryPoint.class, UsersServiceAuthenticationProvider.class})
class SecurityConfigTest {

    private static final String EMAIL = "ana@exemplo.com";
    private static final String PASSWORD = "Senha@1234";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAuthenticationPort userAuthentication;

    @MockitoBean
    private CreateTransactionUseCase createTransaction;

    @MockitoBean
    private GetTransactionUseCase getTransaction;

    @MockitoBean
    private ListTransactionsUseCase listTransactions;

    private String basic(String email, String password) {
        String credentials = email + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private void loginSucceedsAsUser(long id) {
        when(userAuthentication.authenticate(EMAIL, PASSWORD))
                .thenReturn(Optional.of(new AuthenticatedUser(id, "Ana Souza", EMAIL)));
    }

    @Test
    void withoutCredentials_returns401WithTheBasicChallenge() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Basic")))
                .andExpect(content().string(containsString("Unauthorized")));
    }

    @Test
    void withTheWrongPassword_returns401() throws Exception {
        when(userAuthentication.authenticate(EMAIL, "errada")).thenReturn(Optional.empty());

        mockMvc.perform(get("/transactions").header("Authorization", basic(EMAIL, "errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenTheUserServiceIsDown_returns503AsJson() throws Exception {
        when(userAuthentication.authenticate(EMAIL, PASSWORD))
                .thenThrow(new UserServiceUnavailableException("down"));

        mockMvc.perform(get("/transactions").header("Authorization", basic(EMAIL, PASSWORD)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("application/problem+json")))
                .andExpect(content().string(containsString("User service is unavailable")));
    }

    @Test
    void withValidCredentials_theUserOfTheLoginIsUsed() throws Exception {
        loginSucceedsAsUser(5L);
        when(listTransactions.execute(any(ListTransactionsQuery.class))).thenReturn(List.of());

        mockMvc.perform(get("/transactions").header("Authorization", basic(EMAIL, PASSWORD)))
                .andExpect(status().isOk());

        ArgumentCaptor<ListTransactionsQuery> captor = ArgumentCaptor.forClass(ListTransactionsQuery.class);
        verify(listTransactions).execute(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(5L);
    }

    @Test
    void listingTheTransactionsOfAnotherUser_returns403() throws Exception {
        loginSucceedsAsUser(5L);

        mockMvc.perform(get("/transactions").param("userId", "9")
                        .header("Authorization", basic(EMAIL, PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void theUserHeaderIsIgnored_theOwnerIsTheAuthenticatedUser() throws Exception {
        loginSucceedsAsUser(5L);
        when(createTransaction.execute(any(CreateTransactionCommand.class))).thenReturn(
                Transaction.restore(10L, 5L, TransactionType.DEPOSIT, new BigDecimal("100.00"), "Salario",
                        Category.OTHER, null, TransactionStatus.APPROVED, null, LocalDateTime.of(2026, 11, 9, 10, 0)));

        mockMvc.perform(post("/transactions")
                        .header("Authorization", basic(EMAIL, PASSWORD))
                        .header("X-User-Id", "99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"DEPOSIT","amount":100,"description":"Salario"}
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateTransactionCommand> captor = ArgumentCaptor.forClass(CreateTransactionCommand.class);
        verify(createTransaction).execute(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(5L);
    }

    @Test
    void healthEndpoint_isNotProtected() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }
}