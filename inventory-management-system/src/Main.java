import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {

  public static void main(String[] args) {
    Main app = new Main();

    List<Warehouse> warehouses = new ArrayList<>();
    warehouses.add(app.buildDelhiWarehouse());
    warehouses.add(app.buildMumbaiWarehouse());

    List<User> users = new ArrayList<>();
    users.add(new User(1, "Rahul", new Address(110001, "Delhi", "Delhi")));
    users.add(new User(2, "Priya", new Address(400001, "Mumbai", "Maharashtra")));

    ProductDeliverySystem system = new ProductDeliverySystem(users, warehouses);

    System.out.println("\nFLOW A: happy path - Rahul buys 2 Pepsi with UPI");
    app.runOrderFlow(system, 1, "Pepsi 500ml", 2, new UPIPaymentMode());

    System.out.println("\nFLOW B: payment fails - Priya buys 4 Chips with Card, stock rolls back");
    app.runOrderFlow(system, 2, "Lays Chips", 4, new CardPaymentMode());

    System.out.println("\nFLOW C: out of stock - Rahul tries to buy 10 Pepsi");
    app.runOrderFlow(system, 1, "Pepsi 500ml", 10, new UPIPaymentMode());

    System.out.println("\nFINAL STATE");
    system.printAllInventory();
    system.printOrderHistory(1);
    system.printOrderHistory(2);
  }

  private void runOrderFlow(ProductDeliverySystem system, int userId,
      String productName, int count, PaymentMode paymentMode) {

    User user = system.getUser(userId);
    System.out.println("User: " + user.getUserName() + " from " + user.getAddress().getCity());

    Warehouse warehouse = system.selectWarehouse(new NearestWarehouseSelectionStrategy(user.getAddress()));
    System.out.println("Serving warehouse: " + warehouse.getAddress().getCity());

    Product product = system.findProduct(warehouse, productName);
    if (product == null) {
      System.out.println(productName + " is not sold here");
      return;
    }
    System.out.println("Browsing: " + product.getName() + " @ Rs " + product.getPrice()
        + " (" + product.getQuantity() + " in stock)");

    if (!system.addProductToCart(user, product, count)) {
      return;
    }
    System.out.println("Cart: " + count + " x " + product.getName());

    Order order = system.placeOrder(user, warehouse);
    System.out.println("Order #" + order.getOrderId() + " created, bill = Rs "
        + order.getInvoice().getTotalFinalPrice() + " (status " + order.getOrderStatus() + ")");

    system.checkout(order, paymentMode);
    System.out.println("Order #" + order.getOrderId() + " final status: " + order.getOrderStatus());

    if (order.getOrderStatus() != OrderStatus.CONFIRMED) {
      user.getCart().emptyCart();
    }
  }

  private Warehouse buildDelhiWarehouse() {
    Inventory inventory = new Inventory();
    inventory.addProduct(new Product(101, "Pepsi 500ml", 100, 5));
    inventory.addProduct(new Product(102, "Doove Soap", 50, 3));
    return new Warehouse(1, new Address(110001, "Delhi", "Delhi"), inventory);
  }

  private Warehouse buildMumbaiWarehouse() {
    Inventory inventory = new Inventory();
    inventory.addProduct(new Product(101, "Pepsi 500ml", 100, 8));
    inventory.addProduct(new Product(103, "Lays Chips", 20, 10));
    return new Warehouse(2, new Address(400001, "Mumbai", "Maharashtra"), inventory);
  }
}

class Address {
  private final int pinCode;
  private final String city;
  private final String state;

  public Address(int pinCode, String city, String state) {
    this.pinCode = pinCode;
    this.city = city;
    this.state = state;
  }

  public int getPinCode() {
    return pinCode;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }
}

class User {
  private final int userId;
  private final String userName;
  private final Address address;
  private final Cart cart = new Cart();
  private final List<Integer> orderIds = new ArrayList<>();

  public User(int userId, String userName, Address address) {
    this.userId = userId;
    this.userName = userName;
    this.address = address;
  }

  public int getUserId() {
    return userId;
  }

  public String getUserName() {
    return userName;
  }

  public Address getAddress() {
    return address;
  }

  public Cart getCart() {
    return cart;
  }

  public List<Integer> getOrderIds() {
    return orderIds;
  }

  public void addOrderId(int orderId) {
    orderIds.add(orderId);
  }
}

class Cart {
  private Map<Integer, Integer> productIdVsCount = new LinkedHashMap<>();

  public void addItem(int productId, int count) {
    if (productIdVsCount.containsKey(productId)) {
      productIdVsCount.put(productId, productIdVsCount.get(productId) + count);
    } else {
      productIdVsCount.put(productId, count);
    }
  }

  public void removeItem(int productId, int count) {
    Integer inCart = productIdVsCount.get(productId);
    if (inCart == null) {
      return;
    }
    if (inCart <= count) {
      productIdVsCount.remove(productId);
    } else {
      productIdVsCount.put(productId, inCart - count);
    }
  }

  public void emptyCart() {
    productIdVsCount = new LinkedHashMap<>();
  }

  public int getCount(int productId) {
    if (!productIdVsCount.containsKey(productId)) {
      return 0;
    }
    return productIdVsCount.get(productId);
  }

  public boolean isEmpty() {
    return productIdVsCount.isEmpty();
  }

  public Map<Integer, Integer> getItems() {
    return productIdVsCount;
  }
}

class Product {
  private final int productId;
  private final String name;
  private final double price;
  private int quantity;

  public Product(int productId, String name, double price, int quantity) {
    this.productId = productId;
    this.name = name;
    this.price = price;
    this.quantity = quantity;
  }

  public int getProductId() {
    return productId;
  }

  public String getName() {
    return name;
  }

  public double getPrice() {
    return price;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public boolean hasStock(int count) {
    return quantity >= count;
  }
}

class Inventory {
  private final Map<Integer, Product> products = new LinkedHashMap<>();

  public void addProduct(Product product) {
    products.put(product.getProductId(), product);
  }

  public void addStock(int productId, int count) {
    Product product = products.get(productId);
    if (product != null) {
      product.setQuantity(product.getQuantity() + count);
    }
  }

  public Product getProductById(int productId) {
    return products.get(productId);
  }

  public Product getProductByName(String name) {
    for (Product product : products.values()) {
      if (product.getName().equalsIgnoreCase(name)) {
        return product;
      }
    }
    return null;
  }

  public boolean hasStockFor(Map<Integer, Integer> productIdVsCount) {
    for (int productId : productIdVsCount.keySet()) {
      Product product = products.get(productId);
      if (!product.hasStock(productIdVsCount.get(productId))) {
        return false;
      }
    }
    return true;
  }

  public void removeItems(Map<Integer, Integer> productIdVsCount) {
    for (int productId : productIdVsCount.keySet()) {
      Product product = products.get(productId);
      product.setQuantity(product.getQuantity() - productIdVsCount.get(productId));
    }
  }

  public void addItems(Map<Integer, Integer> productIdVsCount) {
    for (int productId : productIdVsCount.keySet()) {
      Product product = products.get(productId);
      product.setQuantity(product.getQuantity() + productIdVsCount.get(productId));
    }
  }

  public Map<Integer, Product> getProducts() {
    return products;
  }
}

class Warehouse {
  private final int warehouseId;
  private final Address address;
  private final Inventory inventory;

  public Warehouse(int warehouseId, Address address, Inventory inventory) {
    this.warehouseId = warehouseId;
    this.address = address;
    this.inventory = inventory;
  }

  public int getWarehouseId() {
    return warehouseId;
  }

  public Address getAddress() {
    return address;
  }

  public Inventory getInventory() {
    return inventory;
  }

  public boolean canFulfill(Map<Integer, Integer> productIdVsCount) {
    return inventory.hasStockFor(productIdVsCount);
  }

  public void removeItemsFromInventory(Map<Integer, Integer> productIdVsCount) {
    inventory.removeItems(productIdVsCount);
  }

  public void addItemsToInventory(Map<Integer, Integer> productIdVsCount) {
    inventory.addItems(productIdVsCount);
  }
}

enum OrderStatus {
  CREATED, CONFIRMED, CANCELLED, DELIVERED, RETURNED
}

class Invoice {
  private static final double TAX_RATE = 0.18;

  private double totalItemPrice;
  private double totalTax;
  private double totalFinalPrice;

  public void generateInvoice(Map<Integer, Integer> items, Inventory inventory) {
    totalItemPrice = 0;
    for (int productId : items.keySet()) {
      Product product = inventory.getProductById(productId);
      totalItemPrice += product.getPrice() * items.get(productId);
    }
    totalTax = totalItemPrice * TAX_RATE;
    totalFinalPrice = totalItemPrice + totalTax;
  }

  public double getTotalItemPrice() {
    return totalItemPrice;
  }

  public double getTotalTax() {
    return totalTax;
  }

  public double getTotalFinalPrice() {
    return totalFinalPrice;
  }
}

interface PaymentMode {
  boolean makePayment(double amount);
}

class UPIPaymentMode implements PaymentMode {
  @Override
  public boolean makePayment(double amount) {
    System.out.println("  [UPI] Collected Rs " + amount);
    return true;
  }
}

class CardPaymentMode implements PaymentMode {
  @Override
  public boolean makePayment(double amount) {
    System.out.println("  [Card] Bank declined Rs " + amount);
    return false;
  }
}

class Payment {
  private final PaymentMode paymentMode;
  private boolean success;

  public Payment(PaymentMode paymentMode) {
    this.paymentMode = paymentMode;
  }

  public boolean makePayment(double amount) {
    success = paymentMode.makePayment(amount);
    return success;
  }

  public boolean isSuccess() {
    return success;
  }
}

interface WarehouseSelectionStrategy {
  Warehouse selectWarehouse(List<Warehouse> warehouses);
}

class NearestWarehouseSelectionStrategy implements WarehouseSelectionStrategy {
  private final Address deliveryAddress;

  public NearestWarehouseSelectionStrategy(Address deliveryAddress) {
    this.deliveryAddress = deliveryAddress;
  }

  @Override
  public Warehouse selectWarehouse(List<Warehouse> warehouses) {
    for (Warehouse warehouse : warehouses) {
      if (warehouse.getAddress().getCity().equalsIgnoreCase(deliveryAddress.getCity())) {
        return warehouse;
      }
    }
    return warehouses.get(0);
  }
}

class Order {
  private final int orderId;
  private final User user;
  private final Warehouse warehouse;
  private final Address deliveryAddress;
  private final Map<Integer, Integer> items;
  private final Invoice invoice = new Invoice();
  private Payment payment;
  private OrderStatus orderStatus;

  public Order(int orderId, User user, Warehouse warehouse) {
    this.orderId = orderId;
    this.user = user;
    this.warehouse = warehouse;
    this.deliveryAddress = user.getAddress();
    this.items = user.getCart().getItems();
    this.orderStatus = OrderStatus.CREATED;
    invoice.generateInvoice(items, warehouse.getInventory());
  }

  public void checkout(PaymentMode paymentMode) {
    if (items.isEmpty()) {
      System.out.println("  Cart is empty, nothing to checkout");
      orderStatus = OrderStatus.CANCELLED;
      return;
    }
    if (!warehouse.canFulfill(items)) {
      System.out.println("  Out of stock, order rejected before payment");
      orderStatus = OrderStatus.CANCELLED;
      return;
    }
    // line no 441-445 is added again additionally to 54  because see below example
    //Here's the scenario that breaks if you delete it. Delhi has 5 Pepsi. Rahul adds 2 to his cart — the cart check passes, 2 ≤ 5. He goes off to get his card. Meanwhile Priya buys 4 Pepsi and her payment succeeds, so stock is now 1. Rahul comes back and clicks pay. Without the canFulfill check, removeItemsFromInventory happily runs setQuantity(1 - 2) and your warehouse now reports minus one Pepsi, with money collected for stock that doesn't exist.
    // The cart check answers "was this available when you added it?" The checkout check answers "is it available right now?" Only the second one is binding, because the gap between them can be minutes.
    warehouse.removeItemsFromInventory(items);
    System.out.println("  Stock blocked for order #" + orderId);

    payment = new Payment(paymentMode);
    if (payment.makePayment(invoice.getTotalFinalPrice())) {
      orderStatus = OrderStatus.CONFIRMED;
      user.getCart().emptyCart();
      user.addOrderId(orderId);
      System.out.println("  Payment success, order confirmed and cart emptied");
    } else {
      warehouse.addItemsToInventory(items);
      orderStatus = OrderStatus.CANCELLED;
      System.out.println("  Payment failed, stock returned to inventory");
    }
  }

  public int getOrderId() {
    return orderId;
  }

  public User getUser() {
    return user;
  }

  public Address getDeliveryAddress() {
    return deliveryAddress;
  }

  public Map<Integer, Integer> getItems() {
    return items;
  }

  public Invoice getInvoice() {
    return invoice;
  }

  public Payment getPayment() {
    return payment;
  }

  public OrderStatus getOrderStatus() {
    return orderStatus;
  }

  public void setOrderStatus(OrderStatus orderStatus) {
    this.orderStatus = orderStatus;
  }
}

class UserController {
  private final Map<Integer, User> users = new LinkedHashMap<>();

  public UserController(List<User> userList) {
    for (User user : userList) {
      users.put(user.getUserId(), user);
    }
  }

  public void addUser(User user) {
    users.put(user.getUserId(), user);
  }

  public void removeUser(int userId) {
    users.remove(userId);
  }

  public User getUser(int userId) {
    return users.get(userId);
  }
}

class WarehouseController {
  private final List<Warehouse> warehouses;

  public WarehouseController(List<Warehouse> warehouses) {
    this.warehouses = warehouses;
  }

  public void addWarehouse(Warehouse warehouse) {
    warehouses.add(warehouse);
  }

  public void removeWarehouse(Warehouse warehouse) {
    warehouses.remove(warehouse);
  }

  public Warehouse getWarehouse(int warehouseId) {
    for (Warehouse warehouse : warehouses) {
      if (warehouse.getWarehouseId() == warehouseId) {
        return warehouse;
      }
    }
    return null;
  }

  public Warehouse selectWarehouse(WarehouseSelectionStrategy strategy) {
    return strategy.selectWarehouse(warehouses);
  }

  public List<Warehouse> getWarehouses() {
    return warehouses;
  }
}

class OrderController {
  private final List<Order> orders = new ArrayList<>();
  private final Map<Integer, List<Order>> userIdVsOrders = new HashMap<>();
  private int orderIdCounter = 1000;

  public Order createNewOrder(User user, Warehouse warehouse) {
    Order order = new Order(++orderIdCounter, user, warehouse);
    orders.add(order);

    if (!userIdVsOrders.containsKey(user.getUserId())) {
      userIdVsOrders.put(user.getUserId(), new ArrayList<>());
    }
    userIdVsOrders.get(user.getUserId()).add(order);
    return order;
  }

  public List<Order> getOrdersByUserId(int userId) {
    if (!userIdVsOrders.containsKey(userId)) {
      return new ArrayList<>();
    }
    return userIdVsOrders.get(userId);
  }

  public Order getOrderById(int orderId) {
    for (Order order : orders) {
      if (order.getOrderId() == orderId) {
        return order;
      }
    }
    return null;
  }
}

class ProductDeliverySystem {
  private final UserController userController;
  private final WarehouseController warehouseController;
  private final OrderController orderController = new OrderController();

  public ProductDeliverySystem(List<User> users, List<Warehouse> warehouses) {
    this.userController = new UserController(users);
    this.warehouseController = new WarehouseController(warehouses);
  }

  public User getUser(int userId) {
    return userController.getUser(userId);
  }

  public Warehouse selectWarehouse(WarehouseSelectionStrategy strategy) {
    return warehouseController.selectWarehouse(strategy);
  }

  public Product findProduct(Warehouse warehouse, String productName) {
    return warehouse.getInventory().getProductByName(productName);
  }

  public boolean addProductToCart(User user, Product product, int count) {
    int alreadyInCart = user.getCart().getCount(product.getProductId());
    if (!product.hasStock(alreadyInCart + count)) {
      System.out.println("Cannot add " + count + " x " + product.getName()
          + ", only " + product.getQuantity() + " in stock and cart already has " + alreadyInCart);
      return false;
    }
    user.getCart().addItem(product.getProductId(), count);
    return true;
  }

  public Order placeOrder(User user, Warehouse warehouse) {
    return orderController.createNewOrder(user, warehouse);
  }

  public void checkout(Order order, PaymentMode paymentMode) {
    order.checkout(paymentMode);
  }

  public void receiveStock(int warehouseId, int productId, int count) {
    Warehouse warehouse = warehouseController.getWarehouse(warehouseId);
    if (warehouse == null) {
      System.out.println("Unknown warehouse " + warehouseId);
      return;
    }
    warehouse.getInventory().addStock(productId, count);
    Product product = warehouse.getInventory().getProductById(productId);
    System.out.println("Received " + count + " x " + product.getName()
        + " at WH-" + warehouseId + ", stock now " + product.getQuantity());
  }

  public void printAllInventory() {
    for (Warehouse warehouse : warehouseController.getWarehouses()) {
      System.out.println("Warehouse " + warehouse.getWarehouseId()
          + " (" + warehouse.getAddress().getCity() + ")");
      for (Product product : warehouse.getInventory().getProducts().values()) {
        System.out.printf("   %-14s Rs %-7.0f stock = %d%n",
            product.getName(), product.getPrice(), product.getQuantity());
      }
    }
  }

  public void printOrderHistory(int userId) {
    User user = userController.getUser(userId);
    System.out.println("Orders of " + user.getUserName() + ":");
    for (Order order : orderController.getOrdersByUserId(userId)) {
      System.out.printf("   #%d  %-10s Rs %.0f%n",
          order.getOrderId(), order.getOrderStatus(), order.getInvoice().getTotalFinalPrice());
    }
  }
}
