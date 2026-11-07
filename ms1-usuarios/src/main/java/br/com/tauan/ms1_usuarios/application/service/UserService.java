package br.com.tauan.ms1_usuarios.application.service;

import br.com.tauan.ms1_usuarios.application.port.in.CreateUserUseCase;
import br.com.tauan.ms1_usuarios.application.port.in.DeleteUserUseCase;
import br.com.tauan.ms1_usuarios.application.port.in.GetUserUseCase;
import br.com.tauan.ms1_usuarios.application.port.in.ListUsersUseCase;
import br.com.tauan.ms1_usuarios.application.port.in.SaveUserCommand;
import br.com.tauan.ms1_usuarios.application.port.in.UpdateUserCommand;
import br.com.tauan.ms1_usuarios.application.port.in.UpdateUserUseCase;
import br.com.tauan.ms1_usuarios.application.port.out.PasswordEncoderPort;
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
    private final PasswordEncoderPort passwordEncoder;

    public UserService(UserRepositoryPort repository, PasswordEncoderPort passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User execute(SaveUserCommand command) {
        String email = normalize(command.email());
        if (repository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        String hash = passwordEncoder.encode(command.password());
        return repository.save(User.newUser(command.name().trim(), email, hash));
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
    public User execute(Long id, UpdateUserCommand command) {
        User user = find(id);
        String email = normalize(command.email());
        if (repository.existsByEmailAndIdNot(email, id)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        user.changeData(command.name().trim(), email);
        if (command.newPassword() != null && !command.newPassword().isBlank()) {
            user.changePasswordHash(passwordEncoder.encode(command.newPassword()));
        }
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