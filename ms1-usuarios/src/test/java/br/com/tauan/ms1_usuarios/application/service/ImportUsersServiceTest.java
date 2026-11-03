package br.com.tauan.ms1_usuarios.application.service;

import br.com.tauan.ms1_usuarios.application.port.in.ImportUserRow;
import br.com.tauan.ms1_usuarios.application.port.in.ImportUsersResult;
import br.com.tauan.ms1_usuarios.application.port.out.PasswordEncoderPort;
import br.com.tauan.ms1_usuarios.application.port.out.UserRepositoryPort;
import br.com.tauan.ms1_usuarios.domain.exception.EmailAlreadyRegisteredException;
import br.com.tauan.ms1_usuarios.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportUsersServiceTest {

    @Mock
    private UserRepositoryPort repository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @InjectMocks
    private ImportUsersService service;

    private ImportUserRow row(int line, String name, String email, String password) {
        return new ImportUserRow(line, name, email, password);
    }

    @Test
    void validRows_areAllImported() {
        when(repository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Senha@1234")).thenReturn("hash");

        ImportUsersResult result = service.execute(List.of(
                row(2, "Ana", "ana@exemplo.com", "Senha@1234"),
                row(3, "Bruno", "bruno@exemplo.com", "Senha@1234")));

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.errors()).isEmpty();
        verify(repository, times(2)).save(any(User.class));
    }

    @Test
    void row_isNormalizedAndPasswordIsHashed() {
        when(repository.existsByEmail("ana@exemplo.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha@1234")).thenReturn("hash");

        service.execute(List.of(row(2, "  Ana Souza ", "  ANA@Exemplo.com ", "Senha@1234")));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Ana Souza");
        assertThat(captor.getValue().getEmail()).isEqualTo("ana@exemplo.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash");
    }

    @ParameterizedTest
    @CsvSource({
            "'',ana@exemplo.com,Senha@1234,name is required",
            "Ana,email-invalido,Senha@1234,invalid email",
            "Ana,'',Senha@1234,invalid email",
            "Ana,ana@exemplo.com,curta,password must have at least 8 characters"
    })
    void invalidRow_isReportedAndNotSaved(String name, String email, String password, String reason) {
        ImportUsersResult result = service.execute(List.of(row(2, name, email, password)));

        assertThat(result.imported()).isZero();
        assertThat(result.errors())
                .extracting(ImportUsersResult.RowError::line, ImportUsersResult.RowError::reason)
                .containsExactly(tuple(2, reason));
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void passwordOver72Bytes_isRejected() {
        ImportUsersResult result = service.execute(List.of(
                row(2, "Ana", "ana@exemplo.com", "a".repeat(73))));

        assertThat(result.errors())
                .extracting(ImportUsersResult.RowError::reason)
                .containsExactly("password must have at most 72 bytes");
    }

    @Test
    void duplicatedEmailInsideFile_failsOnlyTheSecondRow() {
        when(repository.existsByEmail("ana@exemplo.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha@1234")).thenReturn("hash");

        ImportUsersResult result = service.execute(List.of(
                row(2, "Ana", "ana@exemplo.com", "Senha@1234"),
                row(3, "Ana Clone", "ANA@exemplo.com", "Senha@1234")));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.errors())
                .extracting(ImportUsersResult.RowError::line, ImportUsersResult.RowError::reason)
                .containsExactly(tuple(3, "duplicated email in the file"));
        verify(repository, times(1)).save(any(User.class));
    }

    @Test
    void emailAlreadyRegistered_isReportedAndNotSaved() {
        when(repository.existsByEmail("ana@exemplo.com")).thenReturn(true);

        ImportUsersResult result = service.execute(List.of(row(2, "Ana", "ana@exemplo.com", "Senha@1234")));

        assertThat(result.imported()).isZero();
        assertThat(result.errors())
                .extracting(ImportUsersResult.RowError::reason)
                .containsExactly("email already registered");
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void invalidRowInTheMiddle_doesNotStopTheOthers() {
        when(repository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Senha@1234")).thenReturn("hash");

        ImportUsersResult result = service.execute(List.of(
                row(2, "Ana", "ana@exemplo.com", "Senha@1234"),
                row(3, "Bruno", "email-invalido", "Senha@1234"),
                row(4, "Carla", "carla@exemplo.com", "Senha@1234")));

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.errors())
                .extracting(ImportUsersResult.RowError::line, ImportUsersResult.RowError::reason)
                .containsExactly(tuple(3, "invalid email"));
    }

    @Test
    void conflictOnSave_isReportedAndTheNextRowIsStillImported() {
        when(repository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(repository.save(any(User.class)))
                .thenThrow(new EmailAlreadyRegisteredException("ana@exemplo.com"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ImportUsersResult result = service.execute(List.of(
                row(2, "Ana", "ana@exemplo.com", "Senha@1234"),
                row(3, "Bruno", "bruno@exemplo.com", "Senha@1234")));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.errors())
                .extracting(ImportUsersResult.RowError::line, ImportUsersResult.RowError::reason)
                .containsExactly(tuple(2, "email already registered"));
    }
}