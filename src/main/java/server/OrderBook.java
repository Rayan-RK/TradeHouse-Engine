// The central matching engine that safely stores orders and broadcasts successful trades.
package server;

import shared.Order;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderBook {
    private final List<Order> buyOrders = Collections.synchronizedList(new ArrayList<>());
    private final List<Order> sellOrders = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Double> currentPrices = Collections.synchronizedMap(new HashMap<>());

    // Every new order is matched first, then the remaining open book is saved.
    public synchronized void processOrder(Order newOrder) {
        if (newOrder.getType().equals("BUY")) {
            buyOrders.add(newOrder);
        } else if (newOrder.getType().equals("SELL")) {
            sellOrders.add(newOrder);
        }
        attemptMatch();
        persistOpenOrders();
    }

    // Price-priority matching supports partial fills and notifies both clients after a trade.
    private void attemptMatch() {
        if (buyOrders.isEmpty() || sellOrders.isEmpty()) return;

        buyOrders.sort((b1, b2) -> Double.compare(b2.getPrice(), b1.getPrice()));
        sellOrders.sort((s1, s2) -> Double.compare(s1.getPrice(), s2.getPrice()));

        for (int i = 0; i < buyOrders.size(); i++) {
            Order bid = buyOrders.get(i);
            for (int j = 0; j < sellOrders.size(); j++) {
                Order ask = sellOrders.get(j);

                if (bid.getTicker().equals(ask.getTicker()) && bid.getPrice() >= ask.getPrice()) {
                    int tradedShares = Math.min(bid.getQuantity(), ask.getQuantity());
                    Server.accountManager.applyTrade(bid, ask, ask.getPrice(), tradedShares);
                    Server.fileManager.appendTrade(bid, ask, ask.getPrice(), tradedShares);
                    currentPrices.put(bid.getTicker(), ask.getPrice());

                    String broadcastMsg = String.format("MARKET_UPDATE:%s:%.2f", bid.getTicker(), ask.getPrice());
                    Server.broadcast(broadcastMsg);
                    Server.sendAccountUpdate(bid.getUsername());
                    Server.sendAccountUpdate(ask.getUsername());

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

        Server.fileManager.saveAccountState(Server.accountManager.getAccountsSnapshot());
    }

    public synchronized void restoreLastKnownPrices(List<TradeRecord> tradeHistory) {
        currentPrices.clear();
        for (TradeRecord tradeRecord : tradeHistory) {
            currentPrices.put(tradeRecord.getTicker(), tradeRecord.getPrice());
        }
    }

    public synchronized Map<String, Double> getCurrentPricesSnapshot() {
        return new HashMap<>(currentPrices);
    }

    public synchronized void publishMarketTick(String ticker, double price) {
        currentPrices.put(ticker, price);
        Server.broadcast(String.format("MARKET_UPDATE:%s:%.2f", ticker, price));
    }

    public synchronized void replaceBotLiquidity(String botUsername, List<Order> botOrders) {
        removeOrdersByUsername(buyOrders, botUsername, new ArrayList<>());
        removeOrdersByUsername(sellOrders, botUsername, new ArrayList<>());

        for (Order order : botOrders) {
            if ("BUY".equals(order.getType())) {
                buyOrders.add(copyOrder(order));
            } else if ("SELL".equals(order.getType())) {
                sellOrders.add(copyOrder(order));
            }
        }

        attemptMatch();
        persistOpenOrders();
    }

    // Heartbeat cleanup uses this to release reservations for users who disconnect.
    public synchronized void cancelOrdersForUser(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        List<Order> removedOrders = new ArrayList<>();
        int removedBuyOrders = removeOrdersByUsername(buyOrders, username, removedOrders);
        int removedSellOrders = removeOrdersByUsername(sellOrders, username, removedOrders);

        for (Order order : removedOrders) {
            Server.accountManager.releaseOrderReservation(order);
        }

        if (removedBuyOrders > 0 || removedSellOrders > 0) {
            System.out.println("[ORDER BOOK] Cancelled " + (removedBuyOrders + removedSellOrders)
                    + " open order(s) for disconnected user " + username + ".");
            persistOpenOrders();
            Server.fileManager.saveAccountState(Server.accountManager.getAccountsSnapshot());
        }
    }

    private int removeOrdersByUsername(List<Order> orders, String username, List<Order> removedOrders) {
        int before = orders.size();
        orders.removeIf(order -> {
            boolean shouldRemove = username.equalsIgnoreCase(order.getUsername());
            if (shouldRemove) {
                removedOrders.add(copyOrder(order));
            }
            return shouldRemove;
        });
        return before - orders.size();
    }

    public synchronized void restoreOpenOrders(List<Order> restoredOrders) {
        buyOrders.clear();
        sellOrders.clear();

        for (Order order : restoredOrders) {
            if ("BUY".equals(order.getType())) {
                buyOrders.add(copyOrder(order));
            } else if ("SELL".equals(order.getType())) {
                sellOrders.add(copyOrder(order));
            }
        }
    }

    public synchronized List<Order> getOpenOrdersSnapshot() {
        List<Order> snapshot = new ArrayList<>();
        for (Order order : buyOrders) {
            snapshot.add(copyOrder(order));
        }
        for (Order order : sellOrders) {
            snapshot.add(copyOrder(order));
        }
        return snapshot;
    }

    private void persistOpenOrders() {
        Server.fileManager.saveOpenOrders(getOpenOrdersSnapshot());
    }

    private Order copyOrder(Order order) {
        return new Order(order.getUsername(), order.getType(), order.getTicker(), order.getPrice(), order.getQuantity());
    }
}
