package br.com.tauan.ms2_transacoes.adapter.out.client;

import br.com.tauan.ms2_transacoes.application.port.out.UserAuthenticationPort;
import br.com.tauan.ms2_transacoes.domain.exception.UserServiceUnavailableException;
import br.com.tauan.ms2_transacoes.domain.model.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

@Component
public class UsersServiceAuthenticationAdapter implements UserAuthenticationPort {

    private final RestClient restClient;

    public UsersServiceAuthenticationAdapter(@Qualifier("usersServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<AuthenticatedUser> authenticate(String email, String password) {
        UsersServiceLoginResponse response;
        try {
            response = restClient.post()
                    .uri("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email, "password", password))
                    .retrieve()
                    .body(UsersServiceLoginResponse.class);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.BadRequest e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new UserServiceUnavailableException("User service is unavailable", e);
        }

        if (response == null || response.id() == null) {
            throw new UserServiceUnavailableException("User service returned an invalid response");
        }
        return Optional.of(new AuthenticatedUser(response.id(), response.name(), response.email()));
    }
}