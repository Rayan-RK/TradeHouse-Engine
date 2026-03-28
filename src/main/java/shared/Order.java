package shared;

import java.io.Serializable;

/**
 * This class represents a single Buy or Sell order.
 * It is 'Serializable' so it can eventually be sent over the network 
 */
public class Order implements Serializable {
    private String ticker;
    private double price;
    private int quantity;
    private String type; // "BUY" or "SELL"

    public Order(String type, String ticker, double price, int quantity) {
        this.type = type.toUpperCase();
        this.ticker = ticker.toUpperCase();
        this.price = price;
        this.quantity = quantity;
    }

    
    public String getTicker() { return ticker; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public String getType() { return type; }

    @Override
    public String toString() {
        return String.format("[%s] %d shares of %s at $%.2f", type, quantity, ticker, price);
    }
}