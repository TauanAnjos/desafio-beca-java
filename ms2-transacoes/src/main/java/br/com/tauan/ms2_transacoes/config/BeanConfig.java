package br.com.tauan.ms2_transacoes.config;

import br.com.tauan.ms2_transacoes.domain.model.TransactionPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class BeanConfig {

    /**
     * Fuso do negócio: o "dia" dos limites começa à meia-noite em Brasília, e não à meia-noite
     * do servidor (num container em UTC o dia viraria às 21h).
     */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    @Bean
    public Clock clock() {
        return Clock.system(BUSINESS_ZONE);
    }

    @Bean
    public TransactionPolicy transactionPolicy() {
        return new TransactionPolicy();
    }
}