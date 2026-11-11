package br.com.tauan.ms2_transacoes.adapter.in.web;

import br.com.tauan.ms2_transacoes.domain.model.AuthenticatedUser;
import org.springframework.security.core.Authentication;

final class CurrentUser {

    private CurrentUser() {
    }

    static AuthenticatedUser from(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}