// A standalone script used to test network connections and parser logic without needing a GUI.
package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import shared.Order; // Import new shared class

public class TestClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("[TEST CLIENT] Booting up...");

        // --- 1. LOCAL TEST CASE ---
        // This makes sure the shared Order class is working correctly
        Order myOrder = new Order("BUY", "AAPL", 150.00, 10);
        System.out.println("[TEST] Created local order object: " + myOrder.toString());
        // ---------------------------

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("[TEST CLIENT] Connected to TradeHouse Server!");

            // 2. Read the welcome message (Existing test)
            String welcomeMessage = in.readLine();
            System.out.println("Server says: " + welcomeMessage);

            // 3. Send a VALID command (Existing test - tests parser success)
            String validCommand = "BUY:AAPL:150.00:10";
            System.out.println("\n[TEST CLIENT] Sending VALID command: " + validCommand);
            out.println(validCommand);
            String validResponse = in.readLine();
            System.out.println("Server replied: " + validResponse);

            // --- 4. NEW PARSER TEST CASE ---
            // Send an INVALID command (tests parser error handling)
            String invalidCommand = "BUY:APPLE:CHEAP"; // Missing quantity and bad price
            System.out.println("\n[TEST CLIENT] Sending INVALID command: " + invalidCommand);
            out.println(invalidCommand);
            String invalidResponse = in.readLine();
            System.out.println("Server replied: " + invalidResponse);
            // -------------------------------

            System.out.println("\n[TEST CLIENT] Test complete. Disconnecting.");

        } catch (IOException e) {
            System.err.println("[TEST CLIENT ERROR] Could not connect to server. Is it running?");
        }
    }
}