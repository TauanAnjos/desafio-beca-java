package br.com.tauan.ms2_transacoes.application.port.in;

import br.com.tauan.ms2_transacoes.domain.model.AccountInfo;

import java.math.BigDecimal;

public record BalanceView(AccountInfo account, BigDecimal spentToday, BigDecimal receivedToday,
                          BigDecimal availableBalance) {
}