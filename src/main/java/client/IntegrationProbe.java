package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Deterministic socket smoke test for the final demo path.
 */
public class IntegrationProbe {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8080;
    private static final int WAIT_MILLIS = 8000;
    private static final Path DATA_DIR = Paths.get("data");

    public static void main(String[] args) throws Exception {
        String suffix = Long.toString(System.currentTimeMillis());
        String tradeTicker = "TP" + suffix;
        String openTicker = "TO" + suffix;
        String user = "PROBE" + suffix;
        long ledgerRowsBefore = countRows(DATA_DIR.resolve("ledger.csv"));

        try (ProbeClient buyer = new ProbeClient("buyer"); ProbeClient seller = new ProbeClient("seller")) {
            buyer.expect("CONNECTION_SUCCESS");
            seller.expect("CONNECTION_SUCCESS");

            buyer.send("BUY:" + tradeTicker + ":150.00:2");
            seller.send("SELL:" + tradeTicker + ":145.00:1");
            buyer.expect("MARKET_UPDATE:" + tradeTicker + ":145.00");
            seller.expect("MARKET_UPDATE:" + tradeTicker + ":145.00");
            buyer.expect("PING");

            buyer.send("LOGIN:" + user + ":1000");
            buyer.expect("ACCOUNT_UPDATE:" + user + ":1000.00:1000.00:NONE");
            buyer.send("BUY:" + openTicker + ":10.00:1");
            buyer.expect("ACCOUNT_UPDATE:" + user + ":1000.00:990.00:NONE");
            buyer.expect("SERVER_ACK");
        }

        require(countRows(DATA_DIR.resolve("ledger.csv")) > ledgerRowsBefore, "ledger.csv should append a trade");
        require(Files.readString(DATA_DIR.resolve("accounts.csv")).contains(user + ","),
                "accounts.csv should include " + user);
        require(Files.readString(DATA_DIR.resolve("open_orders.csv")).contains(user + ",BUY," + openTicker),
                "open_orders.csv should include the reserved order");

        System.out.println("[INTEGRATION PROBE] PASS multi-client broadcast, heartbeat, and persistence checks.");
    }

    private static long countRows(Path path) throws IOException {
        try (var lines = Files.lines(path)) {
            return lines.count();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static final class ProbeClient implements AutoCloseable {
        private final String name;
        private final Socket socket;
        private final PrintWriter out;
        private final BufferedReader in;

        private ProbeClient(String name) throws IOException {
            this.name = name;
            socket = new Socket(HOST, PORT);
            socket.setSoTimeout(1000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        private void send(String message) {
            out.println(message);
        }

        private void expect(String expected) throws IOException {
            long deadline = System.currentTimeMillis() + WAIT_MILLIS;
            while (System.currentTimeMillis() < deadline) {
                try {
                    String message = in.readLine();
                    if (message == null) {
                        break;
                    }
                    if ("PING".equalsIgnoreCase(message)) {
                        send("PONG");
                    }
                    if (message.contains(expected)) {
                        return;
                    }
                } catch (SocketTimeoutException e) {
                    // Keep waiting until the probe-level deadline expires.
                }
            }
            throw new IOException(name + " did not receive " + expected);
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
