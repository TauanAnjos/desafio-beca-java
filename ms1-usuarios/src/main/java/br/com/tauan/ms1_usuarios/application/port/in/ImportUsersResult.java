package br.com.tauan.ms1_usuarios.application.port.in;

import java.util.List;

public record ImportUsersResult(int imported, List<RowError> errors) {

    public record RowError(int line, String reason) {
    }
}