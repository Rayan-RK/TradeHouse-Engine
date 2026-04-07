package server;

import shared.Order;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents one completed trade stored in the ledger.
 */
public class TradeRecord {
    public static final String CSV_HEADER = "timestamp,buyerUsername,sellerUsername,buyOrderType,sellOrderType,ticker,price,quantity";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final LocalDateTime timestamp;
    private final String buyerUsername;
    private final String sellerUsername;
    private final String buyOrderType;
    private final String sellOrderType;
    private final String ticker;
    private final double price;
    private final int quantity;

    public TradeRecord(LocalDateTime timestamp, String buyerUsername, String sellerUsername, String buyOrderType, String sellOrderType, String ticker, double price, int quantity) {
        this.timestamp = timestamp;
        this.buyerUsername = buyerUsername;
        this.sellerUsername = sellerUsername;
        this.buyOrderType = buyOrderType;
        this.sellOrderType = sellOrderType;
        this.ticker = ticker;
        this.price = price;
        this.quantity = quantity;
    }

    public static TradeRecord fromExecutedOrders(Order buyOrder, Order sellOrder, double tradePrice, int tradedQuantity) {
        return new TradeRecord(
                LocalDateTime.now(),
                buyOrder.getUsername(),
                sellOrder.getUsername(),
                buyOrder.getType(),
                sellOrder.getType(),
                buyOrder.getTicker(),
                tradePrice,
                tradedQuantity
        );
    }

    public static TradeRecord fromCsvRow(String csvRow) {
        String[] parts = csvRow.split(",");
        if (parts.length == 6) {
            return new TradeRecord(
                    LocalDateTime.parse(parts[0], TIMESTAMP_FORMAT),
                    Order.UNKNOWN_USER,
                    Order.UNKNOWN_USER,
                    parts[1],
                    parts[2],
                    parts[3],
                    Double.parseDouble(parts[4]),
                    Integer.parseInt(parts[5])
            );
        }

        if (parts.length != 8) {
            throw new IllegalArgumentException("Invalid ledger row: " + csvRow);
        }

        return new TradeRecord(
                LocalDateTime.parse(parts[0], TIMESTAMP_FORMAT),
                parts[1],
                parts[2],
                parts[3],
                parts[4],
                parts[5],
                Double.parseDouble(parts[6]),
                Integer.parseInt(parts[7])
        );
    }

    public String toCsvRow() {
        return String.format(
                "%s,%s,%s,%s,%s,%s,%.2f,%d",
                timestamp.format(TIMESTAMP_FORMAT),
                buyerUsername,
                sellerUsername,
                buyOrderType,
                sellOrderType,
                ticker,
                price,
                quantity
        );
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getBuyerUsername() {
        return buyerUsername;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public String getBuyOrderType() {
        return buyOrderType;
    }

    public String getSellOrderType() {
        return sellOrderType;
    }

    public String getTicker() {
        return ticker;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }
}
