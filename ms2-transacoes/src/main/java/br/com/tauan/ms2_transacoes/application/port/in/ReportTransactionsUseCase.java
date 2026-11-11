package br.com.tauan.ms2_transacoes.application.port.in;

public interface ReportTransactionsUseCase {

    /**
     * Transações do usuário no período, da mais antiga para a mais recente.
     * from e to são opcionais: sem eles, vale o mês atual.
     */
    ReportResult execute(ListTransactionsQuery query);
}