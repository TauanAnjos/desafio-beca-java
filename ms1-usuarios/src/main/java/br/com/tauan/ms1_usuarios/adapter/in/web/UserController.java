package br.com.tauan.ms1_usuarios.adapter.in.web;

import br.com.tauan.ms1_usuarios.adapter.in.web.dto.UpdateUserRequest;
import br.com.tauan.ms1_usuarios.adapter.in.web.dto.UserRequest;
import br.com.tauan.ms1_usuarios.adapter.in.web.dto.UserResponse;
import br.com.tauan.ms1_usuarios.application.port.in.CreateUserUseCase;
import br.com.tauan.ms1_usuarios.application.port.in.DeleteUserUseCase;
import br.com.tauan.ms1_usuarios.application.port.in.GetUserUseCase;
import br.com.tauan.ms1_usuarios.application.port.in.ListUsersUseCase;
import br.com.tauan.ms1_usuarios.application.port.in.SaveUserCommand;
import br.com.tauan.ms1_usuarios.application.port.in.UpdateUserCommand;
import br.com.tauan.ms1_usuarios.application.port.in.UpdateUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "CRUD de usuários")
public class UserController {

    private final CreateUserUseCase createUser;
    private final ListUsersUseCase listUsers;
    private final GetUserUseCase getUser;
    private final UpdateUserUseCase updateUser;
    private final DeleteUserUseCase deleteUser;

    public UserController(CreateUserUseCase createUser, ListUsersUseCase listUsers,
                          GetUserUseCase getUser, UpdateUserUseCase updateUser,
                          DeleteUserUseCase deleteUser) {
        this.createUser = createUser;
        this.listUsers = listUsers;
        this.getUser = getUser;
        this.updateUser = updateUser;
        this.deleteUser = deleteUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um usuário",
            description = "A senha é guardada com BCrypt e nunca é devolvida nas respostas.")
    @ApiResponse(responseCode = "201", description = "Usuário criado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos (lista de campos em 'fields')",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return UserResponse.from(createUser.execute(
                new SaveUserCommand(request.name(), request.email(), request.password())));
    }

    @GetMapping
    @Operation(summary = "Lista todos os usuários")
    @ApiResponse(responseCode = "200", description = "Lista de usuários")
    public List<UserResponse> list() {
        return listUsers.execute().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um usuário pelo id")
    @ApiResponse(responseCode = "200", description = "Usuário encontrado")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public UserResponse get(@PathVariable Long id) {
        return UserResponse.from(getUser.execute(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um usuário",
            description = "Substitui nome e e-mail. A senha é opcional: só é trocada (e recriptografada "
                    + "com BCrypt) se vier no corpo.")
    @ApiResponse(responseCode = "200", description = "Usuário atualizado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos (lista de campos em 'fields')",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "E-mail já usado por outro usuário",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(updateUser.execute(id,
                new UpdateUserCommand(request.name(), request.email(), request.password())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um usuário")
    @ApiResponse(responseCode = "204", description = "Usuário excluído")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public void delete(@PathVariable Long id) {
        deleteUser.deleteById(id);
    }
}