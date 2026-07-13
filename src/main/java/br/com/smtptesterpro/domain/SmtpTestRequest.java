package br.com.smtptesterpro.domain;

import java.time.Duration;
import java.util.Objects;

public record SmtpTestRequest(
        String host,
        int port,
        SecurityMode securityMode,
        String ehloDomain,
        String username,
        char[] password,
        String from,
        String to,
        String subject,
        String body,
        boolean authenticate,
        boolean sendMessage,
        Duration timeout
) {
    public SmtpTestRequest {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(securityMode, "securityMode");
        Objects.requireNonNull(ehloDomain, "ehloDomain");
        Objects.requireNonNull(timeout, "timeout");
        if (host.isBlank()) {
            throw new IllegalArgumentException("Servidor SMTP e obrigatorio.");
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("Porta SMTP invalida.");
        }
        if (authenticate && (username == null || username.isBlank())) {
            throw new IllegalArgumentException("Usuario e obrigatorio quando autenticacao esta ativa.");
        }
        if (sendMessage && (from == null || from.isBlank() || to == null || to.isBlank())) {
            throw new IllegalArgumentException("Remetente e destinatario sao obrigatorios para envio.");
        }
        password = password == null ? new char[0] : password.clone();
    }

    @Override
    public char[] password() {
        return password.clone();
    }
}
