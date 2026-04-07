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

    public FileManager(Path ledgerPath) {
        this.ledgerPath = ledgerPath;
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

            if (Files.size(ledgerPath) == 0) {
                Files.writeString(ledgerPath, TradeRecord.CSV_HEADER + System.lineSeparator(), StandardOpenOption.WRITE);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize ledger storage at " + ledgerPath.toAbsolutePath(), e);
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
                if (line.isBlank() || line.equals(TradeRecord.CSV_HEADER)) {
                    continue;
                }
                tradeRecords.add(TradeRecord.fromCsvRow(line));
            }

            return tradeRecords;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load ledger history from " + ledgerPath.toAbsolutePath(), e);
        }
    }

    public Path getLedgerPath() {
        return ledgerPath;
    }
}
