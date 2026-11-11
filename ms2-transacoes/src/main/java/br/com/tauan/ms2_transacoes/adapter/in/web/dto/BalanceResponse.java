package br.com.tauan.ms2_transacoes.adapter.in.web.dto;

import br.com.tauan.ms2_transacoes.application.port.in.BalanceView;

import java.math.BigDecimal;

/**
 * "balance" é o valor que está na API Mock naquele momento. "availableBalance" já considera
 * os depósitos e as despesas aprovadas do dia atual.
 */
public record BalanceResponse(
        Long userId,
        BigDecimal balance,
        BigDecimal dailyLimit,
        BigDecimal perTransactionLimit,
        BigDecimal spentToday,
        BigDecimal receivedToday,
        BigDecimal availableBalance
) {

    public static BalanceResponse from(BalanceView view) {
        return new BalanceResponse(
                view.account().userId(),
                view.account().balance(),
                view.account().dailyLimit(),
                view.account().perTransactionLimit(),
                view.spentToday(),
                view.receivedToday(),
                view.availableBalance());
    }
}