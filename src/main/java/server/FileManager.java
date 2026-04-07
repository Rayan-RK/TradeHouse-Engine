package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize ledger storage at " + ledgerPath.toAbsolutePath(), e);
        }
    }

    public Path getLedgerPath() {
        return ledgerPath;
    }
}
