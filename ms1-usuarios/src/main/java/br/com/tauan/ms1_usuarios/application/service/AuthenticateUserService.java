package br.com.tauan.ms1_usuarios.application.service;

import br.com.tauan.ms1_usuarios.application.port.in.AuthenticateUserCommand;
import br.com.tauan.ms1_usuarios.application.port.in.AuthenticateUserUseCase;
import br.com.tauan.ms1_usuarios.application.port.out.PasswordEncoderPort;
import br.com.tauan.ms1_usuarios.application.port.out.UserRepositoryPort;
import br.com.tauan.ms1_usuarios.domain.exception.InvalidCredentialsException;
import br.com.tauan.ms1_usuarios.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private final UserRepositoryPort repository;
    private final PasswordEncoderPort passwordEncoder;

    /**
     * Hash descartável: dá ao e-mail inexistente o mesmo custo do BCrypt de um e-mail existente,
     * para o tempo de resposta não revelar quais e-mails estão cadastrados.
     */
    private final String timingProtectionHash;

    public AuthenticateUserService(UserRepositoryPort repository, PasswordEncoderPort passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.timingProtectionHash = passwordEncoder.encode("timing-protection-hash");
    }

    @Override
    @Transactional(readOnly = true)
    public User execute(AuthenticateUserCommand command) {
        String email = command.email().trim().toLowerCase();
        Optional<User> found = repository.findByEmail(email);

        if (found.isEmpty()) {
            passwordEncoder.matches(command.password(), timingProtectionHash);
            throw new InvalidCredentialsException();
        }

        User user = found.get();
        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }
}