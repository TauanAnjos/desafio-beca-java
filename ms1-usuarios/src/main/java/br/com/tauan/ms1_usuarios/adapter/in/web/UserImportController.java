package br.com.tauan.ms1_usuarios.adapter.in.web;

import br.com.tauan.ms1_usuarios.adapter.in.web.dto.ImportUsersResponse;
import br.com.tauan.ms1_usuarios.application.port.in.ImportUserRow;
import br.com.tauan.ms1_usuarios.application.port.in.ImportUsersUseCase;
import br.com.tauan.ms1_usuarios.domain.exception.InvalidCsvFileException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/users/import")
public class UserImportController {

    private final ImportUsersUseCase importUsers;

    public UserImportController(ImportUsersUseCase importUsers) {
        this.importUsers = importUsers;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportUsersResponse importCsv(@RequestParam("file") MultipartFile file) {
        List<ImportUserRow> rows;
        try (InputStream input = file.getInputStream()) {
            rows = CsvUserParser.parse(input);
        } catch (IOException e) {
            throw new InvalidCsvFileException("Could not read the uploaded file");
        }
        return ImportUsersResponse.from(importUsers.execute(rows));
    }
}