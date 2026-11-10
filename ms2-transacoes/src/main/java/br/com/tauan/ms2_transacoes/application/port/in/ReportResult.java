package br.com.tauan.ms2_transacoes.application.port.in;

import br.com.tauan.ms2_transacoes.domain.model.Transaction;

import java.time.LocalDate;
import java.util.List;

public record ReportResult(LocalDate from, LocalDate to, List<Transaction> transactions) {
}