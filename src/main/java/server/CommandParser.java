// Translates raw text strings received over the socket into valid Java Order objects.
package server;

import shared.Order;

public class CommandParser {

    /**
     * Translates a raw socket string into an Order object.
     * Expected format: "ACTION:TICKER:PRICE:QUANTITY" (e.g., "BUY:AAPL:150.00:10")
     */
    public static Order parseOrder(String rawCommand) {
        try {
            // Split the string by the colon character
            String[] parts = rawCommand.split(":");

            // Check if we have exactly 4 pieces of data
            if (parts.length != 4) {
                System.err.println("[PARSER ERROR] Invalid command format: " + rawCommand);
                return null;
            }

            // Extract and convert the data
            String action = parts[0].toUpperCase();
            String ticker = parts[1].toUpperCase();
            double price = Double.parseDouble(parts[2]);
            int quantity = Integer.parseInt(parts[3]);

            // Create and return the new Order object
            return new Order(action, ticker, price, quantity);

        } catch (NumberFormatException e) {
            System.err.println("[PARSER ERROR] Invalid numbers in command: " + rawCommand);
            return null;
        } catch (Exception e) {
            System.err.println("[PARSER ERROR] Unexpected error parsing: " + rawCommand);
            return null;
        }
    }
}