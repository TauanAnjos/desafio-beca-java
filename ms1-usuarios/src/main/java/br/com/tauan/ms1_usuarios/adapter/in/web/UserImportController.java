package br.com.tauan.ms1_usuarios.adapter.in.web;

import br.com.tauan.ms1_usuarios.adapter.in.web.dto.ImportUsersResponse;
import br.com.tauan.ms1_usuarios.application.port.in.ImportUserRow;
import br.com.tauan.ms1_usuarios.application.port.in.ImportUsersUseCase;
import br.com.tauan.ms1_usuarios.domain.exception.InvalidCsvFileException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
@Tag(name = "Users import", description = "Cadastro em massa por CSV")
public class UserImportController {

    private final ImportUsersUseCase importUsers;

    public UserImportController(ImportUsersUseCase importUsers) {
        this.importUsers = importUsers;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importa usuários de um arquivo CSV",
            description = "Cabeçalho obrigatório: name,email,password (vírgula ou ponto e vírgula). "
                    + "Linhas inválidas não derrubam o arquivo: voltam em 'errors' com a linha e o motivo.")
    @ApiResponse(responseCode = "200", description = "Resumo da importação")
    @ApiResponse(responseCode = "400", description = "Arquivo vazio, ilegível ou com cabeçalho inválido",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ImportUsersResponse importCsv(
            @Parameter(description = "Arquivo CSV com cabeçalho name,email,password")
            @RequestParam("file") MultipartFile file) {
        List<ImportUserRow> rows;
        try (InputStream input = file.getInputStream()) {
            rows = CsvUserParser.parse(input);
        } catch (IOException e) {
            throw new InvalidCsvFileException("Could not read the uploaded file");
        }
        return ImportUsersResponse.from(importUsers.execute(rows));
    }
}