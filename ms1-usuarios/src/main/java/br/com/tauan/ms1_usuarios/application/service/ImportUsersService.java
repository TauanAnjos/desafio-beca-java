package br.com.tauan.ms1_usuarios.application.service;

import br.com.tauan.ms1_usuarios.application.port.in.ImportUserRow;
import br.com.tauan.ms1_usuarios.application.port.in.ImportUsersResult;
import br.com.tauan.ms1_usuarios.application.port.in.ImportUsersUseCase;
import br.com.tauan.ms1_usuarios.application.port.out.UserRepositoryPort;
import br.com.tauan.ms1_usuarios.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ImportUsersService implements ImportUsersUseCase {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepositoryPort repository;

    public ImportUsersService(UserRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ImportUsersResult execute(List<ImportUserRow> rows) {
        Set<String> emailsInFile = new HashSet<>();
        List<ImportUsersResult.RowError> errors = new ArrayList<>();
        int imported = 0;

        for (ImportUserRow row : rows) {
            String name = row.name().trim();
            String email = row.email().trim().toLowerCase();

            String reason = validate(name, email, emailsInFile);
            if (reason != null) {
                errors.add(new ImportUsersResult.RowError(row.line(), reason));
                continue;
            }

            emailsInFile.add(email);
            repository.save(User.newUser(name, email));
            imported++;
        }
        return new ImportUsersResult(imported, errors);
    }

    private String validate(String name, String email, Set<String> emailsInFile) {
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
        if (emailsInFile.contains(email)) {
            return "duplicated email in the file";
        }
        if (repository.existsByEmail(email)) {
            return "email already registered";
        }
        return null;
    }
}