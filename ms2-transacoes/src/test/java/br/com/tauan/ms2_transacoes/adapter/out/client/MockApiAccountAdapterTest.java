package br.com.tauan.ms2_transacoes.adapter.out.client;

import br.com.tauan.ms2_transacoes.domain.exception.AccountServiceUnavailableException;
import br.com.tauan.ms2_transacoes.domain.model.AccountInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MockApiAccountAdapterTest {

    private static final String URL = "http://mockapi.test/api/v1/accounts?userId=1";

    private MockRestServiceServer server;
    private MockApiAccountAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://mockapi.test/api/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new MockApiAccountAdapter(builder.build());
    }

    @Test
    void findByUserId_returnsBalanceAndLimits() {
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id":"1","userId":1,"balance":1000.50,"dailyLimit":5000,"perTransactionLimit":2000}]
                        """, MediaType.APPLICATION_JSON));

        Optional<AccountInfo> account = adapter.findByUserId(1L);

        assertThat(account).isPresent();
        assertThat(account.get().balance()).isEqualByComparingTo("1000.50");
        assertThat(account.get().dailyLimit()).isEqualByComparingTo("5000");
        assertThat(account.get().perTransactionLimit()).isEqualByComparingTo("2000");
        server.verify();
    }

    @Test
    void findByUserId_usesOnlyTheExactUserId() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("""
                        [{"userId":10,"balance":1,"dailyLimit":1,"perTransactionLimit":1},
                         {"userId":1,"balance":300,"dailyLimit":900,"perTransactionLimit":100}]
                        """, MediaType.APPLICATION_JSON));

        Optional<AccountInfo> account = adapter.findByUserId(1L);

        assertThat(account).isPresent();
        assertThat(account.get().balance()).isEqualByComparingTo("300");
    }

    @Test
    void findByUserId_whenNotFound_returnsEmpty() {
        server.expect(requestTo(URL)).andRespond(withResourceNotFound());

        assertThat(adapter.findByUserId(1L)).isEmpty();
    }

    @Test
    void findByUserId_withEmptyList_returnsEmpty() {
        server.expect(requestTo(URL)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(adapter.findByUserId(1L)).isEmpty();
    }

    @Test
    void findByUserId_whenServiceFails_throwsUnavailable() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> adapter.findByUserId(1L))
                .isInstanceOf(AccountServiceUnavailableException.class);
    }

    @Test
    void findByUserId_withIncompleteData_throwsUnavailable() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("""
                        [{"userId":1,"balance":100}]
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.findByUserId(1L))
                .isInstanceOf(AccountServiceUnavailableException.class)
                .hasMessageContaining("incomplete data");
    }
}