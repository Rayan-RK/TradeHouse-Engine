package server;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores one user's cash balance and holdings.
 */
public class UserAccount {
    public static final String CSV_HEADER = "username,cashBalance,holdings";

    private final String username;
    private double cashBalance;
    private final Map<String, Integer> holdings = new HashMap<>();
    private final Map<String, Integer> reservedHoldings = new HashMap<>();
    private double reservedCash;

    public UserAccount(String username, double cashBalance) {
        this.username = username.toUpperCase();
        this.cashBalance = cashBalance;
    }

    public String getUsername() {
        return username;
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public double getReservedCash() {
        return reservedCash;
    }

    public Map<String, Integer> getHoldingsSnapshot() {
        return new HashMap<>(holdings);
    }

    public double getAvailableCash() {
        return cashBalance - reservedCash;
    }

    public int getAvailableHolding(String ticker) {
        int held = holdings.getOrDefault(ticker, 0);
        int reserved = reservedHoldings.getOrDefault(ticker, 0);
        return held - reserved;
    }

    public void reserveCash(double amount) {
        reservedCash += amount;
    }

    public void releaseCash(double amount) {
        reservedCash = Math.max(0, reservedCash - amount);
    }

    public void reserveHolding(String ticker, int quantity) {
        reservedHoldings.merge(ticker, quantity, Integer::sum);
    }

    public void releaseHolding(String ticker, int quantity) {
        int updated = reservedHoldings.getOrDefault(ticker, 0) - quantity;
        if (updated <= 0) {
            reservedHoldings.remove(ticker);
        } else {
            reservedHoldings.put(ticker, updated);
        }
    }

    public void clearReservations() {
        reservedCash = 0;
        reservedHoldings.clear();
    }

    public void adjustCash(double delta) {
        cashBalance += delta;
    }

    public void adjustHolding(String ticker, int delta) {
        int updated = holdings.getOrDefault(ticker, 0) + delta;
        if (updated <= 0) {
            holdings.remove(ticker);
        } else {
            holdings.put(ticker, updated);
        }
    }

    public String toCsvRow() {
        StringBuilder holdingsBuilder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
            if (!first) {
                holdingsBuilder.append("|");
            }
            holdingsBuilder.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }

        return String.format("%s,%.2f,%s", username, cashBalance, holdingsBuilder);
    }

    public static UserAccount fromCsvRow(String csvRow) {
        String[] parts = csvRow.split(",", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid account snapshot row: " + csvRow);
        }

        UserAccount account = new UserAccount(parts[0], Double.parseDouble(parts[1]));
        if (!parts[2].isBlank()) {
            String[] positions = parts[2].split("\\|");
            for (String position : positions) {
                String[] positionParts = position.split("=");
                if (positionParts.length == 2) {
                    account.adjustHolding(positionParts[0].toUpperCase(), Integer.parseInt(positionParts[1]));
                }
            }
        }
        return account;
    }
}
