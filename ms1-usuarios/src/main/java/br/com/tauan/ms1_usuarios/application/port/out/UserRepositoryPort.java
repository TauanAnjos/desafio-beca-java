package br.com.tauan.ms1_usuarios.application.port.out;

import br.com.tauan.ms1_usuarios.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(Long id);

    List<User> findAll();

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    void deleteById(Long id);
}