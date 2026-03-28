package main.java.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {
    private static final String SERVER_IP = "127.0.0.1"; // Localhost
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("[TEST CLIENT] Booting up...");

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("[TEST CLIENT] Connected to TradeHouse Server!");

            // 1. Read the welcome message from the server
            String welcomeMessage = in.readLine();
            System.out.println("Server says: " + welcomeMessage);

            // 2. Send a fake test command to the server
            String testCommand = "BUY:AAPL:150:10";
            System.out.println("[TEST CLIENT] Sending command: " + testCommand);
            out.println(testCommand);

            // 3. Read the echo back from the server
            String response = in.readLine();
            System.out.println("Server replied: " + response);

            System.out.println("[TEST CLIENT] Test complete. Disconnecting.");

        } catch (IOException e) {
            System.err.println("[TEST CLIENT ERROR] Could not connect to server. Is it running?");
        }
    }
}