package br.com.tauan.ms2_transacoes.adapter.out.client;

import br.com.tauan.ms2_transacoes.domain.exception.AccountServiceUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockApiAccountAdapterTimeoutTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String startServerThatAnswersAfter(long millis) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/api/v1/accounts", exchange -> {
            try {
                Thread.sleep(millis);
                byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
                // o cliente já desistiu (timeout)
            } finally {
                exchange.close();
            }
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1";
    }

    private MockApiAccountAdapter adapterFor(String baseUrl, Duration timeout) {
        return new MockApiAccountAdapter(MockApiClientConfig.restClient(baseUrl, timeout));
    }

    @Test
    void findByUserId_whenTheServiceIsTooSlow_throwsUnavailableAroundTheTimeout() throws IOException {
        MockApiAccountAdapter adapter = adapterFor(startServerThatAnswersAfter(1500), Duration.ofMillis(300));

        long start = System.nanoTime();
        assertThatThrownBy(() -> adapter.findByUserId(1L))
                .isInstanceOf(AccountServiceUnavailableException.class);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertThat(elapsed).isLessThan(Duration.ofMillis(1400));
    }

    @Test
    void findByUserId_whenTheServiceAnswersInTime_returnsEmptyForAnEmptyList() throws IOException {
        MockApiAccountAdapter adapter = adapterFor(startServerThatAnswersAfter(0), Duration.ofSeconds(2));

        assertThat(adapter.findByUserId(1L)).isEmpty();
    }

    @Test
    void findByUserId_whenNothingIsListening_throwsUnavailable() throws IOException {
        int freePort;
        try (ServerSocket socket = new ServerSocket(0)) {
            freePort = socket.getLocalPort();
        }
        MockApiAccountAdapter adapter = adapterFor("http://127.0.0.1:" + freePort + "/api/v1",
                Duration.ofSeconds(1));

        assertThatThrownBy(() -> adapter.findByUserId(1L))
                .isInstanceOf(AccountServiceUnavailableException.class);
    }
}