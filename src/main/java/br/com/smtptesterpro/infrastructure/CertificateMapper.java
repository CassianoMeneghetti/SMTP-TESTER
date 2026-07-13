package br.com.smtptesterpro.infrastructure;

import br.com.smtptesterpro.domain.CertificateInfo;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class CertificateMapper {
    private CertificateMapper() {
    }

    static CertificateInfo from(X509Certificate certificate) {
        return new CertificateInfo(
                certificate.getSubjectX500Principal().getName(),
                certificate.getIssuerX500Principal().getName(),
                certificate.getSerialNumber().toString(16),
                certificate.getNotBefore().toInstant(),
                certificate.getNotAfter().toInstant(),
                subjectAlternativeNames(certificate)
        );
    }

    private static List<String> subjectAlternativeNames(X509Certificate certificate) {
        try {
            Collection<List<?>> names = certificate.getSubjectAlternativeNames();
            if (names == null) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            for (List<?> name : names) {
                if (name.size() >= 2) {
                    values.add(String.valueOf(name.get(1)));
                }
            }
            return values;
        } catch (CertificateParsingException exception) {
            return List.of();
        }
    }
}
