package br.com.tauan.ms2_transacoes.application.port.in;

import java.time.LocalDate;

/**
 * from e to são opcionais. Sem eles, vale o mês atual. O dia "to" entra por inteiro.
 */
public record ExpenseSummaryQuery(Long userId, LocalDate from, LocalDate to) {
}