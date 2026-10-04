package tech.sofoste.desktop;

import tech.sofoste.server.ChatServer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Swing desktop shell for FamilyChat 2.
 *
 * <p>The UI never performs socket work on Swing's event thread. LanClient owns
 * the network reader and sends small immutable events back through callbacks.</p>
 */
public final class FamilyChatApp {
    private static final Color INK = new Color(28, 58, 51);
    private static final Color GREEN = new Color(28, 126, 91);
    private static final Color MINT = new Color(226, 246, 238);
    private static final Color CREAM = new Color(249, 247, 240);
    private static final Color LINE = new Color(215, 228, 221);

    private FamilyChatApp() { }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            configureLookAndFeel();
            new Frame().setVisible(true);
        });
    }

    public static void renderScreenshot(String output) {
        try {
            configureLookAndFeel();
            Frame[] holder = new Frame[1];
            SwingUtilities.invokeAndWait(() -> {
                Frame frame = new Frame();
                frame.showDemo();
                frame.setSize(1280, 800);
                frame.setLocation(20, 20);
                frame.setVisible(true);
                holder[0] = frame;
            });
            Thread.sleep(800);
            BufferedImage image = new Robot().createScreenCapture(holder[0].getBounds());
            ImageIO.write(image, "png", Path.of(output).toFile());
            SwingUtilities.invokeAndWait(holder[0]::dispose);
        } catch (Exception exception) { throw new IllegalStateException("Cannot render screenshot", exception); }
    }

    private static void configureLookAndFeel() {
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 13));
        UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("TextArea.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Panel.background", CREAM);
    }

    private static final class Frame extends JFrame implements LanClient.Listener {
        private final CardLayout cards = new CardLayout();
        private final JPanel content = new JPanel(cards);
        private final JTextField name = field("Sofia");
        private final JTextField host = field("127.0.0.1");
        private final JTextField port = field("6868");
        private final JPanel timeline = new JPanel();
        private final JTextField composer = field("");
        private final DefaultListModel<String> people = new DefaultListModel<>();
        private final JLabel roomStatus = new JLabel("LOCAL · READY");
        private final JLabel roomTitle = new JLabel("Family room");
        private LanClient client;
        private ChatServer localServer;

        Frame() {
            super("FamilyChat");
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            setMinimumSize(new Dimension(920, 640));
            setSize(1180, 760);
            setLocationRelativeTo(null);
            setContentPane(buildRoot());
            addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent event) { shutdown(); } });
        }

        private JComponent buildRoot() {
            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(CREAM);
            root.add(header(), BorderLayout.NORTH);
            content.setOpaque(false);
            content.add(welcome(), "welcome");
            content.add(chat(), "chat");
            root.add(content, BorderLayout.CENTER);
            return root;
        }

        private JComponent header() {
            JPanel bar = new JPanel(new BorderLayout(18, 0));
            bar.setBackground(Color.WHITE);
            bar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(15, 28, 15, 28)));
            JLabel mark = new JLabel("  Y  "); mark.setOpaque(true); mark.setBackground(MINT); mark.setForeground(GREEN); mark.setFont(new Font("Georgia", Font.BOLD, 23));
            JPanel brand = new JPanel(new GridLayout(2, 1)); brand.setOpaque(false);
            JLabel title = new JLabel("FAMILYCHAT"); title.setForeground(INK); title.setFont(new Font("Segoe UI", Font.BOLD, 16));
            JLabel sub = new JLabel("PRIVATE LAN CIRCLE"); sub.setForeground(new Color(100, 125, 116)); sub.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            brand.add(title); brand.add(sub);
            JPanel identity = new JPanel(new BorderLayout(10, 0)); identity.setOpaque(false); identity.add(mark, BorderLayout.WEST); identity.add(brand);
            bar.add(identity, BorderLayout.WEST);
            JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0)); tools.setOpaque(false);
            tools.add(linkButton("Help", this::showHelp)); tools.add(linkButton("Settings", this::showSettings));
            bar.add(tools, BorderLayout.EAST);
            return bar;
        }

        private JComponent welcome() {
            JPanel outer = new JPanel(new GridBagLayout()); outer.setOpaque(false); outer.setBorder(new EmptyBorder(35, 40, 55, 40));
            JPanel panel = new JPanel(new BorderLayout(52, 0)); panel.setOpaque(false); panel.setPreferredSize(new Dimension(1030, 535));
            JPanel story = new JPanel(); story.setOpaque(false); story.setLayout(new BoxLayout(story, BoxLayout.Y_AXIS));
            JLabel eyebrow = new JLabel("LOCAL FIRST · FAMILY SPACE"); eyebrow.setForeground(GREEN); eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 11));
            JLabel headline = new JLabel("<html>Your people.<br>Your network.<br>Your conversation.</html>"); headline.setForeground(INK); headline.setFont(new Font("Georgia", Font.PLAIN, 43));
            JLabel body = new JLabel("<html>A calm family chat that stays inside your trusted home network.<br>No account, no cloud, no tracking.</html>"); body.setForeground(new Color(93, 113, 106)); body.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            story.add(eyebrow); story.add(Box.createVerticalStrut(25)); story.add(headline); story.add(Box.createVerticalStrut(24)); story.add(body); story.add(Box.createVerticalGlue());
            JLabel privacy = new JLabel("●  LOCAL SESSION     ◇  NO HISTORY     ↗  CROSS-PLATFORM"); privacy.setForeground(GREEN); privacy.setFont(new Font("Segoe UI", Font.BOLD, 10)); story.add(privacy);
            panel.add(story, BorderLayout.CENTER);
            panel.add(connectionCard(), BorderLayout.EAST);
            outer.add(panel); return outer;
        }

        private JComponent connectionCard() {
            JPanel card = new JPanel(); card.setBackground(Color.WHITE); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), new EmptyBorder(28, 28, 28, 28))); card.setPreferredSize(new Dimension(360, 500));
            JLabel title = new JLabel("Join the family circle"); title.setForeground(INK); title.setFont(new Font("Georgia", Font.PLAIN, 24));
            card.add(title); card.add(Box.createVerticalStrut(8)); card.add(muted("Choose a name and the host address.")); card.add(Box.createVerticalStrut(25));
            card.add(label("YOUR NAME")); card.add(name); card.add(Box.createVerticalStrut(15)); card.add(label("HOST ADDRESS")); card.add(host); card.add(Box.createVerticalStrut(15)); card.add(label("PORT")); card.add(port); card.add(Box.createVerticalStrut(24));
            JButton join = primary("Join room  →", () -> connect(false)); join.setAlignmentX(Component.LEFT_ALIGNMENT); card.add(join); card.add(Box.createVerticalStrut(11));
            JButton create = secondary("Start a room on this PC", () -> connect(true)); create.setAlignmentX(Component.LEFT_ALIGNMENT); card.add(create); card.add(Box.createVerticalGlue());
            card.add(muted("Use only on a trusted private network.")); return card;
        }

        private JComponent chat() {
            JPanel page = new JPanel(new BorderLayout()); page.setOpaque(false); page.setBorder(new EmptyBorder(22, 28, 28, 28));
            JPanel heading = new JPanel(new BorderLayout()); heading.setOpaque(false); roomTitle.setForeground(INK); roomTitle.setFont(new Font("Georgia", Font.PLAIN, 28));
            roomStatus.setForeground(GREEN); roomStatus.setFont(new Font("Segoe UI", Font.BOLD, 10)); heading.add(roomTitle, BorderLayout.WEST); heading.add(roomStatus, BorderLayout.EAST); page.add(heading, BorderLayout.NORTH);
            timeline.setLayout(new BoxLayout(timeline, BoxLayout.Y_AXIS)); timeline.setBackground(Color.WHITE); timeline.setBorder(new EmptyBorder(18, 18, 18, 18));
            JScrollPane scroll = new JScrollPane(timeline); scroll.setBorder(BorderFactory.createLineBorder(LINE)); scroll.getVerticalScrollBar().setUnitIncrement(14);
            JList<String> roster = new JList<>(people); roster.setBackground(MINT); roster.setForeground(INK); roster.setBorder(new EmptyBorder(18, 18, 18, 18)); roster.setPreferredSize(new Dimension(210, 0));
            JPanel center = new JPanel(new BorderLayout(16, 0)); center.setOpaque(false); center.setBorder(new EmptyBorder(18, 0, 16, 0)); center.add(scroll); center.add(roster, BorderLayout.EAST); page.add(center);
            JPanel send = new JPanel(new BorderLayout(10, 0)); send.setOpaque(false); composer.addActionListener(e -> send()); send.add(composer); send.add(primary("Send", this::send), BorderLayout.EAST); page.add(send, BorderLayout.SOUTH); return page;
        }

        private void connect(boolean hosting) {
            int selectedPort;
            try { selectedPort = Integer.parseInt(port.getText().trim()); if (selectedPort < 1024 || selectedPort > 65535) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { error("Port must be between 1024 and 65535."); return; }
            String selectedName = name.getText().trim(); if (selectedName.length() < 2) { error("Please choose a name with at least two characters."); return; }
            if (hosting) {
                localServer = new ChatServer(selectedPort);
                Thread serverThread = new Thread(localServer::execute, "familychat-local-server"); serverThread.setDaemon(true); serverThread.start();
                host.setText("127.0.0.1");
                try { Thread.sleep(120); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
            client = new LanClient(host.getText().trim(), selectedPort, selectedName, this);
            client.connect();
        }

        private void send() { String text = composer.getText().trim(); if (client != null && !text.isEmpty()) { client.send(text); message(name.getText().trim(), text, true); composer.setText(""); } }
        private void message(String author, String text, boolean mine) {
            JPanel bubble = new JPanel(new BorderLayout(10, 3)); bubble.setBackground(mine ? MINT : new Color(244, 246, 244)); bubble.setBorder(new EmptyBorder(10, 13, 10, 13)); bubble.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
            JLabel who = new JLabel(author + "  ·  " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))); who.setForeground(GREEN); who.setFont(new Font("Segoe UI", Font.BOLD, 10));
            JLabel words = new JLabel("<html>" + escape(text) + "</html>"); words.setForeground(INK); bubble.add(who, BorderLayout.NORTH); bubble.add(words); timeline.add(bubble); timeline.add(Box.createVerticalStrut(8)); timeline.revalidate(); timeline.repaint();
        }

        void showDemo() { cards.show(content, "chat"); people.addElement("●  Sofia (you)"); people.addElement("●  Enrico"); people.addElement("●  Islam"); message("Enrico", "Dinner at 19:30 — I will bring dessert 🍰", false); message("Sofia", "Perfect. I’ll tell everyone!", true); }
        @Override public void connected(String user) { SwingUtilities.invokeLater(() -> { cards.show(content, "chat"); people.addElement("●  " + user + " (you)"); roomStatus.setText("●  CONNECTED · LOCAL"); message("FamilyChat", "Welcome to the family circle.", false); composer.requestFocusInWindow(); }); }
        @Override public void event(String type, String user, String text) { SwingUtilities.invokeLater(() -> { if ("MSG".equals(type)) message(user, text, false); else if ("JOIN".equals(type)) { people.addElement("●  " + user); message("FamilyChat", user + " joined.", false); } else if ("LEAVE".equals(type)) { for (int i=0;i<people.size();i++) if (people.get(i).contains(user)) { people.remove(i); break; } message("FamilyChat", user + " left.", false); } }); }
        @Override public void failed(String reason) { SwingUtilities.invokeLater(() -> error(reason)); }
        private void shutdown() { if (client != null) client.close(); if (localServer != null) localServer.close(); }
        private void showHelp() { JOptionPane.showMessageDialog(this, "1. Start one room on a family computer.\n2. Share its private IPv4 address and port.\n3. Everyone joins from the same trusted network.\n\nMessages are not stored. Do not expose the port to the internet.", "FamilyChat help", JOptionPane.INFORMATION_MESSAGE); }
        private void showSettings() { JOptionPane.showMessageDialog(this, "FamilyChat follows your system font scaling.\nDefault port: 6868\nStorage: none\nTelemetry: none", "Settings", JOptionPane.PLAIN_MESSAGE); }
        private void error(String text) { JOptionPane.showMessageDialog(this, text, "FamilyChat", JOptionPane.ERROR_MESSAGE); }
    }

    /** Minimal line-protocol client. One daemon thread owns all network reads. */
    private static final class LanClient implements AutoCloseable {
        interface Listener { void connected(String user); void event(String type, String user, String text); void failed(String reason); }
        private final String host, name; private final int port; private final Listener listener; private Socket socket; private PrintWriter output;
        LanClient(String host, int port, String name, Listener listener) { this.host=host; this.port=port; this.name=name; this.listener=listener; }
        void connect() { Thread thread = new Thread(this::readLoop, "familychat-reader"); thread.setDaemon(true); thread.start(); }
        private void readLoop() {
            try {
                socket = new Socket(); socket.connect(new InetSocketAddress(host, port), 4000);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                String line; while ((line=input.readLine()) != null) {
                    String[] parts=line.split("\\|",3);
                    if ("NAME".equals(parts[0])) output.println(name);
                    else if ("WELCOME".equals(parts[0])) listener.connected(name);
                    else if ("NAME_REJECTED".equals(parts[0])) { listener.failed("This name is unavailable."); close(); break; }
                    else if ("MSG".equals(parts[0]) && parts.length==3) listener.event("MSG",parts[1],parts[2]);
                    else if (("JOIN".equals(parts[0])||"LEAVE".equals(parts[0])) && parts.length>1) listener.event(parts[0],parts[1],"");
                }
            } catch (IOException ex) { if (socket == null || !socket.isClosed()) listener.failed("Cannot reach the room at " + host + ":" + port); }
        }
        void send(String text) { if (output != null) output.println(text); }
        @Override public void close() { try { if (output != null) output.println("/quit"); if (socket != null) socket.close(); } catch (IOException ignored) { } }
    }

    private static JTextField field(String value) { JTextField field=new JTextField(value); field.setMaximumSize(new Dimension(Integer.MAX_VALUE,40)); field.setPreferredSize(new Dimension(280,40)); field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),new EmptyBorder(9,11,9,11))); return field; }
    private static JLabel label(String text) { JLabel label=new JLabel(text); label.setForeground(new Color(95,116,108)); label.setFont(new Font("Segoe UI",Font.BOLD,10)); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label; }
    private static JLabel muted(String text) { JLabel label=new JLabel(text); label.setForeground(new Color(100,120,112)); label.setFont(new Font("Segoe UI",Font.PLAIN,12)); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label; }
    private static JButton primary(String text,Runnable action) { JButton button=new JButton(text); button.setForeground(Color.WHITE); button.setBackground(GREEN); button.setFocusPainted(false); button.setBorder(new EmptyBorder(12,18,12,18)); button.addActionListener(e->action.run()); return button; }
    private static JButton secondary(String text,Runnable action) { JButton button=new JButton(text); button.setForeground(INK); button.setBackground(Color.WHITE); button.setFocusPainted(false); button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),new EmptyBorder(11,17,11,17))); button.addActionListener(e->action.run()); return button; }
    private static JButton linkButton(String text,Runnable action) { JButton button=new JButton(text); button.setForeground(INK); button.setBackground(Color.WHITE); button.setBorder(new EmptyBorder(9,12,9,12)); button.addActionListener(e->action.run()); return button; }
    private static String escape(String text) { return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
}
