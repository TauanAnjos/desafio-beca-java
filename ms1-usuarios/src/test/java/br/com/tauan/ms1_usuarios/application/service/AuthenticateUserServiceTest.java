package br.com.tauan.ms1_usuarios.application.service;

import br.com.tauan.ms1_usuarios.application.port.in.AuthenticateUserCommand;
import br.com.tauan.ms1_usuarios.application.port.out.PasswordEncoderPort;
import br.com.tauan.ms1_usuarios.application.port.out.UserRepositoryPort;
import br.com.tauan.ms1_usuarios.domain.exception.InvalidCredentialsException;
import br.com.tauan.ms1_usuarios.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

    @Mock
    private UserRepositoryPort repository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    private AuthenticateUserService service;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-hash");
        service = new AuthenticateUserService(repository, passwordEncoder);
    }

    private User user() {
        return new User(1L, "Ana Souza", "ana@exemplo.com", "hash", LocalDateTime.of(2026, 11, 1, 10, 0));
    }

    @Test
    void validCredentials_returnTheUser_andTheEmailIsNormalized() {
        User user = user();
        when(repository.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Senha@1234", "hash")).thenReturn(true);

        User result = service.execute(new AuthenticateUserCommand("  ANA@Exemplo.com ", "Senha@1234"));

        assertThat(result).isSameAs(user);
        verify(passwordEncoder, never()).matches("Senha@1234", "dummy-hash");
    }

    @Test
    void unknownEmail_stillRunsThePasswordCheck_soTheTimingDoesNotRevealTheEmail() {
        when(repository.findByEmail("ninguem@exemplo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new AuthenticateUserCommand("ninguem@exemplo.com", "Senha@1234")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(passwordEncoder).matches("Senha@1234", "dummy-hash");
    }

    @Test
    void wrongPassword_throwsTheSameInvalidCredentials() {
        when(repository.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("errada123", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.execute(new AuthenticateUserCommand("ana@exemplo.com", "errada123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }
}