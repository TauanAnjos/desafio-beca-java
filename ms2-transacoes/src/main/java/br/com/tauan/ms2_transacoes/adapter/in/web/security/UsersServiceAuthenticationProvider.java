package br.com.tauan.ms2_transacoes.adapter.in.web.security;

import br.com.tauan.ms2_transacoes.application.port.out.UserAuthenticationPort;
import br.com.tauan.ms2_transacoes.domain.exception.UserServiceUnavailableException;
import br.com.tauan.ms2_transacoes.domain.model.AuthenticatedUser;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UsersServiceAuthenticationProvider implements AuthenticationProvider {

    private final UserAuthenticationPort userAuthentication;

    public UsersServiceAuthenticationProvider(UserAuthenticationPort userAuthentication) {
        this.userAuthentication = userAuthentication;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = String.valueOf(authentication.getCredentials());

        AuthenticatedUser user;
        try {
            user = userAuthentication.authenticate(email, password)
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        } catch (UserServiceUnavailableException e) {
            throw new AuthenticationServiceException("User service is unavailable", e);
        }
        return UsernamePasswordAuthenticationToken.authenticated(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}