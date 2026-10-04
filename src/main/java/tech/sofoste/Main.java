package tech.sofoste;

import tech.sofoste.client.ChatClient;
import tech.sofoste.desktop.FamilyChatApp;
import tech.sofoste.server.ChatServer;

/** One entry point for the desktop app and the preserved console commands. */
public final class Main {
    private static final int DEFAULT_PORT = 6868;
    private Main() { }

    public static void main(String[] args) {
        if (args.length == 0) { FamilyChatApp.launch(); return; }
        if ("--help".equals(args[0])) { printHelp(); return; }
        if ("--version".equals(args[0])) { System.out.println("FamilyChat 2.0.0"); return; }
        if ("--screenshot".equals(args[0]) && args.length > 1) { FamilyChatApp.renderScreenshot(args[1]); return; }
        try {
            if ("server".equalsIgnoreCase(args[0])) {
                new ChatServer(args.length > 1 ? port(args[1]) : DEFAULT_PORT).execute();
            } else if ("client".equalsIgnoreCase(args[0]) && args.length >= 2) {
                new ChatClient(args[1], args.length > 2 ? port(args[2]) : DEFAULT_PORT).execute();
            } else { printHelp(); }
        } catch (IllegalArgumentException exception) { System.err.println("Error: " + exception.getMessage()); }
    }

    private static int port(String raw) {
        final int value;
        try { value = Integer.parseInt(raw); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("port must be a number"); }
        if (value < 1024 || value > 65535) { throw new IllegalArgumentException("port must be between 1024 and 65535"); }
        return value;
    }

    private static void printHelp() {
        System.out.println("FamilyChat 2.0.0");
        System.out.println("  Desktop: java -jar family-chat-2.0.0.jar");
        System.out.println("  Server:  java -jar family-chat-2.0.0.jar server [port]");
        System.out.println("  Client:  java -jar family-chat-2.0.0.jar client <host> [port]");
        System.out.println("  Leave:  /quit or bye");
    }
}
