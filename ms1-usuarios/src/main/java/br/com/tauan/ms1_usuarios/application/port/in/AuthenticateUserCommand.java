package br.com.tauan.ms1_usuarios.application.port.in;

public record AuthenticateUserCommand(String email, String password) {
}