# SecureATM - Project Documentation

## 1. Abstract
SecureATM is an enterprise-grade, Multi-Bank Desktop application built using Java Swing and SQLite. It demonstrates advanced concepts in Object-Oriented Programming (OOP) and software engineering by implementing a robust, unified platform capable of interacting with multiple simulated banking environments (such as SBI, HDFC, and ICICI). Unlike traditional terminal-based ATM projects, SecureATM features a dynamic, partitioned database architecture ensuring strict data isolation per bank, concurrent multithreading for seamless asynchronous transactions, and an automated session manager. The application boasts a responsive interface with dynamic, real-time theming, integrated localization, and high-quality vector graphics, providing a highly professional and modern fintech user experience.

## 2. Project Description
The SecureATM project aims to bridge the gap between academic Java assignments and real-world fintech software design. The core objective was to build a system where a single universal interface could securely connect to various isolated bank databases without cross-contaminating user credentials or transaction histories.

To achieve this, the software uses the **DAO (Data Access Object)** pattern to cleanly separate database operations from core business logic. All database tables are strictly partitioned by a composite key of `(customer_number, bank_name)`. This allows the application to simulate a real-world ATM hub where a user's SBI debit card accesses entirely different funds and history than their HDFC card, despite using the same interface.

From a user experience standpoint, the application features a deeply customized Java Swing GUI. The UI subsystem employs a powerful `BankThemeManager` that instantly morphs the application's color grading, corporate logos, and slogans to match the active bank. Performance and security are handled concurrently in the background; a dedicated thread pool processes all SQLite database transactions to prevent UI freezing, while a daemon-thread Session Manager silently tracks user activity to enforce automatic secure logouts.

## 3. Core Java & OOP Concepts Applied

The codebase aggressively leverages traditional Object-Oriented principles and advanced Java mechanics to ensure a highly modular and robust application:

### A. Encapsulation
- **Implementation:** Data structures like the `Account` POJO strictly protect their properties (balances, credentials). Modification is only possible through controlled, synchronized Service and DAO pathways, protecting the application against illegal state changes.
- **Where:** `com.atm.model.Account.java`

### B. Inheritance
- **Implementation:** Custom UI frameworks are built by extending base Java Swing classes. By inheriting from built-in graphical classes, the system safely introduces modern styling while retaining standard event-handling behaviors.
- **Where:** `LoginFrame` and `DashboardFrame` `extends JFrame`. The `AnimatedButton` `extends JButton`, and `RoundedPanel` `extends JPanel`.

### C. Polymorphism (Method Overriding)
- **Implementation:** Instead of settling for standard grey boxes, the software generates its own vector UI layers by fundamentally overriding how Java draws objects to the screen. High-resolution gradients, shadows, and watermarks are injected by hijacking the build-in paint loops.
- **Where:** Overriding `paintComponent(Graphics g)` in `LoginFrame.BrandPanel` and `RoundedPanel` to insert custom Java 2D Engine graphics and alpha-channel blending.

### D. System Abstraction
- **Implementation:** The underlying complexity of SQL statements, JDBC Connections, and raw ResultSets are completely abstracted away from the UI and Service layers. The higher layers simply call methods like `updateBalance()`, ignorant of the lower-level database mechanics.
- **Where:** `AccountDAO.java` and `DatabaseManager.java`

### E. Design Patterns
- **Singleton Pattern:** Used to ensure only a single instance of a critical system engine ever exists across the entire runtime. Prevents memory leaks and cross-thread connection pooling issues.
  - *Where:* `DatabaseManager.getInstance()` and `BankThemeManager.getInstance()`.
- **DAO Pattern (Data Access Object):** A structural pattern that isolates all Database access code from the rest of the application. 
  - *Where:* `AccountDAO.java`

### F. Advanced Mechanics (Concurrency & Multithreading)
- **Implementation:** True fintech apps do not freeze when talking to a server. SecureATM implements the `ExecutorService` thread pool to run database operations (like validating a PIN or transferring funds) on background worker threads. 
- **Session Tracking:** A Daemon thread (`ScheduledExecutorService`) relentlessly ticks in the background, counting down inactivity seconds independently of what the user is doing on the screen.
- **Where:** `TransactionDialog.java` (Worker Threads) and `SessionManager.java` (Daemon Threads).

## 4. Module Breakdown

The project is rigorously structured into distinct layers following standard MVC/Service architectural patterns:

### Module 1: The UI Layer (`com.atm.ui`)
Responsible for rendering the modern graphical interface and handling user interactivity.
- **`LoginFrame` / `BrandPanel`:** Implements a professional dual-column layout with dynamically rendered, color-graded bank watermarks and localized (English/Hindi) inputs.
- **`DashboardFrame`:** A responsive, light/dark mode sensitive dashboard displaying partitioned balances and contextual action buttons.
- **`TransactionDialog` / `TransactionHistoryDialog`:** Pop-up interfaces dynamically styling themselves to the active bank and rendering specific history queries.
- **`components/`:** Custom UI objects like `AnimatedButton` and `RoundedPanel` that provide high-end hover effects and modern aesthetics.

### Module 2: The Service Layer (`com.atm.service`)
Core business logic acting as the middleman between the UI and the Database.
- **`AuthService`:** Validates user credentials against specific bank partitions, ensuring secure access.
- **`TransactionService`:** Handles complex deposit and withdrawal logic, including real-time balance calculations and triggering the DAO audit logging.
- **`SessionManager`:** A background thread (daemon) manager that tracks user inactivity and safely terminates expired sessions to protect bank data.

### Module 3: Security & Persistence (`com.atm.db` & `com.atm.model`)
Handles object representation and isolated data persistence.
- **`AccountDAO`:** Implements the Data Access Object pattern to execute SQL queries. It enforces the strict `bank_name` partitioning to guarantee data isolation.
- **`DatabaseManager`:** Manages the Singleton lifecycle of the SQLite connection pool and dynamically injects seeded user data on first launch.
- **`Account`:** The plain old Java object (POJO) mapping the database columns into an immutable state for the running session.

### Module 4: Utilities & Resources (`com.atm.util`)
Helper classes that drive the application's dynamic features.
- **`BankThemeManager`:** A subsystem responsible for storing and broadcasting color palettes, logos (`/logos/`), and slogans across all UI frames.
- **`LanguageManager`:** Uses `ResourceBundle` to parse `.properties` files, enabling real-time switching between English and Hindi text.
- **`CurrencyUtil` & `AppLogger`:** Formats double values into readable strings and handles system-wide diagnostic logging safely without interrupting the UI.
