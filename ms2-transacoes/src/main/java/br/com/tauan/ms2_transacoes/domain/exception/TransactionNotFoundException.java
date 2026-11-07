package br.com.tauan.ms2_transacoes.domain.exception;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(Long id) {
        super("Transaction not found: " + id);
    }
}