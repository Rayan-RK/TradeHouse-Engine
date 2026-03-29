// The central matching engine that safely stores and organizes active active bids and asks.
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
     * Checks if the highest buyer is willing to pay the lowest seller's price.
     */
    private synchronized void attemptMatch() {
        // MVP logic: Just checking if we have at least one buyer and one seller
        if (!buyOrders.isEmpty() && !sellOrders.isEmpty()) {
            System.out.println("[ORDER BOOK ENGINE] Buyers and Sellers detected. Ready to match prices!");
            // We will add the actual price-matching math in the next commit!
        }
    }
}