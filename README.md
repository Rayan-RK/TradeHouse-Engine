# TradeHouse (High-Frequency Trading Exchange)

TradeHouse is a multi-threaded, live-market simulation where users can buy and sell simulated assets in real-time. This project demonstrates a robust client-server architecture using Java Sockets, multi-threading, and a custom matching engine.

---

## 🏗️ Project Structure

- **`src/main/java/server/`**: Contains the `Server` (Socket listener), `ClientHandler` (Thread management), `CommandParser` (Protocol logic), and the `OrderBook` (Matching engine).
- **`src/main/java/shared/`**: Contains the `Order` model used by both the Client and Server.
- **`src/main/java/client/`**: Contains the `TestClient` used for backend validation.

---

## 🚀 Live Trade Matching Demo (Backend)

This demo confirms that the **Multi-threaded Server**, **Protocol Parser**, and **Matching Engine** are all functioning correctly according to the project requirements.

### 🛠️ How to Run the Demo

1. **Clean & Compile:**
   Open your terminal and run the following to wipe old classes and recompile:
   ```bash
   rm -rf bin/*
   javac -d bin src/main/java/shared/*.java src/main/java/server/*.java src/main/java/client/*.java
   ```
