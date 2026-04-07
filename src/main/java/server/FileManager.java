package server;

import shared.Order;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Owns ledger file setup and will later handle trade persistence and state restore.
 */
public class FileManager {
    private static final String CSV_HEADER = "timestamp,buyOrderType,sellOrderType,ticker,price,quantity";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
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
                Files.writeString(ledgerPath, CSV_HEADER + System.lineSeparator(), StandardOpenOption.WRITE);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize ledger storage at " + ledgerPath.toAbsolutePath(), e);
        }
    }

    public synchronized void appendTrade(Order buyOrder, Order sellOrder, double tradePrice, int tradedQuantity) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String ledgerEntry = String.format(
                "%s,%s,%s,%s,%.2f,%d",
                timestamp,
                buyOrder.getType(),
                sellOrder.getType(),
                buyOrder.getTicker(),
                tradePrice,
                tradedQuantity
        );

        try (BufferedWriter writer = Files.newBufferedWriter(
                ledgerPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            writer.write(ledgerEntry);
            writer.newLine();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to append trade to ledger at " + ledgerPath.toAbsolutePath(), e);
        }
    }

    public Path getLedgerPath() {
        return ledgerPath;
    }
}
