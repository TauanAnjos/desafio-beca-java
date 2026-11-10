package br.com.tauan.ms1_usuarios.adapter.in.web;

public class InvalidInternalKeyException extends RuntimeException {

    public InvalidInternalKeyException() {
        super("Forbidden");
    }
}