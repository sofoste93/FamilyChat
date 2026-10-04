package tech.sofoste.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe TCP chat server used by the console edition. */
public final class ChatServer implements AutoCloseable {
    private final int port;
    private final ConcurrentHashMap<String, UserThread> users = new ConcurrentHashMap<String, UserThread>();
    private volatile boolean running;
    private volatile ServerSocket serverSocket;
    private volatile int boundPort = -1;

    public ChatServer(int port) { this.port = port; }

    public void execute() {
        running = true;
        try (ServerSocket listener = new ServerSocket(port)) {
            serverSocket = listener;
            boundPort = listener.getLocalPort();
            System.out.println("FamilyChat is listening on port " + listener.getLocalPort());
            while (running) {
                try {
                    Socket socket = listener.accept();
                    socket.setKeepAlive(true);
                    new UserThread(socket, this).start();
                } catch (SocketException exception) {
                    if (running) { System.err.println("Connection error: " + exception.getMessage()); }
                }
            }
        } catch (IOException exception) {
            if (running) { System.err.println("Server error: " + exception.getMessage()); }
        } finally { running = false; }
    }

    boolean register(String requestedName, UserThread thread) {
        String name = requestedName == null ? "" : requestedName.trim();
        return isValidName(name) && users.putIfAbsent(name, thread) == null;
    }

    private boolean isValidName(String name) {
        return name.length() >= 2 && name.length() <= 24 && name.matches("[\\p{L}\\p{N} _.-]+");
    }

    void broadcast(String message, UserThread excluded) {
        for (UserThread user : users.values()) { if (user != excluded) { user.sendMessage(message); } }
    }

    void remove(String userName, UserThread thread) {
        if (userName != null && users.remove(userName, thread)) {
            broadcast("LEAVE|" + userName, thread);
            System.out.println(userName + " left the chat");
        }
    }

    Set<String> userNames() { return Collections.unmodifiableSet(users.keySet()); }

    /** Exposed for launchers and integration tests using an ephemeral port. */
    public int getBoundPort() { return boundPort; }

    @Override public void close() {
        running = false;
        ServerSocket listener = serverSocket;
        if (listener != null) { try { listener.close(); } catch (IOException ignored) { } }
        for (UserThread user : users.values()) { user.closeConnection(); }
        users.clear();
    }
}
