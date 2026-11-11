package br.com.tauan.ms2_transacoes.adapter.in.web.dto;

import br.com.tauan.ms2_transacoes.domain.model.Category;
import br.com.tauan.ms2_transacoes.domain.model.ExchangeRate;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import br.com.tauan.ms2_transacoes.domain.model.TransactionStatus;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionResponseTest {

    private Transaction transaction(List<ExchangeRate> rates) {
        return Transaction.restore(10L, 1L, TransactionType.PURCHASE, new BigDecimal("108.20"), "Mercado",
                Category.FOOD, null, TransactionStatus.APPROVED, null,
                LocalDateTime.of(2026, 11, 9, 10, 0), rates);
    }

    @Test
    void withRates_showsTheOriginalAmountAndTheConvertedValues() {
        TransactionResponse response = TransactionResponse.from(transaction(List.of(
                new ExchangeRate("USD", new BigDecimal("5.41"), LocalDate.of(2026, 11, 6)),
                new ExchangeRate("EUR", new BigDecimal("6.20"), LocalDate.of(2026, 11, 6)))));

        assertThat(response.amount()).isEqualByComparingTo("108.20");
        assertThat(response.conversionAvailable()).isTrue();
        assertThat(response.conversions()).hasSize(2);
        assertThat(response.conversions().get(0).currency()).isEqualTo("USD");
        assertThat(response.conversions().get(0).convertedAmount()).isEqualByComparingTo("20.00");
        assertThat(response.conversions().get(0).rateDate()).isEqualTo(LocalDate.of(2026, 11, 6));
    }

    @Test
    void withoutRates_signalsThatTheConversionIsUnavailable() {
        TransactionResponse response = TransactionResponse.from(transaction(List.of()));

        assertThat(response.conversionAvailable()).isFalse();
        assertThat(response.conversions()).isEmpty();
    }
    @Test
    void withOnlyOneCurrency_theConversionIsNotAvailable_butTheRateStillAppears() {
        TransactionResponse response = TransactionResponse.from(transaction(List.of(
                new ExchangeRate("USD", new BigDecimal("5.41"), LocalDate.of(2026, 11, 6)))));
        assertThat(response.conversionAvailable()).isFalse();
        assertThat(response.conversions()).hasSize(1);
    }
}