package br.com.tauan.ms2_transacoes.adapter.out.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class UsersServiceClientConfig {

    @Bean
    public RestClient usersServiceRestClient(@Value("${users-service.base-url}") String baseUrl,
                                             @Value("${users-service.timeout-ms:3000}") long timeoutMillis) {
        return MockApiClientConfig.restClient(baseUrl, Duration.ofMillis(timeoutMillis));
    }
}