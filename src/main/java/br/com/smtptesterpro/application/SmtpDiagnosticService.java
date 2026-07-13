package br.com.smtptesterpro.application;

import br.com.smtptesterpro.domain.DiagnosticStatus;
import br.com.smtptesterpro.domain.SecurityMode;
import br.com.smtptesterpro.domain.SmtpServerCapabilities;
import br.com.smtptesterpro.domain.SmtpTestRequest;
import br.com.smtptesterpro.infrastructure.DnsDiagnostic;
import br.com.smtptesterpro.infrastructure.NetworkProbe;
import br.com.smtptesterpro.infrastructure.SmtpClientSession;

import java.util.Locale;

import static br.com.smtptesterpro.domain.DiagnosticStep.done;
import static br.com.smtptesterpro.domain.DiagnosticStep.running;

public final class SmtpDiagnosticService {
    private final DnsDiagnostic dnsDiagnostic;
    private final NetworkProbe networkProbe;

    public SmtpDiagnosticService() {
        this(new DnsDiagnostic(), new NetworkProbe());
    }

    SmtpDiagnosticService(DnsDiagnostic dnsDiagnostic, NetworkProbe networkProbe) {
        this.dnsDiagnostic = dnsDiagnostic;
        this.networkProbe = networkProbe;
    }

    public void run(SmtpTestRequest request, DiagnosticListener listener) {
        diagnoseDns(request, listener);
        probeTcp(request, listener);
        runSmtpFlow(request, listener);
    }

    private void diagnoseDns(SmtpTestRequest request, DiagnosticListener listener) {
        if (request.from() == null || !request.from().contains("@")) {
            listener.onStep(done("DNS MX", DiagnosticStatus.SKIPPED, "Informe um remetente para consultar MX do dominio.", 0));
            return;
        }

        String domain = request.from().substring(request.from().indexOf('@') + 1).trim().toLowerCase(Locale.ROOT);
        listener.onStep(running("DNS MX", "Consultando registros MX de " + domain));
        long start = System.nanoTime();
        try {
            var records = dnsDiagnostic.lookupMx(domain);
            var status = records.isEmpty() ? DiagnosticStatus.WARNING : DiagnosticStatus.SUCCESS;
            var detail = records.isEmpty()
                    ? "Nenhum registro MX encontrado para " + domain
                    : "MX encontrados: " + String.join(", ", records);
            listener.onStep(done("DNS MX", status, detail, elapsedMillis(start)));
        } catch (RuntimeException exception) {
            listener.onStep(done("DNS MX", DiagnosticStatus.WARNING, exception.getMessage(), elapsedMillis(start)));
        }
    }

    private void probeTcp(SmtpTestRequest request, DiagnosticListener listener) {
        listener.onStep(running("Conectividade TCP", "Testando " + request.host() + ":" + request.port()));
        long start = System.nanoTime();
        try {
            long latency = networkProbe.measureConnectLatency(request.host(), request.port(), request.timeout());
            listener.onStep(done("Conectividade TCP", DiagnosticStatus.SUCCESS, "Porta aberta. Latencia: " + latency + " ms", elapsedMillis(start)));
        } catch (RuntimeException exception) {
            listener.onStep(done("Conectividade TCP", DiagnosticStatus.FAILED, exception.getMessage(), elapsedMillis(start)));
            throw exception;
        }
    }

    private void runSmtpFlow(SmtpTestRequest request, DiagnosticListener listener) {
        try (SmtpClientSession session = new SmtpClientSession(request, listener)) {
            session.connect();
            SmtpServerCapabilities capabilities = session.ehlo();

            if (request.securityMode() == SecurityMode.STARTTLS) {
                if (!capabilities.startTlsSupported()) {
                    listener.onStep(done("STARTTLS", DiagnosticStatus.FAILED, "Servidor nao anunciou STARTTLS.", 0));
                    throw new IllegalStateException("Servidor nao suporta STARTTLS.");
                }
                session.startTls();
                capabilities = session.ehlo();
            }

            if (request.authenticate()) {
                session.authenticate(capabilities);
            } else {
                listener.onStep(done("Autenticacao", DiagnosticStatus.SKIPPED, "Autenticacao desativada.", 0));
            }

            if (request.sendMessage()) {
                session.sendMail();
            } else {
                listener.onStep(done("Envio de e-mail", DiagnosticStatus.SKIPPED, "Envio real desativado.", 0));
            }

            session.quit();
        }
    }

    private long elapsedMillis(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
