package br.com.tauan.ms1_usuarios.application.service;

import br.com.tauan.ms1_usuarios.application.port.in.CreateUserUseCase;
import br.com.tauan.ms1_usuarios.application.port.in.DeleteUserUseCase;
import br.com.tauan.ms1_usuarios.application.port.in.GetUserUseCase;
import br.com.tauan.ms1_usuarios.application.port.in.ListUsersUseCase;
import br.com.tauan.ms1_usuarios.application.port.in.SaveUserCommand;
import br.com.tauan.ms1_usuarios.application.port.in.UpdateUserUseCase;
import br.com.tauan.ms1_usuarios.application.port.out.UserRepositoryPort;
import br.com.tauan.ms1_usuarios.domain.exception.EmailAlreadyRegisteredException;
import br.com.tauan.ms1_usuarios.domain.exception.UserNotFoundException;
import br.com.tauan.ms1_usuarios.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService implements CreateUserUseCase, ListUsersUseCase, GetUserUseCase,
        UpdateUserUseCase, DeleteUserUseCase {

    private final UserRepositoryPort repository;

    public UserService(UserRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public User execute(SaveUserCommand command) {
        String email = normalize(command.email());
        if (repository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        return repository.save(User.newUser(command.name().trim(), email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> execute() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public User execute(Long id) {
        return find(id);
    }

    @Override
    @Transactional
    public User execute(Long id, SaveUserCommand command) {
        User user = find(id);
        String email = normalize(command.email());
        if (repository.existsByEmailAndIdNot(email, id)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        user.changeData(command.name().trim(), email);
        return repository.save(user);
    }
    @Override
    @Transactional
    public void deleteById(Long id) {
        find(id);
        repository.deleteById(id);
    }
    private User find(Long id) {
        return repository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}