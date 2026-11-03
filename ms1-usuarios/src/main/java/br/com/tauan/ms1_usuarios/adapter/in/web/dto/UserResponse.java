package br.com.tauan.ms1_usuarios.adapter.in.web.dto;

import br.com.tauan.ms1_usuarios.domain.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record UserResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Ana Souza") String name,
        @Schema(example = "ana.souza@exemplo.com") String email,
        @Schema(example = "2026-10-30T10:15:30") LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}