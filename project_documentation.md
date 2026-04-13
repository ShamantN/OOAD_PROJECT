# Extensive E-Commerce Nexus Architecture Document

This document serves as the complete technical and functional blueprint of the **E-Commerce Nexus** full-stack project. It strictly details the entire set of features, Object-Oriented Analysis and Design (OOAD) patterns applied, backend implementations, and frontend interfaces that have been developed.

---

## 1. System Architecture & Tech Stack
The project is built on a highly modular, decoupled architecture following strict **Layered MVC (Model-View-Controller)** principles.

*   **Frontend (View):** Vanilla HTML, CSS, JavaScript (Async/Await Fetch API). Kept lightweight and framework-free for educational clarity.
*   **Backend (Controller/Service):** Java 21, Spring Boot 3.3.1.
*   **Data Access (Repository):** Spring Data JPA (Hibernate ORM).
*   **Database (Model):** MySQL (`ecommerce_db`) with Auto-DDL enabled.

---

## 2. Full Feature Breakdown

### A. Security & Role-Based Access Control (RBAC)
*   **Auth Payload Mapping:** Users register as either `CUSTOMER` or `ADMIN`. The Spring Backend registers these properties.
*   **Simulated JWT / Header Injection:** Due to limits on implementing heavy Spring Security, the system uses a custom HTTP Header `Admin-User-Id` to simulate a stateless Authorization Token.
*   **Strict Middleware:** The `AdminController.java` utilizes a `verifyAdminAccess()` method that actively intercepts the header, queries the Database to ensure the ID exists, and validates that their assigned Role is an Administrator. If it isn't, they are permanently blocked from modifying products via a `403 Forbidden` / `400 Bad Request` proxy sequence.
*   **Frontend Routing Guards:** `admin.js` parses the browser's `localStorage` and surgically replaces the entire DOM tree rendering an error if a non-Admin attempts to view the page.

### B. Catalog & Product Management
*   **Product Creation:** Admins can create base `Product` templates mapping their name and financial cost.
*   **Inventory Isolation:** Products and Inventory are decoupled objects in the database. `Product` specifies *what* the item is, while `Inventory` specifies *where* it is and *how many* exist (e.g., Warehouse mapping).
*   **Restocking Logic:** `ProductService.java` manages safe mathematical increments to stock, guaranteeing no negative restocks (Validating `quantityToAdd > 0`).

### C. The JPQL Catalog Optimization
*   **Entity Abstraction:** Rather than sending the entire `Product` and `Inventory` entities (and all their internal data) to the web browser, the Backend maps the data to a `ProductCatalogDTO` (Data Transfer Object).
*   **Aggregated SQL Views:** The `ProductRepository` utilizes a heavy custom `JPQL` function:
    ```java
    @Query("SELECT new ...ProductCatalogDTO(...) FROM Inventory i JOIN i.product p GROUP BY p.productId HAVING SUM(i.stock) > 0")
    ```
    This completely isolates the end-user. Customers *only* see products that mathematically exist in stock, processed at the database query level rather than taking up memory in Java.

### D. The Shopping Engine (Order Service)
*   **Data Rehydration Security:** The frontend JSON payload only forwards `{ productId: 1, quantity: 2 }`. The Java backend refuses to trust the frontend's concept of price. `OrderService.java` securely fetches the true baseline `Product` from MySQL before accepting the transaction.
*   **Atomic Transactions:** The method uses `@Transactional`. If the system crashes mid-order because one item ran out of stock, the entire database rolls back safely without stealing the customer's inventory.
*   **Cart Price Aggregation:** The Service dynamically loops through the cart (`OrderItem`), invokes the `calculateItemTotal()` method built into the Object, and derives a total dynamically, eliminating pricing fraud.

### E. Finite State Machines & Logistics (Fulfillment Service)
Applying OOAD State Machine paradigms, the `Payment` and `Order` objects are locked in an unchangeable life-cycle.

1.  **State `CREATED`**: When an Order hits the database, the total is finalized. A pending $0.00 `Payment` record is bridged with `PaymentStatus.FAILED`.
2.  **State `PAID`**: An external "Simulated Payment Agent" strikes the `/{orderId}/pay` endpoint alongside a `amount` parameter. `PaymentService.java` mandates the amount must *mathematically match* the generated subset total exactly. Upon success, transitioning the payment to `SUCCESS` and Order to `PAID`.
3.  **State `SHIPPED` / `DELIVERED`**: The `FulfillmentService.java` is strict. It will inherently throw an Exception if an Admin attempts to "Deliver" an order that hasn't been "Shipped", or "Ship" an order that hasn't been "Paid". State jumping is illegal.

---

## 3. The Local Web Interface (Frontend)
The HTML logic utilizes modern reactive fetch paradigms split over three components:

1.  **`login.html` & `auth.js`**: Handles toggling and routing. Consumes raw JSON strings from Spring and traps Registration/Login parameters into the cache memory (`localStorage`).
2.  **`admin.html` & `admin.js`**: The secured gateway. The JavaScript creates a global wrapper `authenticatedAdminFetch()` ensuring every single subsequent button-press, whether it be updating inventory or fulfilling shipments, embeds the Administrator Auth Header over the HTTP channel seamlessly.
3.  **`index.html` & `catalog.js`**: Reconstructs the grid of available products based solely on the returned DTO array. It maps DOM IDs intelligently (e.g. `id="qty-${p.productId}"`) enabling instantaneous readouts of the User's desired quantities back to the JSON payload without requiring heavy state-management libraries like React.

---

## 4. Complete Database ERD (Entity Relationship) Layout
Everything maps directly back to the JPA annotated POJO's within Java.
*   **Users:** Holds Profiles, Passwords (plain-text for Sandbox), Roles.
*   **Products:** Contains canonical Price/Names.
*   **Inventory:** Serves as the junction entity between `Warehouse` and `Product`. 
*   **Orders & OrderItems:** `Orders` hold User PKs. `OrderItems` map a specific `Product` PK and `Quantity` to an `Order` PK. 
*   **Payments:** Holds exactly one 1-To-1 constraint back to an `Orders` entity.

### Complete Work Log
*   Initialized Spring Boot Project footprint and `.properties`.
*   Designed OOP Model relationships.
*   Configured `@RestController` API paths.
*   Diagnosed and mapped missing `productId` parameters to fix JSON parsing crashes.
*   Mapped Backend CORS Configurer to allow local frontends.
*   Built and stylized an independent Frontend ecosystem completely mirroring the Controller mappings.
