package br.com.tauan.ms2_transacoes.domain.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long userId) {
        super("Account not found for the user: " + userId);
    }
}