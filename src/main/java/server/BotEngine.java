// A background thread that generates random market activity to simulate a live trading environment.
package server;

import shared.Order;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class BotEngine implements Runnable {
    private static final String BOT_USERNAME = "BOT";
    private static final long TICK_INTERVAL_MILLIS = 1000;
    private static final int BOT_QUANTITY = 100;
    private static final double NORMAL_VOLATILITY = 0.006;
    private static final double WILD_VOLATILITY = 0.04;
    private static volatile double volatilityFactor = NORMAL_VOLATILITY;
    private static volatile String volatilityMode = "NORMAL";

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
        double drift = (random.nextDouble() - 0.5) * previous * volatilityFactor;
        double next = roundPrice(Math.max(0.01, previous + drift));
        prices.put(ticker, next);
        return next;
    }

    public static boolean setVolatilityMode(String mode) {
        String normalizedMode = mode == null ? "" : mode.trim().toUpperCase(Locale.ROOT);
        if ("WILD".equals(normalizedMode)) {
            volatilityFactor = WILD_VOLATILITY;
            volatilityMode = "WILD";
            return true;
        }
        if ("NORMAL".equals(normalizedMode)) {
            volatilityFactor = NORMAL_VOLATILITY;
            volatilityMode = "NORMAL";
            return true;
        }
        return false;
    }

    public static String getVolatilityMode() {
        return volatilityMode;
    }

    private double roundPrice(double price) {
        return Math.round(price * 100.0) / 100.0;
    }
}
