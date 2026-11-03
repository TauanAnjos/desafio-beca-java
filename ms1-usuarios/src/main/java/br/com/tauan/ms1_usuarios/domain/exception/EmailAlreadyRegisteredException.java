package br.com.tauan.ms1_usuarios.domain.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

  public EmailAlreadyRegisteredException(String email) {
    super("Email already registered: " + email);
  }
}