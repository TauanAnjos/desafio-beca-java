package br.com.tauan.ms2_transacoes.application.port.out;

import br.com.tauan.ms2_transacoes.domain.model.AccountInfo;

import java.util.Optional;

public interface AccountPort {

    /**
     * Busca saldo e limites do usuário no serviço externo.
     * Devolve vazio se o usuário não tem conta cadastrada.
     *
     * @throws br.com.tauan.ms2_transacoes.domain.exception.AccountServiceUnavailableException
     *         se o serviço externo falhar ou devolver dados incompletos
     */
    Optional<AccountInfo> findByUserId(Long userId);
}