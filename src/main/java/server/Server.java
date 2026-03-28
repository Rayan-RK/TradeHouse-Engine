package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    // Standard port for our TradeHouse server
    private static final int PORT = 8080; 
    
    // A thread pool handles multiple concurrent clients efficiently
    private static final ExecutorService pool = Executors.newFixedThreadPool(100);

    public static void main(String[] args) {
        System.out.println("[SERVER] TradeHouse Server is booting up...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Listening for connections on port " + PORT + "...");

            // Infinite loop to constantly accept new clients
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] New Agent Connected: " + clientSocket.getInetAddress().getHostAddress());

                // Hand the connection off to our new ClientHandler thread
                ClientHandler clientThread = new ClientHandler(clientSocket);
                pool.execute(clientThread); 
            }
        } catch (IOException e) {
            System.err.println("[SERVER FATAL ERROR] " + e.getMessage());
        }
    }
}