package br.com.tauan.ms1_usuarios.application.port.out;

public interface PasswordEncoderPort {

    String encode(String rawPassword);
}