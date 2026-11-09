package br.com.tauan.ms1_usuarios.adapter.in.web;

import br.com.tauan.ms1_usuarios.adapter.in.web.dto.LoginRequest;
import br.com.tauan.ms1_usuarios.adapter.in.web.dto.LoginResponse;
import br.com.tauan.ms1_usuarios.application.port.in.AuthenticateUserCommand;
import br.com.tauan.ms1_usuarios.application.port.in.AuthenticateUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Verificação de credenciais")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUser;
    private final InternalKeyGuard internalKeyGuard;

    public AuthController(AuthenticateUserUseCase authenticateUser, InternalKeyGuard internalKeyGuard) {
        this.authenticateUser = authenticateUser;
        this.internalKeyGuard = internalKeyGuard;
    }

    @PostMapping("/login")
    @Operation(summary = "Verifica e-mail e senha",
            description = "Rota interna, usada pelo MS2: exige o cabeçalho X-Internal-Key. "
                    + "E-mail inexistente e senha errada devolvem a mesma resposta 401.")
    @ApiResponse(responseCode = "200", description = "Credenciais válidas")
    @ApiResponse(responseCode = "400", description = "Corpo inválido (lista de campos em 'fields')",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Chave interna ausente ou inválida",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public LoginResponse login(
            @RequestHeader(value = "X-Internal-Key", required = false) String internalKey,
            @Valid @RequestBody LoginRequest request) {
        internalKeyGuard.verify(internalKey);
        return LoginResponse.from(authenticateUser.execute(
                new AuthenticateUserCommand(request.email(), request.password())));
    }
}