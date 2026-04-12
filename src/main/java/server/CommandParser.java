// Translates raw text strings received over the socket into valid Java Order objects.
package server;

import shared.Order;

public class CommandParser {

    /**
     * Translates a raw socket string into an Order object.
     * Supported formats:
     * "ACTION:TICKER:PRICE:QUANTITY" (legacy)
     * "USERNAME:ACTION:TICKER:PRICE:QUANTITY"
     */
    public static Order parseOrder(String rawCommand) {
        try {
            String[] parts = rawCommand.split(":", -1);
            if (parts.length != 4 && parts.length != 5) {
                System.err.println("[PARSER ERROR] Invalid command format: " + rawCommand);
                return null;
            }

            String username = parts.length == 5 ? parts[0].trim().toUpperCase() : Order.UNKNOWN_USER;
            int actionIndex = parts.length == 5 ? 1 : 0;

            if (parts.length == 5 && username.isBlank()) {
                System.err.println("[PARSER ERROR] Username cannot be blank: " + rawCommand);
                return null;
            }

            String action = parts[actionIndex].trim().toUpperCase();
            String ticker = parts[actionIndex + 1].trim().toUpperCase();
            double price = Double.parseDouble(parts[actionIndex + 2].trim());
            int quantity = Integer.parseInt(parts[actionIndex + 3].trim());

            if (!"BUY".equals(action) && !"SELL".equals(action)) {
                System.err.println("[PARSER ERROR] Unsupported order action: " + action);
                return null;
            }

            if (ticker.isBlank()) {
                System.err.println("[PARSER ERROR] Ticker cannot be blank: " + rawCommand);
                return null;
            }

            if (Double.isNaN(price) || Double.isInfinite(price) || price <= 0) {
                System.err.println("[PARSER ERROR] Price must be greater than zero: " + rawCommand);
                return null;
            }

            if (quantity <= 0) {
                System.err.println("[PARSER ERROR] Quantity must be greater than zero: " + rawCommand);
                return null;
            }

            return new Order(username, action, ticker, price, quantity);

        } catch (NumberFormatException e) {
            System.err.println("[PARSER ERROR] Invalid numbers in command: " + rawCommand);
            return null;
        } catch (Exception e) {
            System.err.println("[PARSER ERROR] Unexpected error parsing: " + rawCommand);
            return null;
        }
    }
}
