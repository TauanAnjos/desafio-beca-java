package br.com.tauan.ms2_transacoes.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.time.LocalDate;

@Embeddable
public class ExchangeRateEmbeddable {

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "rate", nullable = false, precision = 19, scale = 6)
    private BigDecimal rate;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    protected ExchangeRateEmbeddable() {
    }

    public ExchangeRateEmbeddable(String currency, BigDecimal rate, LocalDate rateDate) {
        this.currency = currency;
        this.rate = rate;
        this.rateDate = rateDate;
    }

    public String getCurrency() { return currency; }
    public BigDecimal getRate() { return rate; }
    public LocalDate getRateDate() { return rateDate; }
}