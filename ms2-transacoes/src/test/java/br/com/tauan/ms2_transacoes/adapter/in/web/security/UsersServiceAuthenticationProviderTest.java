package br.com.tauan.ms2_transacoes.adapter.in.web.security;

import br.com.tauan.ms2_transacoes.application.port.out.UserAuthenticationPort;
import br.com.tauan.ms2_transacoes.domain.exception.UserServiceUnavailableException;
import br.com.tauan.ms2_transacoes.domain.model.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersServiceAuthenticationProviderTest {

    @Mock
    private UserAuthenticationPort userAuthentication;

    @InjectMocks
    private UsersServiceAuthenticationProvider provider;

    private Authentication credentials(String email, String password) {
        return UsernamePasswordAuthenticationToken.unauthenticated(email, password);
    }

    @Test
    void validCredentials_produceAnAuthenticatedTokenWithTheUserAsPrincipal() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "Ana Souza", "ana@exemplo.com");
        when(userAuthentication.authenticate("ana@exemplo.com", "Senha@1234")).thenReturn(Optional.of(user));

        Authentication result = provider.authenticate(credentials("ana@exemplo.com", "Senha@1234"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isEqualTo(user);
        assertThat(result.getCredentials()).isNull();
    }

    @Test
    void invalidCredentials_throwBadCredentials() {
        when(userAuthentication.authenticate("ana@exemplo.com", "errada")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.authenticate(credentials("ana@exemplo.com", "errada")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void whenTheUserServiceIsDown_throwAuthenticationServiceException() {
        when(userAuthentication.authenticate("ana@exemplo.com", "Senha@1234"))
                .thenThrow(new UserServiceUnavailableException("down"));

        assertThatThrownBy(() -> provider.authenticate(credentials("ana@exemplo.com", "Senha@1234")))
                .isInstanceOf(AuthenticationServiceException.class);
    }

    @Test
    void supportsOnlyUsernamePasswordTokens() {
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(Object.class)).isFalse();
    }
}