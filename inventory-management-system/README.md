# Inventory Management System — Low Level Design

An e-commerce order + inventory system. Multiple warehouses hold stock, a customer
is routed to the nearest one, fills a cart, places an order and pays. Stock is
deducted at checkout and **rolled back if the payment fails**.

## UML Class Diagram

```
┌─────────────────────┐   ┌──────────────────────────────┐
│      <<enum>>       │   │        <<interface>>         │
│     OrderStatus     │   │         PaymentMode          │
│─────────────────────│   │──────────────────────────────│
│  CREATED            │   │ + makePayment(amount): bool  │
│  CONFIRMED          │   └──────────────────────────────┘
│  CANCELLED          │        ▲                 ▲
│  DELIVERED          │        │                 │
│  RETURNED           │   ┌─────────────┐  ┌──────────────┐
└─────────────────────┘   │UPIPaymentMode│ │CardPaymentMode│
                          └─────────────┘  └──────────────┘

┌────────────────────────────────────────┐
│        <<interface>>                   │
│    WarehouseSelectionStrategy          │
│────────────────────────────────────────│
│ + selectWarehouse(list): Warehouse     │
└────────────────────────────────────────┘
                 ▲
                 │
┌────────────────────────────────────────┐
│   NearestWarehouseSelectionStrategy    │  ← matches customer city
│────────────────────────────────────────│
│ - deliveryAddress: Address             │
└────────────────────────────────────────┘

┌────────────────────────────────────────────────────────┐
│                 ProductDeliverySystem                  │  ← FACADE, entry point
│────────────────────────────────────────────────────────│
│ - userController: UserController                       │
│ - warehouseController: WarehouseController             │
│ - orderController: OrderController                     │
│────────────────────────────────────────────────────────│
│ + getUser(userId): User                                │
│ + selectWarehouse(strategy): Warehouse                 │
│ + findProduct(warehouse, name): Product                │
│ + addProductToCart(user, product, count): boolean      │  ← early stock check
│ + placeOrder(user, warehouse): Order                   │
│ + checkout(order, paymentMode): void                   │
│ + receiveStock(whId, productId, count): void           │
│ + printAllInventory() / printOrderHistory(userId)      │
└────────────────────────────────────────────────────────┘
        │                    │                     │
        ▼                    ▼                     ▼
┌──────────────────┐ ┌────────────────────────┐ ┌──────────────────────────────┐
│  UserController  │ │  WarehouseController   │ │       OrderController        │
│──────────────────│ │────────────────────────│ │──────────────────────────────│
│ - users: Map     │ │ - warehouses: List     │ │ - orders: List<Order>        │
│──────────────────│ │────────────────────────│ │ - userIdVsOrders: Map        │
│ + addUser        │ │ + addWarehouse         │ │ - orderIdCounter: int        │
│ + removeUser     │ │ + removeWarehouse      │ │──────────────────────────────│
│ + getUser(id)    │ │ + getWarehouse(id)     │ │ + createNewOrder(user, wh)   │
└──────────────────┘ │ + selectWarehouse(str) │ │ + getOrdersByUserId(userId)  │
                     └────────────────────────┘ │ + getOrderById(orderId)      │
                                                └──────────────────────────────┘

┌──────────────────────────────┐        ┌──────────────────────────────────────┐
│            User              │        │              Warehouse               │
│──────────────────────────────│        │──────────────────────────────────────│
│ - userId: int                │        │ - warehouseId: int                   │
│ - userName: String           │        │ - address: Address                   │
│ - address: Address           │        │ - inventory: Inventory               │
│ - cart: Cart                 │        │──────────────────────────────────────│
│ - orderIds: List<Integer>    │        │ + canFulfill(items): boolean         │
└──────────────────────────────┘        │ + removeItemsFromInventory(items)    │
            │ has one                   │ + addItemsToInventory(items)         │
            ▼                           └──────────────────────────────────────┘
┌──────────────────────────────────────┐             │ has one
│               Cart                   │             ▼
│──────────────────────────────────────│ ┌──────────────────────────────────────┐
│ - productIdVsCount: Map<Int,Int>     │ │              Inventory               │
│──────────────────────────────────────│ │──────────────────────────────────────│
│ + addItem(productId, count)          │ │ - products: Map<Integer, Product>    │
│ + removeItem(productId, count)       │ │──────────────────────────────────────│
│ + getCount(productId): int           │ │ + addProduct(p) / addStock(id, n)    │
│ + emptyCart() / isEmpty()            │ │ + getProductById / getProductByName  │
│ + getItems(): Map<Int,Int>           │ │ + hasStockFor(items): boolean        │
└──────────────────────────────────────┘ │ + removeItems(items)                 │
                                         │ + addItems(items)      ← rollback    │
                                         └──────────────────────────────────────┘
                                                        │ holds many
                                                        ▼
                                         ┌──────────────────────────────────────┐
                                         │               Product                │
                                         │──────────────────────────────────────│
                                         │ - productId: int                     │
                                         │ - name: String                       │
                                         │ - price: double                      │
                                         │ - quantity: int      ← stock count   │
                                         │──────────────────────────────────────│
                                         │ + hasStock(count): boolean           │
                                         │ + setQuantity(q): void               │
                                         └──────────────────────────────────────┘

┌───────────────────────────────────────────────┐
│                     Order                     │
│───────────────────────────────────────────────│
│ - orderId: int            (1001, 1002, ...)   │
│ - user: User                                  │
│ - warehouse: Warehouse                        │
│ - deliveryAddress: Address                    │
│ - items: Map<Integer,Integer>  ← cart snapshot│
│ - invoice: Invoice                            │
│ - payment: Payment                            │
│ - orderStatus: OrderStatus                    │
│───────────────────────────────────────────────│
│ + checkout(paymentMode): void   ← transaction │
└───────────────────────────────────────────────┘
        │ billed by                │ paid by
        ▼                          ▼
┌──────────────────────────────┐ ┌──────────────────────────────┐
│           Invoice            │ │           Payment            │
│──────────────────────────────│ │──────────────────────────────│
│ - TAX_RATE = 0.18            │ │ - paymentMode: PaymentMode   │
│ - totalItemPrice: double     │ │ - success: boolean           │
│ - totalTax: double           │ │──────────────────────────────│
│ - totalFinalPrice: double    │ │ + makePayment(amount): bool  │
│──────────────────────────────│ └──────────────────────────────┘
│ + generateInvoice(items, inv)│
└──────────────────────────────┘

┌──────────────────────────────┐
│           Address            │
│──────────────────────────────│
│ - pinCode: int               │
│ - city: String               │
│ - state: String              │
└──────────────────────────────┘
```

---

## The Core Idea

A `Product` is the listing **and** its stock count in one object: id, name, price,
quantity. "5 Pepsi in Delhi" is one `Product` with `quantity = 5` inside that
warehouse's `Inventory`. The same product id can sit in several warehouses with
different counts.

Stock is checked **twice**, on purpose:

| Where                    | Question it answers              | Why it exists                            |
|--------------------------|----------------------------------|------------------------------------------|
| `addProductToCart`       | Was this available when you added it? | UX — fail fast, never build a doomed order |
| `Order.checkout`         | Is it available *right now*?      | Correctness — stock can vanish while the cart sits open |

Deleting the second one lets two overlapping customers drive the quantity negative.

---

## Flow — Start to End

### 1. Setup
`main` builds two warehouses and two users, then hands them to the facade:

```
buildDelhiWarehouse()   → Pepsi 500ml Rs 100 x5, Doove Soap Rs 50 x3
buildMumbaiWarehouse()  → Pepsi 500ml Rs 100 x8, Lays Chips Rs 20 x10
users                   → Rahul (Delhi), Priya (Mumbai)
                          new ProductDeliverySystem(users, warehouses)
```

### 2. Pick the Warehouse — `selectWarehouse(new NearestWarehouseSelectionStrategy(addr))`
Strategy matches on **city**, falling back to the first warehouse:

```java
for (Warehouse warehouse : warehouses) {
    if (warehouse.getAddress().getCity().equalsIgnoreCase(deliveryAddress.getCity())) {
        return warehouse;
    }
}
return warehouses.get(0);
```

Swap in a `CheapestShippingStrategy` or `MostStockStrategy` without touching anything else.

### 3. Browse — `findProduct(warehouse, "Pepsi 500ml")`
Facade delegates to that warehouse's `Inventory`. A product missing from this
warehouse returns `null` and the flow stops.

### 4. Add to Cart — `addProductToCart(user, product, count)`
The check is **cumulative**, not per-click, so three separate adds of 2 units cannot
sneak 6 past a stock of 3:

```java
int alreadyInCart = user.getCart().getCount(product.getProductId());
if (!product.hasStock(alreadyInCart + count)) {
    return false;                       // rejected, no Order is ever created
}
user.getCart().addItem(product.getProductId(), count);
```

A `Cart` is only a wish list of `productId -> count`. **No stock is held yet.**

### 5. Place Order — `placeOrder(user, warehouse)`
`OrderController` assigns the id and files the order under the user. The `Order`
constructor snapshots the cart and generates the bill:

```java
this.items = user.getCart().getItems();
this.orderStatus = OrderStatus.CREATED;
invoice.generateInvoice(items, warehouse.getInventory());   // 200 + 18% = 236
```

Nothing is charged and no stock has moved.

### 6. Checkout — `order.checkout(paymentMode)`
The one transaction in the system:

```
cart empty?       → CANCELLED
canFulfill(items)? → no  → CANCELLED, before any money is taken
                   → yes → removeItemsFromInventory(items)      stock blocked
                            payment.makePayment(total)
                              success → CONFIRMED, cart emptied, orderId saved
                              failure → addItemsToInventory(items)   ROLLBACK
                                        CANCELLED
```

Because quantities are plain ints, the rollback is the same map passed back the
other way — no bookkeeping of which physical units were taken.

### Output

```
FLOW A: happy path - Rahul buys 2 Pepsi with UPI
Browsing: Pepsi 500ml @ Rs 100.0 (5 in stock)
Order #1001 created, bill = Rs 236.0 (status CREATED)
  Stock blocked for order #1001
  [UPI] Collected Rs 236.0
  Payment success, order confirmed and cart emptied
Order #1001 final status: CONFIRMED

FLOW B: payment fails - Priya buys 4 Chips with Card, stock rolls back
  [Card] Bank declined Rs 94.4
  Payment failed, stock returned to inventory
Order #1002 final status: CANCELLED

FLOW C: out of stock - Rahul tries to buy 10 Pepsi
Cannot add 10 x Pepsi 500ml, only 3 in stock and cart already has 0

FINAL STATE
Warehouse 1 (Delhi)
   Pepsi 500ml    Rs 100     stock = 3        ← 5 - 2 sold
   Doove Soap     Rs 50      stock = 3
Warehouse 2 (Mumbai)
   Pepsi 500ml    Rs 100     stock = 8
   Lays Chips     Rs 20      stock = 10       ← rolled back to 10
```

---

## Design Patterns Used

| Pattern    | Where                                 | Why                                                       |
|------------|---------------------------------------|-----------------------------------------------------------|
| Facade     | `ProductDeliverySystem`               | One door for the API layer, hides the three controllers    |
| Controller | `User/Warehouse/OrderController`      | Each owns one collection — the in-memory "table"           |
| Strategy   | `WarehouseSelectionStrategy`          | Swap nearest / cheapest / most-stock routing               |
| Strategy   | `PaymentMode`                         | UPI, card, netbanking without touching `Order`             |
| Enum State | `OrderStatus`                         | Order lifecycle instead of scattered boolean flags         |

---

## Key Classes at a Glance

| Class                   | Responsibility                                               |
|-------------------------|--------------------------------------------------------------|
| `ProductDeliverySystem` | Entry point — browse, cart, order, checkout, reports          |
| `UserController`        | Holds users, find by id                                       |
| `WarehouseController`   | Holds warehouses, find by id, run the selection strategy      |
| `OrderController`       | Creates orders, keeps per-user order history                  |
| `Warehouse`             | A location plus the inventory stored there                    |
| `Inventory`             | The only place stock quantities change                        |
| `Product`               | Listing and stock count: id, name, price, quantity            |
| `Cart`                  | Wish list of productId → count, holds no stock                |
| `Order`                 | Cart snapshot, invoice, payment, status; owns the transaction |
| `Invoice`               | Items total + 18% tax                                         |
| `Payment`               | Runs the chosen `PaymentMode` and records success             |

---

## Possible Extensions

- **Observer** on `Inventory` for low-stock alerts to suppliers and dashboards
- **Replenishment strategy** (just-in-time vs bulk) to size purchase orders
- **Stock transfer** between warehouses
- **Audit trail** of every quantity change, since all of them funnel through `Inventory`
- **Reservation with TTL** instead of deduct-then-rollback, for long checkouts

---

## Run

```bash
java src/Main.java
```
