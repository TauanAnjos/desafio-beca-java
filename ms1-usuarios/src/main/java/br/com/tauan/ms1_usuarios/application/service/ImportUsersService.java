package br.com.tauan.ms1_usuarios.application.service;

import br.com.tauan.ms1_usuarios.application.port.in.ImportUserRow;
import br.com.tauan.ms1_usuarios.application.port.in.ImportUsersResult;
import br.com.tauan.ms1_usuarios.application.port.in.ImportUsersUseCase;
import br.com.tauan.ms1_usuarios.application.port.out.PasswordEncoderPort;
import br.com.tauan.ms1_usuarios.application.port.out.UserRepositoryPort;
import br.com.tauan.ms1_usuarios.domain.exception.EmailAlreadyRegisteredException;
import br.com.tauan.ms1_usuarios.domain.model.User;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ImportUsersService implements ImportUsersUseCase {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepositoryPort repository;
    private final PasswordEncoderPort passwordEncoder;

    public ImportUsersService(UserRepositoryPort repository, PasswordEncoderPort passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public ImportUsersResult execute(List<ImportUserRow> rows) {
        Set<String> emailsInFile = new HashSet<>();
        List<ImportUsersResult.RowError> errors = new ArrayList<>();
        int imported = 0;

        for (ImportUserRow row : rows) {
            String name = row.name() == null ? "" : row.name().trim();
            String email = row.email() == null ? "" : row.email().trim().toLowerCase();
            String password = row.password() == null ? "" : row.password();

            String reason = validate(name, email, password, emailsInFile);
            if (reason != null) {
                errors.add(new ImportUsersResult.RowError(row.line(), reason));
                continue;
            }

            try {
                repository.save(User.newUser(name, email, passwordEncoder.encode(password)));
                emailsInFile.add(email);
                imported++;
            } catch (EmailAlreadyRegisteredException e) {
                errors.add(new ImportUsersResult.RowError(row.line(), "email already registered"));
            }
        }
        return new ImportUsersResult(imported, errors);
    }

    private String validate(String name, String email, String password, Set<String> emailsInFile) {
        if (name.isBlank()) {
            return "name is required";
        }
        if (name.length() > 120) {
            return "name must have at most 120 characters";
        }
        if (email.isBlank() || !EMAIL.matcher(email).matches()) {
            return "invalid email";
        }
        if (email.length() > 160) {
            return "email must have at most 160 characters";
        }
        if (password.length() < 8) {
            return "password must have at least 8 characters";
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return "password must have at most 72 bytes";
        }
        if (emailsInFile.contains(email)) {
            return "duplicated email in the file";
        }
        if (repository.existsByEmail(email)) {
            return "email already registered";
        }
        return null;
    }
}