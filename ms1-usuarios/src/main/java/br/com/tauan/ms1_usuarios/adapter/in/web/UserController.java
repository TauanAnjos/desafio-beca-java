package br.com.tauan.ms1_usuarios.adapter.in.web;

import br.com.tauan.ms1_usuarios.adapter.in.web.dto.UserRequest;
import br.com.tauan.ms1_usuarios.adapter.in.web.dto.UserResponse;
import br.com.tauan.ms1_usuarios.application.port.in.CreateUserUseCase;
import br.com.tauan.ms1_usuarios.application.port.in.DeleteUserUseCase;
import br.com.tauan.ms1_usuarios.application.port.in.GetUserUseCase;
import br.com.tauan.ms1_usuarios.application.port.in.ListUsersUseCase;
import br.com.tauan.ms1_usuarios.application.port.in.SaveUserCommand;
import br.com.tauan.ms1_usuarios.application.port.in.UpdateUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return UserResponse.from(createUser.execute(new SaveUserCommand(request.name(), request.email())));
    }

    @GetMapping
    public List<UserResponse> list() {
        return listUsers.execute().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return UserResponse.from(getUser.execute(id));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return UserResponse.from(updateUser.execute(id, new SaveUserCommand(request.name(), request.email())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUser.deleteById(id);
    }
}