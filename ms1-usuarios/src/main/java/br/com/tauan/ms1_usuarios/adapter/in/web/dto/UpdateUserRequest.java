package br.com.tauan.ms1_usuarios.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Schema(example = "Ana Souza")
        @NotBlank @Size(max = 120) String name,

        @Schema(example = "ana.souza@exemplo.com")
        @NotBlank @Email @Size(max = 160) String email,

        @Schema(example = "NovaSenha@123",
                description = "Opcional. Se omitida, a senha atual é mantida. De 8 a 72 caracteres")
        @Size(min = 8, max = 72) String password
) {
}