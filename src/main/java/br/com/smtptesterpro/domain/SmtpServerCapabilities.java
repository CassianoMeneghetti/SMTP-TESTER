package br.com.smtptesterpro.domain;

import java.util.List;

public record SmtpServerCapabilities(
        List<String> extensions,
        List<String> authMechanisms,
        boolean startTlsSupported
) {
    public boolean supportsAuth(String mechanism) {
        return authMechanisms.stream().anyMatch(value -> value.equalsIgnoreCase(mechanism));
    }
}
