package server;

import shared.Order;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks server-side account balances and holdings.
 */
public class AccountManager {
    private final Map<String, UserAccount> accounts = new LinkedHashMap<>();

    public synchronized UserAccount login(String username, double startingBalance) {
        return accounts.computeIfAbsent(username.toUpperCase(), key -> new UserAccount(key, startingBalance));
    }

    public synchronized void restoreAccounts(List<UserAccount> restoredAccounts) {
        accounts.clear();
        for (UserAccount account : restoredAccounts) {
            accounts.put(account.getUsername(), account);
        }
    }

    public synchronized List<UserAccount> getAccountsSnapshot() {
        return new ArrayList<>(accounts.values());
    }

    public synchronized UserAccount getAccount(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return accounts.get(username.toUpperCase());
    }

    public synchronized UserAccount addFunds(String username, double amount) {
        UserAccount account = getAccount(username);
        if (account != null) {
            account.adjustCash(amount);
        }
        return account;
    }

    public synchronized String tryReserveForOrder(Order order) {
        if (Order.UNKNOWN_USER.equals(order.getUsername())) {
            return null;
        }

        UserAccount account = getAccount(order.getUsername());
        if (account == null) {
            return "User must LOGIN before placing orders.";
        }

        if ("BUY".equals(order.getType())) {
            double requiredCash = order.getPrice() * order.getQuantity();
            if (account.getAvailableCash() < requiredCash) {
                return "Insufficient funds for BUY order.";
            }
            account.reserveCash(requiredCash);
        } else if ("SELL".equals(order.getType())) {
            if (account.getAvailableHolding(order.getTicker()) < order.getQuantity()) {
                return "Insufficient holdings for SELL order.";
            }
            account.reserveHolding(order.getTicker(), order.getQuantity());
        }

        return null;
    }

    public synchronized void applyTrade(Order buyOrder, Order sellOrder, double tradePrice, int tradedQuantity) {
        if (!Order.UNKNOWN_USER.equals(buyOrder.getUsername())) {
            UserAccount buyer = getAccount(buyOrder.getUsername());
            if (buyer != null) {
                buyer.releaseCash(buyOrder.getPrice() * tradedQuantity);
                buyer.adjustCash(-(tradePrice * tradedQuantity));
                buyer.adjustHolding(buyOrder.getTicker(), tradedQuantity);
            }
        }

        if (!Order.UNKNOWN_USER.equals(sellOrder.getUsername())) {
            UserAccount seller = getAccount(sellOrder.getUsername());
            if (seller != null) {
                seller.releaseHolding(sellOrder.getTicker(), tradedQuantity);
                seller.adjustCash(tradePrice * tradedQuantity);
                seller.adjustHolding(sellOrder.getTicker(), -tradedQuantity);
            }
        }
    }

    public synchronized void releaseOrderReservation(Order order) {
        if (Order.UNKNOWN_USER.equals(order.getUsername())) {
            return;
        }

        UserAccount account = getAccount(order.getUsername());
        if (account == null) {
            return;
        }

        if ("BUY".equals(order.getType())) {
            account.releaseCash(order.getPrice() * order.getQuantity());
        } else if ("SELL".equals(order.getType())) {
            account.releaseHolding(order.getTicker(), order.getQuantity());
        }
    }

    public synchronized void rebuildReservations(List<Order> openOrders) {
        for (UserAccount account : accounts.values()) {
            account.clearReservations();
        }

        for (Order order : openOrders) {
            if (Order.UNKNOWN_USER.equals(order.getUsername())) {
                continue;
            }

            UserAccount account = getAccount(order.getUsername());
            if (account == null) {
                continue;
            }

            if ("BUY".equals(order.getType())) {
                account.reserveCash(order.getPrice() * order.getQuantity());
            } else if ("SELL".equals(order.getType())) {
                account.reserveHolding(order.getTicker(), order.getQuantity());
            }
        }
    }
}
