package br.com.tauan.ms2_transacoes.application.port.in;

import br.com.tauan.ms2_transacoes.domain.model.Transaction;

import java.util.List;

public interface ListTransactionsUseCase {

    List<Transaction> execute(ListTransactionsQuery query);
}