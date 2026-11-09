package br.com.tauan.ms2_transacoes.adapter.out.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UsersServiceLoginResponse(Long id, String name, String email) {
}