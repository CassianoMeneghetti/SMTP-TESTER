package br.com.smtptesterpro.domain;

import java.time.Instant;

public record DiagnosticStep(
        String name,
        DiagnosticStatus status,
        String detail,
        long durationMillis,
        Instant timestamp
) {
    public static DiagnosticStep running(String name, String detail) {
        return new DiagnosticStep(name, DiagnosticStatus.RUNNING, detail, 0, Instant.now());
    }

    public static DiagnosticStep done(String name, DiagnosticStatus status, String detail, long durationMillis) {
        return new DiagnosticStep(name, status, detail, durationMillis, Instant.now());
    }
}
