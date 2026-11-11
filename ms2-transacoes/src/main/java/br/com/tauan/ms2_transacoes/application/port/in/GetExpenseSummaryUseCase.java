package br.com.tauan.ms2_transacoes.application.port.in;

public interface GetExpenseSummaryUseCase {

    ExpenseSummaryResult execute(ExpenseSummaryQuery query);
}