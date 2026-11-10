package br.com.tauan.ms2_transacoes.domain.model;

import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRateTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 6);

    @Test
    void convertFromBrl_dividesByTheRate_roundingToTwoDecimals() {
        ExchangeRate usd = new ExchangeRate("usd", new BigDecimal("5.4125"), DATE);

        assertThat(usd.currency()).isEqualTo("USD");
        assertThat(usd.convertFromBrl(new BigDecimal("100.00"))).isEqualByComparingTo("18.48");
    }

    @Test
    void invalidData_isRejected() {
        assertThatThrownBy(() -> new ExchangeRate("DOLAR", BigDecimal.ONE, DATE))
                .isInstanceOf(InvalidTransactionException.class);
        assertThatThrownBy(() -> new ExchangeRate("USD", BigDecimal.ZERO, DATE))
                .isInstanceOf(InvalidTransactionException.class);
        assertThatThrownBy(() -> new ExchangeRate("USD", null, DATE))
                .isInstanceOf(InvalidTransactionException.class);
        assertThatThrownBy(() -> new ExchangeRate("USD", BigDecimal.ONE, null))
                .isInstanceOf(InvalidTransactionException.class);
    }
}