package br.com.tauan.ms1_usuarios.application.port.in;

import br.com.tauan.ms1_usuarios.domain.model.User;

public interface AuthenticateUserUseCase {

    /**
     * @throws br.com.tauan.ms1_usuarios.domain.exception.InvalidCredentialsException
     *         para e-mail inexistente ou senha errada (a mesma resposta nos dois casos)
     */
    User execute(AuthenticateUserCommand command);
}