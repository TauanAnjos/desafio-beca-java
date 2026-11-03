package br.com.tauan.ms1_usuarios.adapter.in.web.dto;

import br.com.tauan.ms1_usuarios.application.port.in.ImportUsersResult;

import java.util.List;

public record ImportUsersResponse(int imported, int skipped, List<RowErrorResponse> errors) {

    public record RowErrorResponse(int line, String reason) {
    }

    public static ImportUsersResponse from(ImportUsersResult result) {
        List<RowErrorResponse> errors = result.errors().stream()
                .map(error -> new RowErrorResponse(error.line(), error.reason()))
                .toList();
        return new ImportUsersResponse(result.imported(), errors.size(), errors);
    }
}