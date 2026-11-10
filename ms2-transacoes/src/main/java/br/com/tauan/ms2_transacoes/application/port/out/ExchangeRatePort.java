package br.com.tauan.ms2_transacoes.application.port.out;

import br.com.tauan.ms2_transacoes.domain.model.ExchangeRate;

import java.util.Optional;

public interface ExchangeRatePort {

    /**
     * Cotação do real na moeda, no dia útil mais recente disponível.
     * Devolve vazio se não estiver disponível (serviço fora do ar, lento ou sem cotação).
     * Não lança exceção: a criação da transação nunca depende desta consulta.
     */
    Optional<ExchangeRate> findLatest(String currency);
}