// A dedicated background thread that handles network reading and writing for a single user.
package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import shared.Order;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        PrintWriter out = null;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            ClientSession session = new ClientSession(clientSocket, out);

            // Register this client centrally so future heartbeat cleanup can target live sessions.
            Server.registerClient(session);
            out.println("CONNECTION_SUCCESS: Welcome to TradeHouse.");

            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("[SERVER] Received from client: " + clientMessage);

                if (clientMessage.toUpperCase().startsWith("LOGIN:")) {
                    handleLogin(clientMessage, session);
                    continue;
                }

                if ("PONG".equalsIgnoreCase(clientMessage)) {
                    session.recordPong();
                    System.out.println("[SERVER] Heartbeat acknowledged by " + session.getRemoteAddress());
                    continue;
                }

                if ("PING".equalsIgnoreCase(clientMessage)) {
                    session.sendMessage("PONG");
                    continue;
                }

                Order newOrder = CommandParser.parseOrder(clientMessage);
                
                if (newOrder != null) {
                    if (Order.UNKNOWN_USER.equals(newOrder.getUsername()) && session.getUsername() != null) {
                        newOrder = new Order(session.getUsername(), newOrder.getType(), newOrder.getTicker(), newOrder.getPrice(), newOrder.getQuantity());
                    }

                    if (!Order.UNKNOWN_USER.equals(newOrder.getUsername())) {
                        session.setUsername(newOrder.getUsername());
                        String reserveError = Server.accountManager.tryReserveForOrder(newOrder);
                        if (reserveError != null) {
                            out.println("SERVER_ERROR: " + reserveError);
                            continue;
                        }
                    }

                    Server.market.processOrder(newOrder);
                    out.println("SERVER_ACK: Order received and sent to Order Book.");
                } else {
                    out.println("SERVER_ERROR: Invalid command format.");
                }
            }

        } catch (IOException e) {
            System.err.println("[SERVER ERROR] Client connection issue: " + e.getMessage());
        } finally {
            Server.removeClient(clientSocket);
            try {
                System.out.println("[SERVER] Closing client connection.");
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("[SERVER ERROR] Could not close socket.");
            }
        }
    }

    private void handleLogin(String clientMessage, ClientSession session) {
        String[] parts = clientMessage.split(":", -1);
        if (parts.length != 3) {
            session.sendMessage("SERVER_ERROR: LOGIN format must be LOGIN:USERNAME:STARTING_BALANCE");
            return;
        }

        try {
            String username = parts[1].trim().toUpperCase();
            double startingBalance = Double.parseDouble(parts[2].trim());

            if (username.isBlank()) {
                session.sendMessage("SERVER_ERROR: Username cannot be blank.");
                return;
            }

            if (Double.isNaN(startingBalance) || Double.isInfinite(startingBalance) || startingBalance < 0) {
                session.sendMessage("SERVER_ERROR: Starting balance cannot be negative.");
                return;
            }

            UserAccount account = Server.accountManager.login(username, startingBalance);
            session.setUsername(account.getUsername());
            Server.fileManager.saveAccountState(Server.accountManager.getAccountsSnapshot());
            session.sendMessage(String.format("LOGIN_SUCCESS:%s:%.2f", account.getUsername(), account.getCashBalance()));
        } catch (NumberFormatException e) {
            session.sendMessage("SERVER_ERROR: Invalid starting balance for LOGIN.");
        }
    }
}
