package br.com.tauan.ms2_transacoes.adapter.out.client;

import br.com.tauan.ms2_transacoes.application.port.out.AccountPort;
import br.com.tauan.ms2_transacoes.domain.exception.AccountServiceUnavailableException;
import br.com.tauan.ms2_transacoes.domain.model.AccountInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Component
public class MockApiAccountAdapter implements AccountPort {

    private final RestClient restClient;

    public MockApiAccountAdapter(@Qualifier("mockApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<AccountInfo> findByUserId(Long userId) {
        List<MockApiAccountResponse> accounts;
        try {
            accounts = restClient.get()
                    .uri(uri -> uri.path("/accounts").queryParam("userId", userId).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<MockApiAccountResponse>>() {
                    });
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new AccountServiceUnavailableException("Account service is unavailable", e);
        }

        if (accounts == null) {
            return Optional.empty();
        }
        // O MockAPI filtra por texto: confirma o userId exato antes de usar o registro.
        return accounts.stream()
                .filter(account -> userId.equals(account.userId()))
                .findFirst()
                .map(account -> toDomain(userId, account));
    }

    private AccountInfo toDomain(Long userId, MockApiAccountResponse account) {
        if (account.balance() == null || account.dailyLimit() == null || account.perTransactionLimit() == null) {
            throw new AccountServiceUnavailableException(
                    "Account service returned incomplete data for user " + userId);
        }
        return new AccountInfo(account.userId(), account.balance(), account.dailyLimit(),
                account.perTransactionLimit());
    }
}