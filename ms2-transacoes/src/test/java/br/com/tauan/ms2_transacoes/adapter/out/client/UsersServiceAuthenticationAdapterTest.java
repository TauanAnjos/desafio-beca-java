package br.com.tauan.ms2_transacoes.adapter.out.client;

import br.com.tauan.ms2_transacoes.domain.exception.UserServiceUnavailableException;
import br.com.tauan.ms2_transacoes.domain.model.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class UsersServiceAuthenticationAdapterTest {

    private static final String URL = "http://users.test/auth/login";

    private MockRestServiceServer server;
    private UsersServiceAuthenticationAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://users.test");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new UsersServiceAuthenticationAdapter(builder.build());
    }

    @Test
    void authenticate_withValidCredentials_returnsTheUser() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"))
                .andExpect(jsonPath("$.password").value("Senha@1234"))
                .andRespond(withSuccess("""
                        {"id":1,"name":"Ana Souza","email":"ana@exemplo.com"}
                        """, MediaType.APPLICATION_JSON));

        Optional<AuthenticatedUser> user = adapter.authenticate("ana@exemplo.com", "Senha@1234");

        assertThat(user).contains(new AuthenticatedUser(1L, "Ana Souza", "ana@exemplo.com"));
        server.verify();
    }

    @Test
    void authenticate_withInvalidCredentials_returnsEmpty() {
        server.expect(requestTo(URL)).andRespond(withUnauthorizedRequest());

        assertThat(adapter.authenticate("ana@exemplo.com", "errada")).isEmpty();
    }

    @Test
    void authenticate_whenTheUserServiceRejectsTheBody_returnsEmpty() {
        server.expect(requestTo(URL)).andRespond(withBadRequest());

        assertThat(adapter.authenticate("", "")).isEmpty();
    }

    @Test
    void authenticate_whenTheUserServiceFails_throwsUnavailable() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> adapter.authenticate("ana@exemplo.com", "Senha@1234"))
                .isInstanceOf(UserServiceUnavailableException.class);
    }

    @Test
    void authenticate_whenTheConnectionFails_throwsUnavailable() {
        server.expect(requestTo(URL)).andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> adapter.authenticate("ana@exemplo.com", "Senha@1234"))
                .isInstanceOf(UserServiceUnavailableException.class);
    }

    @Test
    void authenticate_withAnIncompleteResponse_throwsUnavailable() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.authenticate("ana@exemplo.com", "Senha@1234"))
                .isInstanceOf(UserServiceUnavailableException.class)
                .hasMessageContaining("invalid response");
    }
}