// The main entry point that listens for incoming connections and manages the thread pool, broadcasts, and market bots.
package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final int PORT = 8080;
    private static final String NO_BOTS_FLAG = "--no-bots";
    private static final long HEARTBEAT_INTERVAL_SECONDS = 5;
    private static final long HEARTBEAT_TIMEOUT_MILLIS = 15000;
    private static final ExecutorService pool = Executors.newFixedThreadPool(100);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Path DATA_DIRECTORY = Paths.get("data");
    private static final Path LEDGER_PATH = DATA_DIRECTORY.resolve("ledger.csv");
    private static final Path ACCOUNT_SNAPSHOT_PATH = DATA_DIRECTORY.resolve("accounts.csv");
    private static final Path OPEN_ORDERS_PATH = DATA_DIRECTORY.resolve("open_orders.csv");

    public static final OrderBook market = new OrderBook();
    public static final AccountManager accountManager = new AccountManager();
    public static final FileManager fileManager = new FileManager(LEDGER_PATH, ACCOUNT_SNAPSHOT_PATH, OPEN_ORDERS_PATH);

    // Thread-safe registries for active clients and their broadcast writers.
    public static final Map<Socket, ClientSession> activeClients = new ConcurrentHashMap<>();
    public static final CopyOnWriteArrayList<PrintWriter> clientOutputs = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("[SERVER] TradeHouse Server is booting up...");
        boolean startBots = shouldStartBots(args);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            restoreMarketState();
            startHeartbeatMonitor();

            if (startBots) {
                startMarketMakerBots();
            } else {
                System.out.println("[SERVER] Market Maker Bots disabled for deterministic testing.");
            }

            System.out.println("[SERVER] Listening for connections on port " + PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] New Agent Connected: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler clientThread = new ClientHandler(clientSocket);
                pool.execute(clientThread);
            }
        } catch (IOException e) {
            System.err.println("[SERVER FATAL ERROR] " + e.getMessage());
        } finally {
            shutdownServices();
        }
    }

    private static void restoreMarketState() {
        fileManager.initializeStorage();
        List<TradeRecord> previousTrades = fileManager.loadTradeHistory();
        List<UserAccount> restoredAccounts = fileManager.loadAccountState();
        List<shared.Order> restoredOpenOrders = fileManager.loadOpenOrders();

        accountManager.restoreAccounts(restoredAccounts);
        market.restoreOpenOrders(restoredOpenOrders);
        accountManager.rebuildReservations(restoredOpenOrders);
        market.restoreLastKnownPrices(previousTrades);
        System.out.println("[SERVER] Loaded " + previousTrades.size() + " historical trade(s) from the ledger.");
        System.out.println("[SERVER] Restored " + restoredAccounts.size() + " account snapshot(s).");
        System.out.println("[SERVER] Restored " + restoredOpenOrders.size() + " open order(s).");
        System.out.println("[SERVER] Restored last known prices for " + market.getCurrentPricesSnapshot().size() + " ticker(s).");
    }

    private static void startMarketMakerBots() {
        Thread botThread = new Thread(new BotEngine(), "market-maker-bots");
        botThread.setDaemon(true);
        botThread.start();
    }

    private static boolean shouldStartBots(String[] args) {
        for (String arg : args) {
            if (NO_BOTS_FLAG.equalsIgnoreCase(arg)) {
                return false;
            }
        }
        return true;
    }

    private static void shutdownServices() {
        scheduler.shutdownNow();
        pool.shutdownNow();
    }

    /**
     * Sends a message to every single connected client simultaneously.
     */
    public static void broadcast(String message) {
        for (PrintWriter writer : clientOutputs) {
            writer.println(message);
        }
    }

    public static void sendAccountUpdate(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        UserAccount account = accountManager.getAccount(username);
        if (account == null) {
            return;
        }

        Map<String, Integer> holdings = account.getHoldingsSnapshot();
        String holdingsText = holdings.isEmpty() ? "NONE" : holdings.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(java.util.stream.Collectors.joining("|"));
        String message = String.format("ACCOUNT_UPDATE:%s:%.2f:%.2f:%s",
                account.getUsername(), account.getCashBalance(), account.getAvailableCash(), holdingsText);

        for (ClientSession session : getActiveClientSessions()) {
            if (account.getUsername().equalsIgnoreCase(session.getUsername())) {
                session.sendMessage(message);
            }
        }
    }

    public static void registerClient(ClientSession session) {
        activeClients.put(session.getSocket(), session);
        clientOutputs.add(session.getWriter());
        System.out.println("[SERVER] Active client count: " + activeClients.size());
    }

    public static void removeClient(Socket clientSocket) {
        ClientSession session = activeClients.remove(clientSocket);
        if (session != null) {
            clientOutputs.remove(session.getWriter());
            System.out.println("[SERVER] Removed client session for " + session.getRemoteAddress());
        }
        System.out.println("[SERVER] Active client count: " + activeClients.size());
    }

    public static List<ClientSession> getActiveClientSessions() {
        return new ArrayList<>(activeClients.values());
    }

    private static void startHeartbeatMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();

            for (ClientSession session : getActiveClientSessions()) {
                session.sendMessage("PING");

                if (now - session.getLastPongTimeMillis() > HEARTBEAT_TIMEOUT_MILLIS) {
                    System.out.println("[HEARTBEAT] Client timed out: " + session.getRemoteAddress());
                    disconnectStaleClient(session);
                }
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private static void disconnectStaleClient(ClientSession session) {
        String username = session.getUsername();
        if (username != null && !username.isBlank()) {
            market.cancelOrdersForUser(username);
        }

        try {
            session.getSocket().close();
        } catch (IOException e) {
            System.err.println("[HEARTBEAT] Failed to close stale client socket: " + e.getMessage());
        }
    }
}
