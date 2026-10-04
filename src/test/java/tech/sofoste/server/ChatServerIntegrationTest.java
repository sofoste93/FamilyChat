package tech.sofoste.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ChatServerIntegrationTest {
    @Test
    void twoClientsCanJoinAndExchangeUtf8Messages() throws Exception {
        ChatServer server = new ChatServer(0);
        Thread serverThread = new Thread(server::execute, "test-server");
        serverThread.setDaemon(true);
        serverThread.start();
        for (int attempt = 0; attempt < 100 && server.getBoundPort() < 0; attempt++) {
            Thread.sleep(10);
        }

        try (Peer alice = new Peer(server.getBoundPort()); Peer bob = new Peer(server.getBoundPort())) {
            alice.join("Alice");
            bob.join("Bob");
            assertEquals("JOIN|Bob", alice.read());
            bob.send("Café prêt ☕");
            assertEquals("MSG|Bob|Café prêt ☕", alice.read());
        } finally {
            server.close();
        }
        assertTrue(server.getBoundPort() > 0);
    }

    private static final class Peer implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader input;
        private final PrintWriter output;

        Peer(int port) throws Exception {
            socket = new Socket("127.0.0.1", port);
            socket.setSoTimeout(2000);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        void join(String name) throws Exception {
            assertEquals("FAMILYCHAT/1", read());
            assertTrue(read().startsWith("USERS|"));
            assertEquals("NAME", read());
            send(name);
            assertEquals("WELCOME|" + name, read());
        }

        String read() throws Exception { return input.readLine(); }
        void send(String line) { output.println(line); }
        @Override public void close() throws Exception { socket.close(); }
    }
}
