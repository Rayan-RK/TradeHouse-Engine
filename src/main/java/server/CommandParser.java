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
            String[] parts = rawCommand.split(":");
            if (parts.length != 4 && parts.length != 5) {
                System.err.println("[PARSER ERROR] Invalid command format: " + rawCommand);
                return null;
            }

            String username = parts.length == 5 ? parts[0].toUpperCase() : Order.UNKNOWN_USER;
            int actionIndex = parts.length == 5 ? 1 : 0;

            String action = parts[actionIndex].toUpperCase();
            String ticker = parts[actionIndex + 1].toUpperCase();
            double price = Double.parseDouble(parts[actionIndex + 2]);
            int quantity = Integer.parseInt(parts[actionIndex + 3]);

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
