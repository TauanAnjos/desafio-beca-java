package br.com.tauan.ms2_transacoes.adapter.in.web;

import br.com.tauan.ms2_transacoes.domain.exception.AccountServiceUnavailableException;
import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void invalidTransaction_isMappedToBadRequest() {
        ProblemDetail problem = handler.invalidTransaction(
                new InvalidTransactionException("amount must be greater than zero"));

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("amount must be greater than zero");
    }

    @Test
    void accountServiceUnavailable_isMappedToServiceUnavailable() {
        ProblemDetail problem = handler.accountServiceUnavailable(
                new AccountServiceUnavailableException("Account service is unavailable"));

        assertThat(problem.getStatus()).isEqualTo(503);
        assertThat(problem.getDetail()).isEqualTo("Account service is unavailable");
    }
}