package br.com.tauan.ms2_transacoes.domain.exception;

public class UserServiceUnavailableException extends RuntimeException {
  public UserServiceUnavailableException(String message) {
    super(message);
  }
}
