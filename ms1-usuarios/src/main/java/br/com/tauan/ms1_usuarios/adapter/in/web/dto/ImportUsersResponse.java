package br.com.tauan.ms1_usuarios.adapter.in.web.dto;

import br.com.tauan.ms1_usuarios.application.port.in.ImportUsersResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ImportUsersResponse(
        @Schema(example = "4") int imported,
        @Schema(example = "1") int skipped,
        List<RowErrorResponse> errors
) {

    public record RowErrorResponse(
            @Schema(example = "3") int line,
            @Schema(example = "invalid email") String reason
    ) {
    }

    public static ImportUsersResponse from(ImportUsersResult result) {
        List<RowErrorResponse> errors = result.errors().stream()
                .map(error -> new RowErrorResponse(error.line(), error.reason()))
                .toList();
        return new ImportUsersResponse(result.imported(), errors.size(), errors);
    }
}