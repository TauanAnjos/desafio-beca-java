package br.com.tauan.ms2_transacoes.adapter.out.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MockApiAccountResponse(Long userId, BigDecimal balance, BigDecimal dailyLimit,
                                     BigDecimal perTransactionLimit) {
}