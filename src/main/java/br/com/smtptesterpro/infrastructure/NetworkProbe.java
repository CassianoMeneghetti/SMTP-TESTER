package br.com.smtptesterpro.infrastructure;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

public final class NetworkProbe {
    public long measureConnectLatency(String host, int port, Duration timeout) {
        long start = System.nanoTime();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), Math.toIntExact(timeout.toMillis()));
            return (System.nanoTime() - start) / 1_000_000;
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel conectar em " + host + ":" + port + ". " + exception.getMessage(), exception);
        }
    }
}
