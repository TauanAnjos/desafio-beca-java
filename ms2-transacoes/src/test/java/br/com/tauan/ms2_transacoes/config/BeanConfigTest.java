package br.com.tauan.ms2_transacoes.config;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class BeanConfigTest {

    @Test
    void clock_usesTheBrasiliaZone() {
        assertThat(new BeanConfig().clock().getZone()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
    }
}