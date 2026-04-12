# TradeHouse

TradeHouse is a multi-threaded Java client/server market simulation. The server accepts multiple socket clients, matches buy and sell orders, persists completed trades to a CSV ledger, restores market prices, account balances, and open orders on startup, and uses heartbeat checks to remove unresponsive clients.

## Project Structure

- `src/main/java/server/`: Server, client handlers, order book, bots, persistence, and heartbeat logic
- `src/main/java/shared/`: Shared `Order` model
- `src/main/java/client/`: `TestClient` backend validation client
- `src/main/java/client/gui/`: JavaFX GUI scaffold
- `data/`: Server-side ledger storage

## Compile

On Windows PowerShell:

```powershell
if (Test-Path bin) { Get-ChildItem bin -Recurse -File | Remove-Item -Force }
javac -d bin src/main/java/shared/*.java src/main/java/server/*.java src/main/java/client/*.java
```

## Run

Start the server in one terminal:

```powershell
java -cp bin server.Server
```

For deterministic GUI/network testing without automated market-maker orders:

```powershell
java -cp bin server.Server --no-bots
```

Start the backend test client in another terminal:

```powershell
java -cp bin client.TestClient
```

## JavaFX GUI Scaffold

The JavaFX client shell lives in `src/main/java/client/gui/TradeHouseApp.java`. It is a frontend-only scaffold at this stage: it opens the trading dashboard layout without requiring the server, and the socket connection is planned for the next implementation step.

JavaFX is not bundled with current JDKs, so install the JavaFX SDK and point `PATH_TO_FX` at its `lib` directory before compiling the GUI.

On Windows PowerShell:

```powershell
$env:PATH_TO_FX = "C:\path\to\javafx-sdk\lib"
javac --module-path $env:PATH_TO_FX --add-modules javafx.controls -d bin src/main/java/client/gui/*.java
java --module-path $env:PATH_TO_FX --add-modules javafx.controls -cp bin client.gui.TradeHouseApp
```

On macOS/Linux:

```bash
export PATH_TO_FX=/path/to/javafx-sdk/lib
javac --module-path "$PATH_TO_FX" --add-modules javafx.controls -d bin src/main/java/client/gui/*.java
java --module-path "$PATH_TO_FX" --add-modules javafx.controls -cp bin client.gui.TradeHouseApp
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

This repository currently contains the backend server, a socket test client, persistence logic, account restore logic, open-order restore logic, heartbeat fault-tolerance work, and an initial JavaFX GUI shell. The GUI is not connected to the server yet; socket integration is the next frontend step.
