package br.com.tauan.ms1_usuarios.application.port.in;

public record UpdateUserCommand(String name, String email, String newPassword) {
}