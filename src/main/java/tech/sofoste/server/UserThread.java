package tech.sofoste.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Owns one client connection and always releases it in a finally block. */
final class UserThread extends Thread {
    private final Socket socket;
    private final ChatServer server;
    private volatile PrintWriter writer;
    private String userName;

    UserThread(Socket socket, ChatServer server) { super("familychat-user"); this.socket = socket; this.server = server; }

    @Override public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            writer = output;
            output.println("FAMILYCHAT/1");
            output.println("USERS|" + String.join(",", server.userNames()));
            output.println("NAME");
            String requested;
            while ((requested = reader.readLine()) != null) {
                requested = requested.trim();
                if (server.register(requested, this)) { userName = requested; break; }
                output.println("NAME_REJECTED|Choose a unique name of 2 to 24 characters");
            }
            if (userName == null) { return; }
            output.println("WELCOME|" + userName);
            server.broadcast("JOIN|" + userName, this);
            String message;
            while ((message = reader.readLine()) != null) {
                String clean = message.trim();
                if ("bye".equalsIgnoreCase(clean) || "/quit".equalsIgnoreCase(clean)) { break; }
                if (!clean.isEmpty() && clean.length() <= 1000) { server.broadcast("MSG|" + userName + "|" + clean, this); }
            }
        } catch (IOException exception) {
            if (!socket.isClosed()) { System.err.println("Client connection ended: " + exception.getMessage()); }
        } finally { server.remove(userName, this); closeConnection(); }
    }

    void sendMessage(String message) { PrintWriter output = writer; if (output != null) { output.println(message); } }
    void closeConnection() { try { socket.close(); } catch (IOException ignored) { } }
}
