package br.com.tauan.ms2_transacoes.application.port.in;

import br.com.tauan.ms2_transacoes.domain.model.ExpenseSummary;

import java.time.LocalDate;

public record ExpenseSummaryResult(LocalDate from, LocalDate to, ExpenseSummary summary) {
}