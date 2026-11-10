package br.com.tauan.ms2_transacoes.application.port.out;

import br.com.tauan.ms2_transacoes.domain.model.AuthenticatedUser;

import java.util.Optional;

public interface UserAuthenticationPort {

    /**
     * Confere e-mail e senha no serviço de usuários. Devolve vazio se as credenciais forem inválidas.
     *
     * @throws br.com.tauan.ms2_transacoes.domain.exception.UserServiceUnavailableException
     *         se o serviço de usuários falhar ou devolver dados incompletos
     */
    Optional<AuthenticatedUser> authenticate(String email, String password);
}