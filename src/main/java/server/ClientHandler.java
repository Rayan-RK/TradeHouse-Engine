package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            // Setup the I/O streams for this specific client
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Send a welcome message to the client as soon as they connect
            out.println("CONNECTION_SUCCESS: Welcome to TradeHouse.");

            String clientMessage;
            // Infinite loop to listen for incoming messages from this client
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("[INCOMING] User says: " + clientMessage);
                
                // For now, just echo it back so we know the connection is live.
                out.println("SERVER_ECHO: " + clientMessage);
            }

        } catch (IOException e) {
            System.out.println("[SERVER] Client disconnected abruptly.");
        } finally {
            try {
                // Always clean up resources when the client leaves
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
                System.out.println("[SERVER] Connection closed and thread released.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}