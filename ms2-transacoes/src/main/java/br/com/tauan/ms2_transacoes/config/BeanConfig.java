package br.com.tauan.ms2_transacoes.config;

import br.com.tauan.ms2_transacoes.domain.model.TransactionPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class BeanConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public TransactionPolicy transactionPolicy() {
        return new TransactionPolicy();
    }
}