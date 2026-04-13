# TradeHouse

TradeHouse is a multi-threaded Java market simulation where multiple socket clients log in, place buy/sell orders, watch live market updates, and keep server-owned account state through CSV persistence. The project is built around the course requirements: JavaFX GUI, socket networking, server-side threading, and File I/O instead of a database.

## Collaborators

- Rayan Khan
- Karan Bhaskar
- Mohammad Ali Jafari

## Screenshot

TODO: Add a screenshot of the JavaFX dashboard running before final submission.

## Demo

TODO: Add the 2-5 minute demo video URL before final submission.

The recording should show server startup, at least two clients, login, order submission, live market updates, account/portfolio feedback, CSV persistence, and heartbeat behavior.

## Features

- Multi-client socket server on port `8080`
- Threaded client handlers and server-side market-maker bots
- Synchronized order book with partial fills and price-priority matching
- JavaFX trading dashboard with live charting, order ticket, account panel, activity table, dark/light theme toggle, and sound toggle
- Client socket reader thread that responds to heartbeat `PING` with `PONG`
- Server-driven account updates for cash, available cash, and holdings
- CSV persistence for completed trades, account snapshots, and open orders
- Deterministic integration probe for multi-client broadcast, heartbeat, and persistence checks

## Requirements

- JDK 25
- Maven 3.9+

Maven downloads JavaFX through the OpenJFX dependency, so no manual JavaFX installation is required.

## Clone And Build

```bash
git clone git@github.com:OntarioTech-CS-program/w26-csci2020u-finalproject-w26-team55.git
cd w26-csci2020u-finalproject-w26-team55
mvn clean compile
```

## Run

Start the server:

```bash
mvn exec:java -Dexec.mainClass=server.Server
```

Start the server without market-maker bots for deterministic testing:

```bash
mvn exec:java -Dexec.mainClass=server.Server -Dexec.args=--no-bots
```

Run the JavaFX GUI in another terminal:

```bash
mvn javafx:run
```

Run the console backend test client:

```bash
mvn exec:java -Dexec.mainClass=client.TestClient
```

Run the deterministic integration probe against a server started with `--no-bots`:

```bash
mvn exec:java -Dexec.mainClass=client.IntegrationProbe
```

The probe checks two-client market broadcasts, heartbeat `PING`/`PONG`, and CSV persistence evidence for the ledger, accounts, and open orders.

## Protocol

Client to server:

- `LOGIN:USERNAME:STARTING_BALANCE`
- `BUY:TICKER:PRICE:QUANTITY`
- `SELL:TICKER:PRICE:QUANTITY`
- `USERNAME:BUY:TICKER:PRICE:QUANTITY`
- `USERNAME:SELL:TICKER:PRICE:QUANTITY`
- `PONG`

Server to client:

- `CONNECTION_SUCCESS:...`
- `LOGIN_SUCCESS:USERNAME:CASH`
- `ACCOUNT_UPDATE:USERNAME:CASH:AVAILABLE_CASH:HOLDINGS`
- `SERVER_ACK:...`
- `SERVER_ERROR:...`
- `MARKET_UPDATE:TICKER:PRICE`
- `PING`

## Persistence

Runtime CSV files are created under `data/`:

- `data/ledger.csv`: completed trades
- `data/accounts.csv`: account balances and holdings
- `data/open_orders.csv`: open buy/sell orders

On startup, the server restores accounts, open orders, and last known market prices from those files. Runtime CSV files are ignored by Git so local demo/test data does not pollute commits.

## Project Structure

- `src/main/java/server/`: server, client handlers, order book, bots, persistence, heartbeat logic
- `src/main/java/shared/`: shared `Order` model
- `src/main/java/client/`: console validation clients and `IntegrationProbe`
- `src/main/java/client/gui/`: JavaFX GUI, socket client, and audio cues
- `src/main/resources/client/gui/`: JavaFX CSS theme
- `data/`: runtime CSV persistence directory

## Rubric Coverage

- Scope: multi-client trading simulation with GUI, live updates, persistence, and reliability behavior
- Technology Requirements: sockets, multi-threading, JavaFX GUI, and server-side File I/O are all used meaningfully
- README: project description, collaborators, run instructions, dependencies, references, AI disclosure, and media placeholders are included
- Video Demo: TODO placeholder is present; final link must be added after recording
- Overall Best Practices: Maven build, package organization, validation, error feedback, integration probe, and AI disclosure are included
- Project Functionality: login, order submission, matching, market broadcasts, account updates, persistence restore, and heartbeat behavior are implemented
- UI/UX: modern dashboard, live chart, theme toggle, sound toggle, inline feedback, portfolio panel, and activity history are implemented
- Solution Quality: the remaining presentation risk is adding the final screenshot and demo URL

## References And Resources

- Java Standard Library: `java.net.Socket`, `java.util.concurrent`, `java.nio.file`, and `javax.sound.sampled`
- JavaFX Controls and `LineChart` via OpenJFX
- OpenJFX Maven plugin for `mvn javafx:run`
- Maven compiler and exec plugins

## AI Usage Disclosure

AI assistance was used for UI color/theme ideas, CSS layout suggestions, socket integration troubleshooting, and project checklist planning. The team reviewed and tested the final work.
