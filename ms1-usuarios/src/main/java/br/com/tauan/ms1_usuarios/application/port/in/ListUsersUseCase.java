package br.com.tauan.ms1_usuarios.application.port.in;

import br.com.tauan.ms1_usuarios.domain.model.User;

import java.util.List;

public interface ListUsersUseCase {

    List<User> execute();
}