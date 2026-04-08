package server;

import java.io.PrintWriter;
import java.net.Socket;

/**
 * Represents one active client connection tracked by the server.
 */
public class ClientSession {
    private final Socket socket;
    private final PrintWriter writer;
    private final String remoteAddress;

    public ClientSession(Socket socket, PrintWriter writer) {
        this.socket = socket;
        this.writer = writer;
        this.remoteAddress = socket.getInetAddress().getHostAddress();
    }

    public Socket getSocket() {
        return socket;
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }
}
