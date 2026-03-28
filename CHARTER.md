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

*Note: The "Actual Contribution" column should be updated at the time of final submission.
You may edit the "Task" column to reflect your own tasks.*

| Task / Module | Assigned Member (Plan) | Actual Contribution (Final) |
| :--- | :--- | :--- |
| **Multi-threaded Server (Bots)** | Rayan Khan | [To be completed at final submission] |
| **Socket Networking (Order Engine)** | Rayan Khan | [To be completed at final submission] |
| **GUI Implementation (Live Graphs)** | Karan Bhaskar | [To be completed at final submission] |
| **Persistence (File I/O & Ledger)** | Mohammad Ali Jafari | [To be completed at final submission] |
| **UX/Sound Effects** | Karan Bhaskar | [To be completed at final submission] |
| **Documentation/README (Heartbeats)** | Mohammad Ali Jafari | [To be completed at final submission] |

## 4. Final Contribution Status (Tag one at Final Submission)

At the end of the project, the team must agree on one of the following tags:

* **[ ] (1) EVEN CONTRIBUTION:** All members met expectations from the original charter.
* **[ ] (2) UNEVEN CONTRIBUTION:** One or more members did not meet expectations.

*Note: Graders will use the "Actual Contribution" column above to apply uneven grades if necessary.*
