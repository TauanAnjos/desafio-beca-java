package br.com.tauan.ms1_usuarios.application.port.in;

import java.util.List;

public interface ImportUsersUseCase {

    ImportUsersResult execute(List<ImportUserRow> rows);
}