package client.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class ClientNetwork implements AutoCloseable {
    private final String host;
    private final int port;
    private final Consumer<String> messageHandler;
    private final Consumer<String> errorHandler;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private volatile boolean connected;

    public ClientNetwork(String host, int port, Consumer<String> messageHandler, Consumer<String> errorHandler) {
        this.host = host;
        this.port = port;
        this.messageHandler = messageHandler;
        this.errorHandler = errorHandler;
    }

    public synchronized void connect() throws IOException {
        if (connected) {
            return;
        }

        socket = new Socket(host, port);
        writer = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connected = true;
        startReaderThread();
    }

    public void login(String username, String startingBalance) {
        send(String.format("LOGIN:%s:%s", username.trim(), startingBalance.trim()));
    }

    public void submitOrder(String side, String ticker, String price, String quantity) {
        send(String.format("%s:%s:%s:%s", side.trim(), ticker.trim(), price.trim(), quantity.trim()));
    }

    public synchronized void send(String message) {
        if (!connected || writer == null) {
            throw new IllegalStateException("Not connected to the TradeHouse server.");
        }
        writer.println(message);
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public synchronized void close() {
        connected = false;

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            errorHandler.accept("Failed to close socket: " + e.getMessage());
        }
    }

    private void startReaderThread() {
        Thread readerThread = new Thread(this::readMessages, "tradehouse-client-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readMessages() {
        boolean closedByServer = false;
        try {
            String message;
            while (connected && (message = reader.readLine()) != null) {
                if ("PING".equalsIgnoreCase(message)) {
                    send("PONG");
                }
                messageHandler.accept(message);
            }
            closedByServer = connected;
        } catch (IOException e) {
            if (connected) {
                errorHandler.accept("Connection lost: " + e.getMessage());
            }
        } finally {
            connected = false;
            if (closedByServer) {
                errorHandler.accept("Connection closed by server.");
            }
        }
    }
}
