// Defines the shared data model representing a single Buy or Sell order across the network.
package shared;

import java.io.Serializable;

/**
 * This class represents a single Buy or Sell order.
 * It is 'Serializable' so it can eventually be sent over the network
 */
public class Order implements Serializable {
    public static final String UNKNOWN_USER = "UNKNOWN";

    private String username;
    private String ticker;
    private double price;
    private int quantity;
    private String type; // "BUY" or "SELL"

    public Order(String type, String ticker, double price, int quantity) {
        this(UNKNOWN_USER, type, ticker, price, quantity);
    }

    public Order(String username, String type, String ticker, double price, int quantity) {
        this.username = username.toUpperCase();
        this.type = type.toUpperCase();
        this.ticker = ticker.toUpperCase();
        this.price = price;
        this.quantity = quantity;
    }

    public String getUsername() { return username; }
    public String getTicker() { return ticker; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public String getType() { return type; }

    // Allows the Order Book to deduct shares after a trade
    public void setQuantity(int newQuantity) { this.quantity = newQuantity; }

    @Override
    public String toString() {
        return String.format("[%s] %s placed %d shares of %s at $%.2f", type, username, quantity, ticker, price);
    }
}
