package br.com.tauan.ms2_transacoes.adapter.out.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class MockApiClientConfig {

    @Bean
    public RestClient mockApiRestClient(@Value("${mockapi.base-url}") String baseUrl,
                                        @Value("${mockapi.timeout-ms:3000}") long timeoutMillis) {
        return restClient(baseUrl, Duration.ofMillis(timeoutMillis));
    }

    static RestClient restClient(String baseUrl, Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}