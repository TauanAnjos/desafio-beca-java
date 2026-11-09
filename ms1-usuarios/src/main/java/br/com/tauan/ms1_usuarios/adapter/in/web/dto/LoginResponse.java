package br.com.tauan.ms1_usuarios.adapter.in.web.dto;

import br.com.tauan.ms1_usuarios.domain.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Ana Souza") String name,
        @Schema(example = "ana.souza@exemplo.com") String email
) {

    public static LoginResponse from(User user) {
        return new LoginResponse(user.getId(), user.getName(), user.getEmail());
    }
}