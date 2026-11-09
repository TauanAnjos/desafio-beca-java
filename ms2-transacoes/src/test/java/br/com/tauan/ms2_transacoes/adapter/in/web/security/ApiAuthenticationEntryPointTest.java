package br.com.tauan.ms2_transacoes.adapter.in.web.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiAuthenticationEntryPointTest {

    private final ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint();

    @Test
    void badCredentials_return401WithTheBasicChallenge() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response, new BadCredentialsException("x"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).startsWith("Basic");
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString()).contains("\"status\":401");
    }

    @Test
    void whenTheUserServiceFails_return503WithoutTheChallenge() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response, new AuthenticationServiceException("down"));

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("WWW-Authenticate")).isNull();
        assertThat(response.getContentAsString()).contains("\"status\":503");
    }
}