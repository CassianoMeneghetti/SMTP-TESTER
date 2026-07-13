package br.com.smtptesterpro.infrastructure;

import br.com.smtptesterpro.application.DiagnosticListener;
import br.com.smtptesterpro.domain.CertificateInfo;
import br.com.smtptesterpro.domain.DiagnosticStatus;
import br.com.smtptesterpro.domain.SecurityMode;
import br.com.smtptesterpro.domain.SmtpServerCapabilities;
import br.com.smtptesterpro.domain.SmtpTestRequest;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import static br.com.smtptesterpro.domain.DiagnosticStep.done;
import static br.com.smtptesterpro.domain.DiagnosticStep.running;

public final class SmtpClientSession implements Closeable {
    private final SmtpTestRequest request;
    private final DiagnosticListener listener;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public SmtpClientSession(SmtpTestRequest request, DiagnosticListener listener) {
        this.request = request;
        this.listener = listener;
    }

    public void connect() {
        listener.onStep(running("Handshake SMTP", "Abrindo sessao SMTP"));
        long start = System.nanoTime();
        try {
            socket = createSocket(request.securityMode() == SecurityMode.SSL_TLS);
            socket.setSoTimeout(Math.toIntExact(request.timeout().toMillis()));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            if (socket instanceof SSLSocket sslSocket) {
                reportTls(sslSocket, start);
            }

            SmtpResponse banner = readResponse();
            expectPositive(banner, "Banner SMTP invalido");
            listener.onStep(done("Handshake SMTP", DiagnosticStatus.SUCCESS, banner.joinedText(), elapsedMillis(start)));
        } catch (IOException exception) {
            listener.onStep(done("Handshake SMTP", DiagnosticStatus.FAILED, exception.getMessage(), elapsedMillis(start)));
            throw new IllegalStateException("Falha no handshake SMTP.", exception);
        }
    }

    public SmtpServerCapabilities ehlo() {
        listener.onStep(running("EHLO", "Negociando capacidades"));
        long start = System.nanoTime();
        try {
            SmtpResponse response = command("EHLO " + request.ehloDomain());
            expectPositive(response, "EHLO recusado");
            SmtpServerCapabilities capabilities = parseCapabilities(response.lines());
            String auth = capabilities.authMechanisms().isEmpty() ? "sem AUTH anunciado" : "AUTH " + String.join("/", capabilities.authMechanisms());
            String tls = capabilities.startTlsSupported() ? "STARTTLS disponivel" : "STARTTLS nao anunciado";
            listener.onStep(done("EHLO", DiagnosticStatus.SUCCESS, tls + "; " + auth, elapsedMillis(start)));
            return capabilities;
        } catch (RuntimeException exception) {
            listener.onStep(done("EHLO", DiagnosticStatus.FAILED, exception.getMessage(), elapsedMillis(start)));
            throw exception;
        }
    }

    public void startTls() {
        listener.onStep(running("STARTTLS", "Promovendo conexao para TLS"));
        long start = System.nanoTime();
        try {
            SmtpResponse response = command("STARTTLS");
            expectPositive(response, "STARTTLS recusado");

            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, request.host(), request.port(), true);
            sslSocket.setUseClientMode(true);
            sslSocket.startHandshake();
            socket = sslSocket;
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            reportTls(sslSocket, start);
            listener.onStep(done("STARTTLS", DiagnosticStatus.SUCCESS, "Conexao TLS estabelecida.", elapsedMillis(start)));
        } catch (IOException exception) {
            listener.onStep(done("STARTTLS", DiagnosticStatus.FAILED, exception.getMessage(), elapsedMillis(start)));
            throw new IllegalStateException("Falha ao iniciar STARTTLS.", exception);
        }
    }

    public void authenticate(SmtpServerCapabilities capabilities) {
        listener.onStep(running("Autenticacao", "Validando credenciais SMTP"));
        long start = System.nanoTime();
        try {
            if (capabilities.supportsAuth("PLAIN")) {
                authenticatePlain();
            } else if (capabilities.supportsAuth("LOGIN")) {
                authenticateLogin();
            } else {
                throw new IllegalStateException("Servidor nao anunciou AUTH PLAIN ou LOGIN.");
            }
            listener.onStep(done("Autenticacao", DiagnosticStatus.SUCCESS, "Credenciais aceitas.", elapsedMillis(start)));
        } catch (RuntimeException exception) {
            listener.onStep(done("Autenticacao", DiagnosticStatus.FAILED, exception.getMessage(), elapsedMillis(start)));
            throw exception;
        }
    }

    public void sendMail() {
        listener.onStep(running("Envio de e-mail", "Enviando mensagem de teste"));
        long start = System.nanoTime();
        try {
            expectPositive(command("MAIL FROM:<" + request.from() + ">"), "MAIL FROM recusado");
            expectPositive(command("RCPT TO:<" + request.to() + ">"), "RCPT TO recusado");
            expectCode(command("DATA"), 354, "DATA recusado");
            writeLine("Subject: " + request.subject());
            writeLine("From: " + request.from());
            writeLine("To: " + request.to());
            writeLine("Content-Type: text/plain; charset=UTF-8");
            writeLine("");
            for (String line : request.body().replace("\r\n", "\n").split("\n", -1)) {
                writeLine(line.startsWith(".") ? "." + line : line);
            }
            writeLine(".");
            expectPositive(readResponse(), "Mensagem recusada");
            listener.onStep(done("Envio de e-mail", DiagnosticStatus.SUCCESS, "Mensagem aceita pelo servidor.", elapsedMillis(start)));
        } catch (IOException exception) {
            listener.onStep(done("Envio de e-mail", DiagnosticStatus.FAILED, exception.getMessage(), elapsedMillis(start)));
            throw new IllegalStateException("Falha ao ler resposta do envio.", exception);
        } catch (RuntimeException exception) {
            listener.onStep(done("Envio de e-mail", DiagnosticStatus.FAILED, exception.getMessage(), elapsedMillis(start)));
            throw exception;
        }
    }

    public void quit() {
        try {
            command("QUIT");
        } catch (RuntimeException ignored) {
            // A sessao ja cumpriu o diagnostico principal.
        }
    }

    private Socket createSocket(boolean ssl) throws IOException {
        Duration timeout = request.timeout();
        if (ssl) {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslSocket = (SSLSocket) factory.createSocket();
            sslSocket.connect(new InetSocketAddress(request.host(), request.port()), Math.toIntExact(timeout.toMillis()));
            sslSocket.startHandshake();
            return sslSocket;
        }
        Socket plainSocket = new Socket();
        plainSocket.connect(new InetSocketAddress(request.host(), request.port()), Math.toIntExact(timeout.toMillis()));
        return plainSocket;
    }

    private void authenticatePlain() {
        String secret = "\0" + request.username() + "\0" + new String(request.password());
        String encoded = Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8));
        expectPositive(command("AUTH PLAIN " + encoded, true), "AUTH PLAIN recusado");
    }

    private void authenticateLogin() {
        expectCode(command("AUTH LOGIN"), 334, "AUTH LOGIN recusado");
        expectCode(command(Base64.getEncoder().encodeToString(request.username().getBytes(StandardCharsets.UTF_8)), true), 334, "Usuario recusado");
        expectPositive(command(Base64.getEncoder().encodeToString(new String(request.password()).getBytes(StandardCharsets.UTF_8)), true), "Senha recusada");
    }

    private SmtpResponse command(String command) {
        return command(command, false);
    }

    private SmtpResponse command(String command, boolean secret) {
        try {
            writeLine(command, secret);
            return readResponse();
        } catch (IOException exception) {
            throw new IllegalStateException("Falha de comunicacao SMTP: " + exception.getMessage(), exception);
        }
    }

    private void writeLine(String value) {
        writeLine(value, false);
    }

    private void writeLine(String value, boolean secret) {
        try {
            listener.onProtocol("C", secret ? maskCommand(value) : value);
            writer.write(value);
            writer.write("\r\n");
            writer.flush();
        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao enviar comando SMTP: " + exception.getMessage(), exception);
        }
    }

    private SmtpResponse readResponse() throws IOException {
        List<String> lines = new ArrayList<>();
        String line;
        do {
            line = reader.readLine();
            if (line == null) {
                throw new IOException("Servidor encerrou a conexao.");
            }
            listener.onProtocol("S", line);
            lines.add(line);
        } while (line.length() >= 4 && line.charAt(3) == '-');
        return new SmtpResponse(lines);
    }

    private void reportTls(SSLSocket sslSocket, long start) {
        SSLSession session = sslSocket.getSession();
        String detail = "TLS " + session.getProtocol() + "; cifra " + session.getCipherSuite();
        listener.onStep(done("TLS", DiagnosticStatus.SUCCESS, detail, elapsedMillis(start)));
        try {
            Certificate[] certificates = session.getPeerCertificates();
            if (certificates.length > 0 && certificates[0] instanceof X509Certificate certificate) {
                CertificateInfo info = CertificateMapper.from(certificate);
                listener.onStep(done(
                        "Certificado",
                        DiagnosticStatus.SUCCESS,
                        info.subject() + "; valido ate " + info.validUntil(),
                        0
                ));
            }
        } catch (Exception exception) {
            listener.onStep(done("Certificado", DiagnosticStatus.WARNING, "Nao foi possivel ler certificado: " + exception.getMessage(), 0));
        }
    }

    private SmtpServerCapabilities parseCapabilities(List<String> lines) {
        List<String> extensions = new ArrayList<>();
        List<String> auth = new ArrayList<>();
        boolean startTls = false;
        for (int index = 1; index < lines.size(); index++) {
            String extension = lines.get(index).length() > 4 ? lines.get(index).substring(4).trim() : "";
            extensions.add(extension);
            String upper = extension.toUpperCase(Locale.ROOT);
            if (upper.equals("STARTTLS")) {
                startTls = true;
            }
            if (upper.startsWith("AUTH")) {
                String[] tokens = extension.split("\\s+");
                for (int i = 1; i < tokens.length; i++) {
                    auth.add(tokens[i].toUpperCase(Locale.ROOT));
                }
            }
        }
        return new SmtpServerCapabilities(List.copyOf(extensions), List.copyOf(auth), startTls);
    }

    private void expectPositive(SmtpResponse response, String message) {
        if (response.code() < 200 || response.code() >= 400) {
            throw new IllegalStateException(message + ": " + response.joinedText());
        }
    }

    private void expectCode(SmtpResponse response, int expectedCode, String message) {
        if (response.code() != expectedCode) {
            throw new IllegalStateException(message + ": " + response.joinedText());
        }
    }

    private String maskCommand(String command) {
        if (command.toUpperCase(Locale.ROOT).startsWith("AUTH")) {
            return command.split("\\s+")[0] + " ********";
        }
        return "********";
    }

    private long elapsedMillis(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    @Override
    public void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Closing quietly keeps diagnostics focused on the root failure.
            }
        }
    }

    private record SmtpResponse(List<String> lines) {
        int code() {
            if (lines.isEmpty() || lines.getFirst().length() < 3) {
                return -1;
            }
            try {
                return Integer.parseInt(lines.getFirst().substring(0, 3));
            } catch (NumberFormatException exception) {
                return -1;
            }
        }

        String joinedText() {
            return String.join(" | ", lines);
        }
    }
}
