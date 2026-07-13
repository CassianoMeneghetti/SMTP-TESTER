package br.com.smtptesterpro.application;

import br.com.smtptesterpro.domain.DiagnosticStep;

public interface DiagnosticListener {
    void onStep(DiagnosticStep step);

    void onProtocol(String direction, String line);
}
