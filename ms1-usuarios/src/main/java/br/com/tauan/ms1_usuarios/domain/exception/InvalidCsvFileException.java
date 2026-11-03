package br.com.tauan.ms1_usuarios.domain.exception;

public class InvalidCsvFileException extends RuntimeException {

    public InvalidCsvFileException(String message) {
        super(message);
    }
}