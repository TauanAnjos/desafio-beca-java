package br.com.tauan.ms1_usuarios.adapter.in.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalKeyGuardTest {

    private final InternalKeyGuard guard = new InternalKeyGuard("chave-de-teste");

    @Test
    void correctKey_passes() {
        assertThatCode(() -> guard.verify("chave-de-teste")).doesNotThrowAnyException();
    }

    @Test
    void wrongKey_isRejected() {
        assertThatThrownBy(() -> guard.verify("outra-chave")).isInstanceOf(InvalidInternalKeyException.class);
    }

    @Test
    void missingKey_isRejected() {
        assertThatThrownBy(() -> guard.verify(null)).isInstanceOf(InvalidInternalKeyException.class);
    }

    @Test
    void blankConfiguredKey_failsFast() {
        assertThatThrownBy(() -> new InternalKeyGuard("  ")).isInstanceOf(IllegalStateException.class);
    }
}