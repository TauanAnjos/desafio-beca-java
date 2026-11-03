package br.com.tauan.ms1_usuarios.application.port.in;

public record ImportUserRow(int line, String name, String email, String password) {
}