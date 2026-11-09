package br.com.tauan.ms1_usuarios.adapter.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalKeyGuard {

    private final byte[] expectedKey;

    public InternalKeyGuard(@Value("${internal-api.key}") String expectedKey) {
        if (expectedKey == null || expectedKey.isBlank()) {
            throw new IllegalStateException("internal-api.key must be configured");
        }
        this.expectedKey = expectedKey.getBytes(StandardCharsets.UTF_8);
    }

    public void verify(String providedKey) {
        byte[] provided = providedKey == null ? new byte[0] : providedKey.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedKey, provided)) {
            throw new InvalidInternalKeyException();
        }
    }
}