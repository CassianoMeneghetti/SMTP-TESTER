package br.com.smtptesterpro.domain;

import java.time.Instant;
import java.util.List;

public record CertificateInfo(
        String subject,
        String issuer,
        String serialNumber,
        Instant validFrom,
        Instant validUntil,
        List<String> subjectAlternativeNames
) {
}
