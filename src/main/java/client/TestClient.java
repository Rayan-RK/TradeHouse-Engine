// A standalone script used to test the Order Book matching and Real-Time Broadcasting.
package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("[TEST CLIENT] Booting up...");

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("[TEST CLIENT] Connected to TradeHouse Server!");

            // 1. Read the welcome message
            System.out.println("Server says: " + in.readLine());

            // 2. Send the BUY order (Bid)
            String buyCommand = "BUY:AAPL:150.00:10";
            System.out.println("\n[TEST CLIENT] Sending BUY order: " + buyCommand);
            out.println(buyCommand);
            
            // 3. Send the SELL order (Ask) to trigger the match
            String sellCommand = "SELL:AAPL:145.00:5";
            System.out.println("[TEST CLIENT] Sending SELL order: " + sellCommand);
            out.println(sellCommand);

            // --- NEW: BROADCAST LISTENER LOOP ---
            System.out.println("\n[TEST CLIENT] Waiting for Server Broadcasts...");
            
            // We listen for the next 3 messages from the server
            for (int i = 0; i < 3; i++) {
                String serverMsg = in.readLine();
                if (serverMsg != null) {
                    System.out.println(">>> BROADCAST RECEIVED: " + serverMsg);
                }
            }
            // ------------------------------------

            System.out.println("\n[TEST CLIENT] Test complete. If you saw 'MARKET_UPDATE', broadcasting is working!");

        } catch (IOException e) {
            System.err.println("[TEST CLIENT ERROR] Could not connect to server.");
        }
    }
}