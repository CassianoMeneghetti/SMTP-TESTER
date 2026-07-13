package br.com.smtptesterpro.domain;

public enum AuthMode {
    USER_PASSWORD("Usuario/Senha"),
    XOAUTH2("OAuth2 XOAUTH2");

    private final String label;

    AuthMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
