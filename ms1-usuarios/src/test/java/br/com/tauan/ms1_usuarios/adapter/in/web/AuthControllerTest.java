package br.com.tauan.ms1_usuarios.adapter.in.web;

import br.com.tauan.ms1_usuarios.application.port.in.AuthenticateUserCommand;
import br.com.tauan.ms1_usuarios.application.port.in.AuthenticateUserUseCase;
import br.com.tauan.ms1_usuarios.domain.exception.InvalidCredentialsException;
import br.com.tauan.ms1_usuarios.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String KEY = "chave-de-teste";
    private static final String BODY = """
            {"email":"ana@exemplo.com","password":"Senha@1234"}
            """;

    @Mock
    private AuthenticateUserUseCase authenticateUser;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authenticateUser, new InternalKeyGuard(KEY)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_withTheInternalKey_returns200WithoutAnyPassword() throws Exception {
        when(authenticateUser.execute(any(AuthenticateUserCommand.class)))
                .thenReturn(new User(1L, "Ana Souza", "ana@exemplo.com", "hash", LocalDateTime.of(2026, 11, 1, 10, 0)));

        mockMvc.perform(post("/auth/login")
                        .header("X-Internal-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void login_withoutTheKey_returns403_andDoesNotCheckAnyPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(authenticateUser);
    }

    @Test
    void login_withAWrongKey_returns403_andDoesNotCheckAnyPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .header("X-Internal-Key", "chave-errada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(authenticateUser);
    }

    @Test
    void login_withInvalidCredentials_returns401() throws Exception {
        when(authenticateUser.execute(any(AuthenticateUserCommand.class)))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/login")
                        .header("X-Internal-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));
    }

    @Test
    void login_withAnInvalidBody_returns400ListingTheFields() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .header("X-Internal-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("email")));
    }
}