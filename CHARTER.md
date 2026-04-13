# CSCI 2020U Project Charter & Work Contract

## 1. Project Overview

* **Project members:** Rayan Khan, Karan Bhaskar, Mohammad Ali Jafari
* **Product Name:** TradeHouse (High-Frequency Trading Exchange)
* **Project Description:** A multi-threaded, live-market simulation where users can buy and sell
  simulated assets in real-time.

  **Server:** Features an Order Matching Engine that processes incoming buy/sell orders using a
  priority-based matching algorithm. Automated "Market Maker" bot threads run concurrently to
  inject market volatility and simulate realistic price fluctuations. The server maintains a
  persistent order book and trade ledger via File I/O, and uses fault-tolerant socket heartbeats
  to detect and gracefully handle dropped client connections.

  **Client:** A JavaFX desktop application that connects to the server over a persistent socket
  connection. Each client instance allows a user to: (1) log in with a username and starting
  balance, (2) submit market and limit buy/sell orders through a trading panel, (3) view a live
  updating price chart that reflects real-time market data pushed from the server, (4) monitor
  their portfolio balance and executed trade history, and (5) receive audio cues when trades are
  confirmed or major market events occur. The client is stateless — all order validation,
  matching, and balance tracking are handled server-side.

* **Tech Stack:** Java, JavaFX (with Live Charting components), `java.net` Sockets for
  high-frequency data streaming, `java.lang.Thread` for bot and client concurrency, and
  File I/O for ledger and order-book persistence.

## 2. Team Work Contract

* **Communication Channel:** Discord
* **Meeting Schedule:** Every Monday after lecture, with a weekly code-merge sync on weekends.
* **Conflict Resolution:** If team members do not contribute to assigned tasks, we will contact
  the instructor immediately. All team members are also responsible to enable other members to
  execute their contributions (i.e., push the code needed, etc).

## 3. Work Division & Contribution Report

*Note: The "Actual Contribution" column has been updated for final review.
You may edit the "Task" column to reflect your own tasks.*

| Task / Module | Assigned Member (Plan) | Actual Contribution (Final) |
| :--- | :--- | :--- |
| **Multi-threaded Server (Bots)** | Rayan Khan | Implemented server startup, client thread handling, order-book matching flow, and market-maker bot support. |
| **Socket Networking (Order Engine)** | Rayan Khan | Implemented the socket protocol for login, buy/sell order submission, server acknowledgements, market updates, and heartbeat messages. |
| **GUI Implementation (Live Graphs)** | Karan Bhaskar | Implemented the Maven-based JavaFX dashboard, socket client, login/order controls, live chart updates, portfolio/account display, and activity table. |
| **Client Integration Plumbing** | Karan Bhaskar | Added client-side helper functions and GUI-to-server plumbing for the app menu, volatility/volume controls, add-funds flow, and session profit/loss display while preserving the assigned server, networking, and persistence ownership. |
| **Persistence (File I/O & Ledger)** | Mohammad Ali Jafari | Implemented CSV ledger/account/open-order storage and server startup restoration using server-side File I/O. |
| **UX/Sound Effects** | Karan Bhaskar | Implemented the modern dark/light JavaFX theme, inline user feedback, sound toggle, and generated success/error/market audio cues. |
| **Documentation/README (Heartbeats)** | Mohammad Ali Jafari | Documented persistence and heartbeat behavior; final README polish includes Maven run instructions, backend check usage, references, AI disclosure, and placeholders for screenshot/demo links. |

## 4. Final Contribution Status (Tag one at Final Submission)

At the end of the project, the team must agree on one of the following tags:

* **[x] (1) EVEN CONTRIBUTION:** All members met expectations from the original charter.
* **[ ] (2) UNEVEN CONTRIBUTION:** One or more members did not meet expectations.

*Note: The team should select one final contribution status before submission. Graders will use the "Actual Contribution" column above to apply uneven grades if necessary.*
