# E-Commerce Nexus - UML Analysis and Design Models

Based on your evaluation criteria image, you need specific quantities of diagrams. I have created all of them below using **Mermaid.js**, a standard text-to-diagram language that renders beautifully in Markdown viewers (like GitHub).

You can copy this exact file directly into your presentation or project documentation, or take screenshots of the rendered diagrams from this Artifact view.

---

## 1. Use Case Diagram (1 Required)

This diagram shows the system boundaries, the actors (Customer and Admin), and the use cases they perform.

```mermaid
usecaseDiagram
    actor Customer as "Customer"
    actor Admin as "Admin"
    
    rectangle "E-Commerce Nexus System" {
        usecase UC1 as "Browse Catalog"
        usecase UC2 as "Manage Shopping Cart"
        usecase UC3 as "Place Order"
        usecase UC4 as "Pay for Order"
        usecase UC5 as "Cancel Order"
        usecase UC6 as "View Order History"
        
        usecase UC7 as "Add New Product"
        usecase UC8 as "Restock Inventory"
        usecase UC9 as "Ship Order"
        usecase UC10 as "Deliver Order"
        usecase UC11 as "View Financial Metrics"
        usecase UC12 as "Login / Register"
    }
    
    Customer --> UC1
    Customer --> UC2
    Customer --> UC3
    Customer --> UC4
    Customer --> UC5
    Customer --> UC6
    Customer --> UC12
    
    Admin --> UC7
    Admin --> UC8
    Admin --> UC9
    Admin --> UC10
    Admin --> UC11
    Admin --> UC12
```

---

## 2. Class Diagram (1 Required)

This diagram outlines the core structural entities of the database model, their attributes, and multiplicity relationships (the ORM mappings).

```mermaid
classDiagram
    class User {
        -int userId
        -String name
        -String email
        -String password
        -Role role
    }
    
    class Product {
        -int productId
        -String name
        -double price
    }
    
    class Warehouse {
        -int warehouseId
        -String name
        -String location
    }
    
    class Inventory {
        -int inventoryId
        -int stockCount
        +isAvailable(int qty) boolean
        +reduceStock(int qty)
        +increaseStock(int qty)
    }
    
    class Order {
        -int orderId
        -double totalAmount
        -OrderStatus status
        +placeOrder()
        +cancelOrder()
    }
    
    class OrderItem {
        -int orderItemId
        -int quantity
        -double priceAtPurchase
        +calculateItemTotal() double
    }
    
    class Payment {
        -int paymentId
        -double amount
        -PaymentStatus status
    }
    
    class CancellationImpact {
        -int impactId
        -double refundableAmount
        -double cancellationFee
        -double deliveryChargeLoss
        -double finalRefund
        +calculateImpact(Order order)
    }

    User "1" --> "*" Order : places
    Order "1" *-- "*" OrderItem : contains
    OrderItem "*" --> "1" Product : references
    Inventory "*" --> "1" Product : tracks
    Inventory "*" --> "1" Warehouse : located in
    Order "1" -- "1" Payment : requires
    Order "1" -- "0..1" CancellationImpact : incurs penalty
```

---

## 3. Activity Diagrams (4 Required)

### Activity Diagram 1: Place Order Flow
```mermaid
stateDiagram-v2
    [*] --> BrowseCatalog
    BrowseCatalog --> AddToCart: Select Product
    AddToCart --> ViewCart
    ViewCart --> Checkout: Click Checkout
    
    state Checkout {
        CheckStock: Validate Inventory Stock
        CalculateTotal: Calculate Prices
        SaveOrder: Create Order Entity
    }
    
    Checkout --> Success: Stock Available
    Checkout --> Error: Out of Stock
    
    Error --> ViewCart
    Success --> OrderCreated
    OrderCreated --> [*]
```

### Activity Diagram 2: Order Fulfillment Flow (Admin)
```mermaid
stateDiagram-v2
    [*] --> LoginAsAdmin
    LoginAsAdmin --> ViewOrderBoard
    ViewOrderBoard --> CheckStatus
    
    CheckStatus --> ShipOrder: Order is PAID
    ShipOrder --> OrderShipped
    
    CheckStatus --> DeliverOrder: Order is SHIPPED
    DeliverOrder --> OrderDelivered
    
    OrderShipped --> ViewOrderBoard
    OrderDelivered --> [*]
```

### Activity Diagram 3: Cancel Order & Penalty Calculation Flow
```mermaid
stateDiagram-v2
    [*] --> RequestCancel
    RequestCancel --> CheckState
    
    CheckState --> RejectCancellation: State == DELIVERED
    CheckState --> RejectCancellation: State == CANCELLED
    
    CheckState --> CalculatePenalty: Valid State
    
    state CalculatePenalty {
        state if_state <<choice>>
        if_state --> RefundFull: CREATED or PAID
        if_state --> Deduct15PercentAnd10Loss: SHIPPED
    }
    
    CalculatePenalty --> RestoreInventory
    RestoreInventory --> PublishImpactMetrics
    PublishImpactMetrics --> [*]
```

### Activity Diagram 4: Restocking Inventory Flow
```mermaid
stateDiagram-v2
    [*] --> EnterRestockMenu
    EnterRestockMenu --> InputDetails: Enter ProductID & WarehouseID
    InputDetails --> ValidateProduct
    
    ValidateProduct --> Valid: Product exists
    ValidateProduct --> Invalid: Product not found
    
    Invalid --> EnterRestockMenu: Display Error
    
    Valid --> UpdateStock
    UpdateStock --> SaveToDatabase
    SaveToDatabase --> [*]
```

---

## 4. State Diagrams (4 Required)

### State Diagram 1: Core Order Lifecycle State Machine
This is the primary Finite State Machine implemented in your Java Service.
```mermaid
stateDiagram-v2
    [*] --> CREATED: placeOrder()
    
    CREATED --> PAID: processPayment()
    PAID --> SHIPPED: adminShip()
    SHIPPED --> DELIVERED: adminDeliver()
    
    CREATED --> CANCELLED: cancelOrder() (No Penalty)
    PAID --> CANCELLED: cancelOrder() (No Penalty)
    SHIPPED --> CANCELLED: cancelOrder() (Applies Penalty)
    
    DELIVERED --> [*]
    CANCELLED --> [*]
```

### State Diagram 2: Payment Lifecycle State Machine
```mermaid
stateDiagram-v2
    [*] --> FAILED: Auto-created with Order
    
    FAILED --> SUCCESS: User enters payment
    
    SUCCESS --> REFUNDED: Order is cancelled
    
    SUCCESS --> [*]
    REFUNDED --> [*]
```

### State Diagram 3: Inventory Stock State
```mermaid
stateDiagram-v2
    [*] --> InStock: Created with quantity > 0
    
    InStock --> OutOfStock: reduceStock() to 0
    OutOfStock --> InStock: adminRestock() or OrderCancelled
    
    InStock --> InStock: reduceStock() keeps > 0
    OutOfStock --> [*]: Product Discontinued
```

### State Diagram 4: Authentication Session State
```mermaid
stateDiagram-v2
    [*] --> LoggedOut
    
    LoggedOut --> Authenticating: Submit Login Form
    
    Authenticating --> LoggedIn: Valid Credentials
    Authenticating --> LoggedOut: Invalid (Show Error)
    
    LoggedIn --> ActiveSession: Store in localStorage
    ActiveSession --> LoggedOut: User Clicks Logout
    ActiveSession --> LoggedOut: Session Cleared
    
    LoggedOut --> [*]
```
