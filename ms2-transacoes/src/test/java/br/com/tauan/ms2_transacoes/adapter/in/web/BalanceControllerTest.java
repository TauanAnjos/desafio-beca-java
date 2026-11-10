package br.com.tauan.ms2_transacoes.adapter.in.web;

import br.com.tauan.ms2_transacoes.application.port.in.BalanceView;
import br.com.tauan.ms2_transacoes.application.port.in.GetBalanceUseCase;
import br.com.tauan.ms2_transacoes.domain.exception.AccountNotFoundException;
import br.com.tauan.ms2_transacoes.domain.exception.AccountServiceUnavailableException;
import br.com.tauan.ms2_transacoes.domain.model.AccountInfo;
import br.com.tauan.ms2_transacoes.domain.model.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BalanceControllerTest {

    @Mock
    private GetBalanceUseCase getBalance;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BalanceController(getBalance))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Authentication authenticated(long userId) {
        return UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(userId, "Ana", "ana@exemplo.com"), null, List.of());
    }

    @Test
    void get_returnsTheBalanceOfTheMockApiAndTheAvailableBalance() throws Exception {
        AccountInfo account = new AccountInfo(1L, new BigDecimal("1000"), new BigDecimal("5000"),
                new BigDecimal("2000"));
        when(getBalance.execute(1L)).thenReturn(new BalanceView(account, new BigDecimal("160"),
                new BigDecimal("500"), new BigDecimal("1340")));

        mockMvc.perform(get("/balance").principal(authenticated(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.balance").value(1000.0))
                .andExpect(jsonPath("$.availableBalance").value(1340.0))
                .andExpect(jsonPath("$.spentToday").value(160.0))
                .andExpect(jsonPath("$.receivedToday").value(500.0));
    }

    @Test
    void get_withoutAnAccount_returns404() throws Exception {
        when(getBalance.execute(9L)).thenThrow(new AccountNotFoundException(9L));

        mockMvc.perform(get("/balance").principal(authenticated(9L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_whenTheMockApiIsDown_returns503() throws Exception {
        when(getBalance.execute(1L)).thenThrow(new AccountServiceUnavailableException("Account service is unavailable"));

        mockMvc.perform(get("/balance").principal(authenticated(1L)))
                .andExpect(status().isServiceUnavailable());
    }
}