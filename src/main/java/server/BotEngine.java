// A background thread that generates random market activity to simulate a live trading environment.
package server;

import shared.Order;
import java.util.Random;

public class BotEngine implements Runnable {
    private final Random random = new Random();
    
    // The list of stocks our bots will trade
    private final String[] tickers = {"AAPL", "MSFT", "TSLA", "GOOGL", "AMZN", "BTC"};

    @Override
    public void run() {
        System.out.println("[BOT ENGINE] Market Maker Bots are active...");

        while (true) {
            try {
                // Wait between 2 to 5 seconds before the next bot trade
                Thread.sleep(2000 + random.nextInt(3000));

                // Generate random trade data
                String ticker = tickers[random.nextInt(tickers.length)];
                String type = random.nextBoolean() ? "BUY" : "SELL";
                
                // Random price between $100 and $200
                double price = 100 + (100 * random.nextDouble());
                int quantity = 1 + random.nextInt(20);

                // Create the order object
                Order botOrder = new Order(type, ticker, price, quantity);

                // Drop it directly into the market!
                System.out.println("[BOT] Placed " + botOrder.toString());
                Server.market.processOrder(botOrder);

            } catch (InterruptedException e) {
                System.err.println("[BOT ENGINE ERROR] Bot loop interrupted.");
                break;
            }
        }
    }
}