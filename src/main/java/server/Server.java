// The main entry point that listens for incoming connections and manages the thread pool, broadcasts, and market bots.
package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 8080; 
    private static final ExecutorService pool = Executors.newFixedThreadPool(100);
    private static final Path DATA_DIRECTORY = Paths.get("data");
    private static final Path LEDGER_PATH = DATA_DIRECTORY.resolve("ledger.csv");

    public static final OrderBook market = new OrderBook();
    public static final FileManager fileManager = new FileManager(LEDGER_PATH);

    // Thread-safe list of all active client output streams for real-time broadcasts
    public static final CopyOnWriteArrayList<PrintWriter> clientOutputs = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("[SERVER] TradeHouse Server is booting up...");
        fileManager.initializeStorage();
        List<TradeRecord> previousTrades = fileManager.loadTradeHistory();
        market.restoreLastKnownPrices(previousTrades);
        System.out.println("[SERVER] Loaded " + previousTrades.size() + " historical trade(s) from the ledger.");
        System.out.println("[SERVER] Restored last known prices for " + market.getCurrentPricesSnapshot().size() + " ticker(s).");

        // --- NEW: Start the Market Maker Bots ---
        // We run this in a separate thread so it doesn't block the Socket listener
        Thread botThread = new Thread(new BotEngine());
        botThread.setDaemon(true); 
        botThread.start();
        // -----------------------------------------

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Listening for connections on port " + PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] New Agent Connected: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler clientThread = new ClientHandler(clientSocket);
                pool.execute(clientThread); 
            }
        } catch (IOException e) {
            System.err.println("[SERVER FATAL ERROR] " + e.getMessage());
        }
    }

    /**
     * Sends a message to every single connected client simultaneously.
     */
    public static void broadcast(String message) {
        for (PrintWriter writer : clientOutputs) {
            writer.println(message);
        }
    }
}
