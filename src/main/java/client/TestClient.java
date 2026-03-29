// A standalone script used to test the Order Book matching logic.
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
            String welcomeMessage = in.readLine();
            System.out.println("Server says: " + welcomeMessage);

            // 2. Send the BUY order (Bid) - Willing to pay $150
            String buyCommand = "BUY:AAPL:150.00:10";
            System.out.println("\n[TEST CLIENT] Sending BUY order: " + buyCommand);
            out.println(buyCommand);
            System.out.println("Server replied: " + in.readLine());

            // 3. Send the SELL order (Ask) - Willing to sell for $145
            // This overlaps with the buyer and should trigger a trade on the server!
            String sellCommand = "SELL:AAPL:145.00:5";
            System.out.println("\n[TEST CLIENT] Sending SELL order: " + sellCommand);
            out.println(sellCommand);
            System.out.println("Server replied: " + in.readLine());

            System.out.println("\n[TEST CLIENT] Test complete. Check the SERVER terminal to see the trade banner!");

        } catch (IOException e) {
            System.err.println("[TEST CLIENT ERROR] Could not connect to server. Is it running?");
        }
    }
}