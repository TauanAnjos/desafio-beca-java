package br.com.tauan.ms2_transacoes.domain.model;

import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Cotação do real em uma moeda: "rate" são reais por uma unidade da moeda (ex.: 5,41 reais por dólar).
 */
public record ExchangeRate(String currency, BigDecimal rate, LocalDate rateDate) {

    public ExchangeRate {
        if (currency == null || currency.trim().length() != 3) {
            throw new InvalidTransactionException("currency must be a 3-letter code");
        }
        if (rate == null || rate.signum() <= 0) {
            throw new InvalidTransactionException("exchange rate must be greater than zero");
        }
        if (rateDate == null) {
            throw new InvalidTransactionException("exchange rate date is required");
        }
        currency = currency.trim().toUpperCase();
    }

    public BigDecimal convertFromBrl(BigDecimal brlAmount) {
        return brlAmount.divide(rate, 2, RoundingMode.HALF_UP);
    }
}