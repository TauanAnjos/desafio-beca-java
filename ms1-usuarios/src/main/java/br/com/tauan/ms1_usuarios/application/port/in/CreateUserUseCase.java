package br.com.tauan.ms1_usuarios.application.port.in;

import br.com.tauan.ms1_usuarios.domain.model.User;

public interface CreateUserUseCase {

    User execute(SaveUserCommand command);
}