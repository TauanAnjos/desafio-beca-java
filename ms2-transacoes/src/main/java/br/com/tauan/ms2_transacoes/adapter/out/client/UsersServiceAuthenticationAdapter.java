package br.com.tauan.ms2_transacoes.adapter.out.client;

import br.com.tauan.ms2_transacoes.application.port.out.UserAuthenticationPort;
import br.com.tauan.ms2_transacoes.domain.exception.UserServiceUnavailableException;
import br.com.tauan.ms2_transacoes.domain.model.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

@Component
public class UsersServiceAuthenticationAdapter implements UserAuthenticationPort {

    private static final Logger log = LoggerFactory.getLogger(UsersServiceAuthenticationAdapter.class);
    private static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

    private final RestClient restClient;
    private final String internalKey;

    public UsersServiceAuthenticationAdapter(@Qualifier("usersServiceRestClient") RestClient restClient,
                                             @Value("${users-service.internal-key}") String internalKey) {
        this.restClient = restClient;
        this.internalKey = internalKey;
    }

    @Override
    public Optional<AuthenticatedUser> authenticate(String email, String password) {
        UsersServiceLoginResponse response;
        try {
            response = restClient.post()
                    .uri("/auth/login")
                    .header(INTERNAL_KEY_HEADER, internalKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email, "password", password))
                    .retrieve()
                    .body(UsersServiceLoginResponse.class);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.BadRequest e) {
            return Optional.empty();
        } catch (RestClientException e) {
            // Nunca registrar a senha nem a chave: só a causa da falha (um 403 aqui indica chave errada).
            log.warn("User service call failed: {}", e.getMessage());
            throw new UserServiceUnavailableException("User service is unavailable", e);
        }

        if (response == null || response.id() == null) {
            log.warn("User service returned an incomplete login response");
            throw new UserServiceUnavailableException("User service returned an invalid response");
        }
        return Optional.of(new AuthenticatedUser(response.id(), response.name(), response.email()));
    }
}