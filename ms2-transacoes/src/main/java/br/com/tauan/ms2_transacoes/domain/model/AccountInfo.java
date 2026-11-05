package br.com.tauan.ms2_transacoes.domain.model;

import java.math.BigDecimal;

public record AccountInfo(Long userId, BigDecimal balance, BigDecimal dailyLimit,
                          BigDecimal perTransactionLimit) {
}