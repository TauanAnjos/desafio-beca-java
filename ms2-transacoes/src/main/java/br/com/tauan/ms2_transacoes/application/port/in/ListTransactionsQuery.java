package br.com.tauan.ms2_transacoes.application.port.in;

import br.com.tauan.ms2_transacoes.domain.model.TransactionType;

import java.time.LocalDate;

/**
 * from, to e type são opcionais. O dia "to" entra por inteiro.
 */
public record ListTransactionsQuery(Long userId, LocalDate from, LocalDate to, TransactionType type) {
}