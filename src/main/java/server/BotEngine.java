// A background thread that generates random market activity to simulate a live trading environment.
package server;

import shared.Order;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BotEngine implements Runnable {
    private static final String BOT_USERNAME = "BOT";
    private static final long TICK_INTERVAL_MILLIS = 1000;
    private static final int BOT_QUANTITY = 100;

    private final Random random = new Random();
    private final String[] tickers = {"AAPL", "MSFT", "TSLA", "GOOGL", "AMZN", "BTC"};
    private final Map<String, Double> prices = new HashMap<>(Map.of(
            "AAPL", 150.00,
            "MSFT", 420.00,
            "TSLA", 250.00,
            "GOOGL", 175.00,
            "AMZN", 185.00,
            "BTC", 65000.00));

    @Override
    public void run() {
        System.out.println("[BOT ENGINE] Market Maker Bots are active...");

        while (true) {
            try {
                Thread.sleep(TICK_INTERVAL_MILLIS);

                List<Order> botOrders = new ArrayList<>();
                for (String ticker : tickers) {
                    double price = nextPrice(ticker);
                    double spread = Math.max(0.25, price * 0.002);

                    Server.market.publishMarketTick(ticker, price);
                    botOrders.add(new Order(BOT_USERNAME, "BUY", ticker, roundPrice(price - spread), BOT_QUANTITY));
                    botOrders.add(new Order(BOT_USERNAME, "SELL", ticker, roundPrice(price + spread), BOT_QUANTITY));
                }

                Server.market.replaceBotLiquidity(BOT_USERNAME, botOrders);
            } catch (InterruptedException e) {
                System.err.println("[BOT ENGINE ERROR] Bot loop interrupted.");
                break;
            }
        }
    }

    private double nextPrice(String ticker) {
        double previous = prices.getOrDefault(ticker, 150.00);
        double drift = (random.nextDouble() - 0.5) * previous * 0.004;
        double next = roundPrice(Math.max(0.01, previous + drift));
        prices.put(ticker, next);
        return next;
    }

    private double roundPrice(double price) {
        return Math.round(price * 100.0) / 100.0;
    }
}
