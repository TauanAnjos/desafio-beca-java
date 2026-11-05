package br.com.tauan.ms2_transacoes.application.port.in;

import br.com.tauan.ms2_transacoes.domain.model.Transaction;

public interface GetTransactionUseCase {

    Transaction execute(Long userId, Long transactionId);
}