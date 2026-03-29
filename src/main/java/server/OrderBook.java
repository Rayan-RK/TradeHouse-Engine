// The central matching engine that safely stores and organizes active bids and asks.
package server;

import shared.Order;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderBook {
    // Thread-safe lists to hold active orders
    private final List<Order> buyOrders = Collections.synchronizedList(new ArrayList<>());
    private final List<Order> sellOrders = Collections.synchronizedList(new ArrayList<>());

    /**
     * Takes a newly parsed order and sorts it into the correct list.
     */
    public void processOrder(Order newOrder) {
        // Now using your getType() method!
        if (newOrder.getType().equals("BUY")) {
            buyOrders.add(newOrder);
            System.out.println("[ORDER BOOK] Added BUY order. Total Bids: " + buyOrders.size());
        } else if (newOrder.getType().equals("SELL")) {
            sellOrders.add(newOrder);
            System.out.println("[ORDER BOOK] Added SELL order. Total Asks: " + sellOrders.size());
        } else {
            System.err.println("[ORDER BOOK ERROR] Unknown type: " + newOrder.getType());
        }

        // Every time a new order comes in, we check if a trade can happen
        attemptMatch();
    }

    /**
     * The core trading algorithm. Sorts orders by best price and executes overlapping trades.
     */
    private synchronized void attemptMatch() {
        if (buyOrders.isEmpty() || sellOrders.isEmpty()) return;

        // 1. Sort lists: Highest Bids first, Lowest Asks first
        buyOrders.sort((b1, b2) -> Double.compare(b2.getPrice(), b1.getPrice()));
        sellOrders.sort((s1, s2) -> Double.compare(s1.getPrice(), s2.getPrice()));

        // 2. Loop through every buyer
        for (int i = 0; i < buyOrders.size(); i++) {
            Order bid = buyOrders.get(i);

            // 3. Look for a matching seller
            for (int j = 0; j < sellOrders.size(); j++) {
                Order ask = sellOrders.get(j);

                // Check: Same Ticker AND Buyer price >= Seller price
                if (bid.getTicker().equals(ask.getTicker()) && bid.getPrice() >= ask.getPrice()) {
                    
                    // Match found! Calculate how many shares actually trade
                    int tradedShares = Math.min(bid.getQuantity(), ask.getQuantity());

                    System.out.println("\n=========================================");
                    System.out.printf("[TRADE EXECUTED] %s: %d shares @ $%.2f%n", bid.getTicker(), tradedShares, ask.getPrice());
                    System.out.println("=========================================\n");

                    // Deduct the traded shares from both parties
                    bid.setQuantity(bid.getQuantity() - tradedShares);
                    ask.setQuantity(ask.getQuantity() - tradedShares);

                    // If the seller is out of shares, remove them from the book
                    if (ask.getQuantity() == 0) {
                        sellOrders.remove(j);
                        j--; // Adjust index so we don't skip the next item
                    }

                    // If the buyer is out of shares, remove them and break to the next buyer
                    if (bid.getQuantity() == 0) {
                        buyOrders.remove(i);
                        i--; // Adjust index
                        break; 
                    }
                }
            }
        }
    }
}