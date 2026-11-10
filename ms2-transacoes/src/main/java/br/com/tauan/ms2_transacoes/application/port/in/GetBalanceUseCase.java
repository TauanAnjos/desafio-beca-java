package br.com.tauan.ms2_transacoes.application.port.in;

public interface GetBalanceUseCase {

    /**
     * @throws br.com.tauan.ms2_transacoes.domain.exception.AccountNotFoundException
     *         se o usuário não tem conta na API Mock
     * @throws br.com.tauan.ms2_transacoes.domain.exception.AccountServiceUnavailableException
     *         se a API Mock estiver fora do ar
     */
    BalanceView execute(Long userId);
}