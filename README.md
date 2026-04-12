# TradeHouse

TradeHouse is a multi-threaded Java client/server market simulation. The server accepts multiple socket clients, matches buy and sell orders, persists completed trades to a CSV ledger, restores market prices, account balances, and open orders on startup, and uses heartbeat checks to remove unresponsive clients.

## Project Structure

- `src/main/java/server/`: Server, client handlers, order book, bots, persistence, and heartbeat logic
- `src/main/java/shared/`: Shared `Order` model
- `src/main/java/client/`: `TestClient` backend validation client
- `src/main/java/client/gui/`: JavaFX GUI and socket client
- `src/main/resources/client/gui/`: JavaFX CSS theme resources
- `data/`: Server-side ledger storage

## Compile

Requirements:

- JDK 25
- Maven 3.9+

Maven downloads JavaFX and compiles the server, shared models, test client, and GUI:

```bash
mvn clean compile
```

## Run

Start the server in one terminal:

```bash
mvn exec:java -Dexec.mainClass=server.Server
```

For deterministic GUI/network testing without automated market-maker orders:

```bash
mvn exec:java -Dexec.mainClass=server.Server -Dexec.args=--no-bots
```

Start the backend test client in another terminal:

```bash
mvn exec:java -Dexec.mainClass=client.TestClient
```

## JavaFX GUI

The JavaFX client lives in `src/main/java/client/gui/TradeHouseApp.java`. It can connect to the local server, send `LOGIN` requests, validate and submit basic `BUY`/`SELL` orders, listen for server responses on a background thread, and reply to heartbeat `PING` messages with `PONG`.

The dashboard uses a modern JavaFX theme with dark/light mode toggle, live market status cards, an order ticket, a market chart, inline order feedback, and one activity table for market/order updates. Richer account/portfolio feedback and UX polish are planned for the next frontend steps.

Run the GUI:

```bash
mvn javafx:run
```

## Supported Order Formats

The server currently accepts both formats below:

- `LOGIN:MOHAMMAD:10000`
- `BUY:AAPL:150.00:10`
- `MOHAMMAD:BUY:AAPL:150.00:10`

`LOGIN` creates or restores a server-side account. The username-prefixed order format is used by the persistence and heartbeat cleanup logic to associate orders with a client identity.

## Persistence

- Completed trades are appended to `data/ledger.csv`
- If the ledger does not exist, the server creates it on startup
- If an older ledger format is found, the server migrates it to the current CSV schema
- Server-side balances and holdings are stored in `data/accounts.csv`
- Open orders are stored in `data/open_orders.csv`
- On startup, the server restores account snapshots, open orders, and the last known traded price for each ticker

## Heartbeats

- The server sends `PING` to active clients every 5 seconds
- Clients should reply with `PONG`
- If a client does not respond within 15 seconds, the server disconnects it
- If that client submitted user-tagged orders, the server cancels that user's open orders

## Demo Checklist

For a milestone demo or recording, show:

1. Server booting and restoring the ledger, accounts, and open orders
2. A client logging in with `LOGIN:USERNAME:STARTING_BALANCE`
3. A successful trade that produces a `MARKET_UPDATE`
4. `data/ledger.csv`, `data/accounts.csv`, and `data/open_orders.csv` updating
5. A server restart restoring account state and open orders
6. A client stopping heartbeat responses and being removed by the server

## Current Scope

This repository currently contains the backend server, a socket test client, persistence logic, account restore logic, open-order restore logic, heartbeat fault-tolerance work, and a Maven-based JavaFX GUI with socket login/order submission, local order validation, recent market activity, live market updates, and dark/light mode. Richer portfolio feedback and final demo documentation are the next frontend steps.
