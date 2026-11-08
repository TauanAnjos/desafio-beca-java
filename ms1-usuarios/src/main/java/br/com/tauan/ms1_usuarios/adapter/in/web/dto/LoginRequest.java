package br.com.tauan.ms1_usuarios.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "ana.souza@exemplo.com")
        @NotBlank String email,

        @Schema(example = "Senha@1234")
        @NotBlank String password
) {
}