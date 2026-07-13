package br.com.smtptesterpro.domain;

public enum SecurityMode {
    NONE("Sem TLS"),
    STARTTLS("STARTTLS"),
    SSL_TLS("SSL/TLS direto");

    private final String label;

    SecurityMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
