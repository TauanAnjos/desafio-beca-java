package br.com.tauan.ms2_transacoes.domain.model;

import java.math.BigDecimal;

public record AccountInfo(Long userId, BigDecimal balance, BigDecimal dailyLimit,
                          BigDecimal perTransactionLimit) {

    /**
     * Saldo disponível: o saldo da API Mock mais os depósitos aprovados do dia, menos as despesas
     * aprovadas do dia. Valores nulos contam como zero.
     */
    public BigDecimal availableBalance(BigDecimal spentToday, BigDecimal receivedToday) {
        BigDecimal spent = spentToday == null ? BigDecimal.ZERO : spentToday;
        BigDecimal received = receivedToday == null ? BigDecimal.ZERO : receivedToday;
        return balance.add(received).subtract(spent);
    }
}