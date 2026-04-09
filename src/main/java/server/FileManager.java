package server;

import shared.Order;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns ledger file setup and will later handle trade persistence and state restore.
 */
public class FileManager {
    private final Path ledgerPath;
    private final Path accountSnapshotPath;
    private final Path openOrdersPath;

    public FileManager(Path ledgerPath, Path accountSnapshotPath, Path openOrdersPath) {
        this.ledgerPath = ledgerPath;
        this.accountSnapshotPath = accountSnapshotPath;
        this.openOrdersPath = openOrdersPath;
    }

    public void initializeStorage() {
        try {
            Path parent = ledgerPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (Files.notExists(ledgerPath)) {
                Files.createFile(ledgerPath);
                System.out.println("[FILE MANAGER] Created ledger file at " + ledgerPath.toAbsolutePath());
            } else {
                System.out.println("[FILE MANAGER] Using ledger file at " + ledgerPath.toAbsolutePath());
            }

            initializeSnapshotFile(accountSnapshotPath, UserAccount.CSV_HEADER);
            initializeSnapshotFile(openOrdersPath, "username,type,ticker,price,quantity");

            if (Files.size(ledgerPath) == 0) {
                Files.writeString(ledgerPath, TradeRecord.CSV_HEADER + System.lineSeparator(), StandardOpenOption.WRITE);
            } else {
                migrateLedgerIfNeeded();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize ledger storage at " + ledgerPath.toAbsolutePath(), e);
        }
    }

    private void initializeSnapshotFile(Path snapshotPath, String header) throws IOException {
        if (Files.notExists(snapshotPath)) {
            Files.createFile(snapshotPath);
        }

        if (Files.size(snapshotPath) == 0) {
            Files.writeString(snapshotPath, header + System.lineSeparator(), StandardOpenOption.WRITE);
        }
    }

    public synchronized void appendTrade(Order buyOrder, Order sellOrder, double tradePrice, int tradedQuantity) {
        TradeRecord tradeRecord = TradeRecord.fromExecutedOrders(buyOrder, sellOrder, tradePrice, tradedQuantity);

        try (BufferedWriter writer = Files.newBufferedWriter(
                ledgerPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            writer.write(tradeRecord.toCsvRow());
            writer.newLine();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to append trade to ledger at " + ledgerPath.toAbsolutePath(), e);
        }
    }

    public List<TradeRecord> loadTradeHistory() {
        try {
            List<String> lines = Files.readAllLines(ledgerPath);
            List<TradeRecord> tradeRecords = new ArrayList<>();

            for (String line : lines) {
                if (line.isBlank() || line.startsWith("timestamp,")) {
                    continue;
                }
                tradeRecords.add(TradeRecord.fromCsvRow(line));
            }

            return tradeRecords;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load ledger history from " + ledgerPath.toAbsolutePath(), e);
        }
    }

    public synchronized void saveAccountState(List<UserAccount> accounts) {
        List<String> lines = new ArrayList<>();
        lines.add(UserAccount.CSV_HEADER);
        for (UserAccount account : accounts) {
            lines.add(account.toCsvRow());
        }
        writeSnapshot(accountSnapshotPath, lines);
    }

    public List<UserAccount> loadAccountState() {
        try {
            List<String> lines = Files.readAllLines(accountSnapshotPath);
            List<UserAccount> accounts = new ArrayList<>();
            for (String line : lines) {
                if (line.isBlank() || line.equals(UserAccount.CSV_HEADER)) {
                    continue;
                }
                accounts.add(UserAccount.fromCsvRow(line));
            }
            return accounts;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load account snapshot from " + accountSnapshotPath.toAbsolutePath(), e);
        }
    }

    public synchronized void saveOpenOrders(List<Order> orders) {
        List<String> lines = new ArrayList<>();
        lines.add("username,type,ticker,price,quantity");
        for (Order order : orders) {
            lines.add(String.format("%s,%s,%s,%.2f,%d",
                    order.getUsername(),
                    order.getType(),
                    order.getTicker(),
                    order.getPrice(),
                    order.getQuantity()));
        }
        writeSnapshot(openOrdersPath, lines);
    }

    public List<Order> loadOpenOrders() {
        try {
            List<String> lines = Files.readAllLines(openOrdersPath);
            List<Order> orders = new ArrayList<>();
            for (String line : lines) {
                if (line.isBlank() || line.startsWith("username,")) {
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length != 5) {
                    continue;
                }

                orders.add(new Order(
                        parts[0],
                        parts[1],
                        parts[2],
                        Double.parseDouble(parts[3]),
                        Integer.parseInt(parts[4])));
            }
            return orders;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load open order snapshot from " + openOrdersPath.toAbsolutePath(), e);
        }
    }

    private void writeSnapshot(Path path, List<String> lines) {
        try {
            Files.write(path, lines, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write snapshot file at " + path.toAbsolutePath(), e);
        }
    }

    private void migrateLedgerIfNeeded() throws IOException {
        List<String> lines = Files.readAllLines(ledgerPath);
        if (lines.isEmpty()) {
            return;
        }

        if (TradeRecord.CSV_HEADER.equals(lines.get(0))) {
            return;
        }

        List<TradeRecord> tradeRecords = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("timestamp,")) {
                continue;
            }
            tradeRecords.add(TradeRecord.fromCsvRow(line));
        }

        List<String> migratedLines = new ArrayList<>();
        migratedLines.add(TradeRecord.CSV_HEADER);
        for (TradeRecord tradeRecord : tradeRecords) {
            migratedLines.add(tradeRecord.toCsvRow());
        }

        Files.write(ledgerPath, migratedLines, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        System.out.println("[FILE MANAGER] Migrated legacy ledger format to include user columns.");
    }

    public Path getLedgerPath() {
        return ledgerPath;
    }
}
