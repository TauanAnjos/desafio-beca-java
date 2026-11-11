package br.com.tauan.ms2_transacoes.adapter.out.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class BrasilApiClientConfig {

    @Bean
    public RestClient brasilApiRestClient(@Value("${brasilapi.base-url}") String baseUrl,
                                          @Value("${brasilapi.timeout-ms:2000}") long timeoutMillis) {
        return MockApiClientConfig.restClient(baseUrl, Duration.ofMillis(timeoutMillis));
    }
}