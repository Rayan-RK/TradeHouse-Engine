package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import shared.Order;

public class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Send welcome message
            out.println("CONNECTION_SUCCESS: Welcome to TradeHouse.");

            String clientMessage;
            // Keep reading messages as long as the client is connected
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("[SERVER] Received from client: " + clientMessage);

                // --- NEW PARSING LOGIC ---
                Order newOrder = CommandParser.parseOrder(clientMessage);
                
                if (newOrder != null) {
                    out.println("SERVER_ACK: Successfully parsed " + newOrder.toString());
                } else {
                    out.println("SERVER_ERROR: Invalid command format. Use ACTION:TICKER:PRICE:QUANTITY");
                }
                // -------------------------
            }

        } catch (IOException e) {
            System.err.println("[SERVER ERROR] Client connection issue: " + e.getMessage());
        } finally {
            try {
                System.out.println("[SERVER] Closing client connection.");
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("[SERVER ERROR] Could not close socket.");
            }
        }
    }
}