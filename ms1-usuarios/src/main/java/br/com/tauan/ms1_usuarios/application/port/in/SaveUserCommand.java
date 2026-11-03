package br.com.tauan.ms1_usuarios.application.port.in;

public record SaveUserCommand(String name, String email) {
}