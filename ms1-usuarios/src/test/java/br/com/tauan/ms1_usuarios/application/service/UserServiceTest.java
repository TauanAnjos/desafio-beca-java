package br.com.tauan.ms1_usuarios.application.service;

import br.com.tauan.ms1_usuarios.application.port.in.SaveUserCommand;
import br.com.tauan.ms1_usuarios.application.port.out.PasswordEncoderPort;
import br.com.tauan.ms1_usuarios.application.port.out.UserRepositoryPort;
import br.com.tauan.ms1_usuarios.domain.exception.EmailAlreadyRegisteredException;
import br.com.tauan.ms1_usuarios.domain.exception.UserNotFoundException;
import br.com.tauan.ms1_usuarios.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepositoryPort repository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @InjectMocks
    private UserService service;

    private User existingUser() {
        return new User(1L, "Old Name", "old@exemplo.com", "oldHash", LocalDateTime.of(2026, 10, 30, 10, 0));
    }

    @Test
    void create_savesUserWithNormalizedEmailAndHashedPassword() {
        when(repository.existsByEmail("ana@exemplo.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha@1234")).thenReturn("hash");
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.execute(new SaveUserCommand("  Ana Souza ", "  ANA@Exemplo.com ", "Senha@1234"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Ana Souza");
        assertThat(captor.getValue().getEmail()).isEqualTo("ana@exemplo.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(result.getPasswordHash()).isNotEqualTo("Senha@1234");
    }

    @Test
    void create_withExistingEmail_throwsAndDoesNotSave() {
        when(repository.existsByEmail("ana@exemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> service.execute(new SaveUserCommand("Ana", "ana@exemplo.com", "Senha@1234")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(repository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void list_returnsAllUsers() {
        User user = existingUser();
        when(repository.findAll()).thenReturn(List.of(user));

        assertThat(service.execute()).containsExactly(user);
    }

    @Test
    void get_returnsUser() {
        User user = existingUser();
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(service.execute(1L)).isSameAs(user);
    }

    @Test
    void get_whenNotFound_throws() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(99L)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void update_changesDataAndRehashesPassword() {
        User user = existingUser();
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.existsByEmailAndIdNot("novo@exemplo.com", 1L)).thenReturn(false);
        when(passwordEncoder.encode("NovaSenha1")).thenReturn("newHash");
        when(repository.save(user)).thenReturn(user);

        User result = service.execute(1L, new SaveUserCommand(" Novo Nome ", "NOVO@Exemplo.com", "NovaSenha1"));

        assertThat(result.getName()).isEqualTo("Novo Nome");
        assertThat(result.getEmail()).isEqualTo("novo@exemplo.com");
        assertThat(result.getPasswordHash()).isEqualTo("newHash");
    }

    @Test
    void update_withEmailOfAnotherUser_throwsAndDoesNotSave() {
        when(repository.findById(1L)).thenReturn(Optional.of(existingUser()));
        when(repository.existsByEmailAndIdNot("outro@exemplo.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(1L, new SaveUserCommand("Nome", "outro@exemplo.com", "Senha@1234")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(repository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void update_whenNotFound_throws() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(99L, new SaveUserCommand("Nome", "a@exemplo.com", "Senha@1234")))
                .isInstanceOf(UserNotFoundException.class);

        verify(repository, never()).save(any(User.class));
    }

    @Test
    void delete_removesExistingUser() {
        when(repository.findById(1L)).thenReturn(Optional.of(existingUser()));

        service.deleteById(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_whenNotFound_throwsAndDoesNotDelete() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(99L)).isInstanceOf(UserNotFoundException.class);

        verify(repository, never()).deleteById(99L);
    }
}