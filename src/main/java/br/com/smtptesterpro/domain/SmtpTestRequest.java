package br.com.smtptesterpro.domain;

import java.time.Duration;
import java.util.Objects;

public record SmtpTestRequest(
        String host,
        int port,
        SecurityMode securityMode,
        AuthMode authMode,
        String ehloDomain,
        String username,
        char[] password,
        char[] oauthAccessToken,
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
        Objects.requireNonNull(authMode, "authMode");
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
        oauthAccessToken = oauthAccessToken == null ? new char[0] : oauthAccessToken.clone();
        if (sendMessage && (from == null || from.isBlank() || to == null || to.isBlank())) {
            throw new IllegalArgumentException("Remetente e destinatario sao obrigatorios para envio.");
        }
        if (authenticate && authMode == AuthMode.XOAUTH2 && oauthAccessToken.length == 0) {
            throw new IllegalArgumentException("Access token OAuth2 e obrigatorio para autenticacao XOAUTH2.");
        }
        password = password == null ? new char[0] : password.clone();
    }

    @Override
    public char[] password() {
        return password.clone();
    }

    @Override
    public char[] oauthAccessToken() {
        return oauthAccessToken.clone();
    }
}
