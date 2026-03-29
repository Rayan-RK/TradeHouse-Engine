// The central matching engine that safely stores orders and broadcasts successful trades.
package server;

import shared.Order;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderBook {
    private final List<Order> buyOrders = Collections.synchronizedList(new ArrayList<>());
    private final List<Order> sellOrders = Collections.synchronizedList(new ArrayList<>());

    public void processOrder(Order newOrder) {
        if (newOrder.getType().equals("BUY")) {
            buyOrders.add(newOrder);
        } else if (newOrder.getType().equals("SELL")) {
            sellOrders.add(newOrder);
        }
        attemptMatch();
    }

    private synchronized void attemptMatch() {
        if (buyOrders.isEmpty() || sellOrders.isEmpty()) return;

        buyOrders.sort((b1, b2) -> Double.compare(b2.getPrice(), b1.getPrice()));
        sellOrders.sort((s1, s2) -> Double.compare(s1.getPrice(), s2.getPrice()));

        for (int i = 0; i < buyOrders.size(); i++) {
            Order bid = buyOrders.get(i);
            for (int j = 0; j < sellOrders.size(); j++) {
                Order ask = sellOrders.get(j);

                if (bid.getTicker().equals(ask.getTicker()) && bid.getPrice() >= ask.getPrice()) {
                    int tradedShares = Math.min(bid.getQuantity(), ask.getQuantity());

                    // --- BROADCAST THE TRADE ---
                    String broadcastMsg = String.format("MARKET_UPDATE:%s:%.2f", bid.getTicker(), ask.getPrice());
                    Server.broadcast(broadcastMsg);
                    // ----------------------------

                    System.out.println("\n=========================================");
                    System.out.printf("[TRADE EXECUTED] %s: %d shares @ $%.2f%n", bid.getTicker(), tradedShares, ask.getPrice());
                    System.out.println("=========================================\n");

                    bid.setQuantity(bid.getQuantity() - tradedShares);
                    ask.setQuantity(ask.getQuantity() - tradedShares);

                    if (ask.getQuantity() == 0) { sellOrders.remove(j); j--; }
                    if (bid.getQuantity() == 0) { buyOrders.remove(i); i--; break; }
                }
            }
        }
    }
}