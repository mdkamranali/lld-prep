import java.util.*;

// ========================== ENUMS ==========================

enum ItemType { COKE, PEPSI, JUICE, SODA }

enum Coin {
    PENNY(1), NICKEL(5), DIME(10), QUARTER(25);

    public int value;
    Coin(int value) { this.value = value; }
}

// ========================== ITEM ==========================

class Item {
    private int      code;
    private ItemType type;
    private int      price;
    private boolean  soldOut;

    public Item(int code, ItemType type, int price) {
        this.code    = code;
        this.type    = type;
        this.price   = price;
        this.soldOut = false;
    }

    public int      getCode()    { return code;    }
    public ItemType getType()    { return type;    }
    public int      getPrice()   { return price;   }
    public boolean  isSoldOut()  { return soldOut; }

    public void setSoldOut(boolean soldOut) { this.soldOut = soldOut; }
}

// ========================== INVENTORY ==========================

class Inventory {
    private Item[] items;

    public Inventory(int capacity) {
        items = new Item[capacity];
    }

    public Item[] getItems() { return items; }

    public void addItem(Item item, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= items.length) {
            System.out.println("[Error] Invalid slot index: " + slotIndex);
            return;
        }
        if (items[slotIndex] != null) {
            System.out.println("[Error] Slot " + slotIndex + " already has an item.");
            return;
        }
        items[slotIndex] = item;
    }

    public Item getItem(int codeNumber) {
        for (Item item : items) {
            if (item != null && item.getCode() == codeNumber) {
                if (item.isSoldOut()) {
                    System.out.println("[Error] Item at " + codeNumber + " is sold out.");
                    return null;
                }
                return item;
            }
        }
        System.out.println("[Error] Invalid code: " + codeNumber);
        return null;
    }

    public void markSoldOut(int codeNumber) {
        for (Item item : items) {
            if (item != null && item.getCode() == codeNumber) {
                item.setSoldOut(true);
                return;
            }
        }
    }
}

// ========================== STATE (Abstract) ==========================
// Abstract class: cannot be instantiated directly (new State() is not allowed).
// It serves as a base for all concrete states, providing default "not allowed"
// behavior. Each subclass overrides only the actions valid in that state.

abstract class State {
    public void clickOnInsertCoinButton(VendingMachine machine) {
        System.out.println("[Warning] Action not allowed in current state.");
    }

    public void clickOnStartProductSelectionButton(VendingMachine machine) {
        System.out.println("[Warning] Action not allowed in current state.");
    }

    public void insertCoin(VendingMachine machine, Coin coin) {
        System.out.println("[Warning] Action not allowed in current state.");
    }

    public void chooseProduct(VendingMachine machine, int codeNumber) {
        System.out.println("[Warning] Action not allowed in current state.");
    }

    public int getChange(int returnChangeMoney) {
        System.out.println("[Warning] Action not allowed in current state.");
        return 0;
    }

    public Item dispenseProduct(VendingMachine machine, int codeNumber) {
        System.out.println("[Warning] Action not allowed in current state.");
        return null;
    }

    public List<Coin> refundFullMoney(VendingMachine machine) {
        System.out.println("[Warning] Action not allowed in current state.");
        return Collections.emptyList();
    }

    public void updateInventory(VendingMachine machine, Item item, int slotIndex) {
        System.out.println("[Warning] Action not allowed in current state.");
    }
}

// ========================== IDLE STATE ==========================

class IdleState extends State {

    public IdleState() {
        System.out.println("Vending Machine → IdleState");
    }

    public IdleState(VendingMachine machine) {
        System.out.println("Vending Machine → IdleState");
        machine.getCoinList().clear();
    }

    @Override
    public void clickOnInsertCoinButton(VendingMachine machine) {
        machine.setVendingMachineState(new HasMoneyState());
    }

    @Override
    public void updateInventory(VendingMachine machine, Item item, int slotIndex) {
        machine.getInventory().addItem(item, slotIndex);
    }
}

// ========================== HAS MONEY STATE ==========================

class HasMoneyState extends State {

    public HasMoneyState() {
        System.out.println("Vending Machine → HasMoneyState");
    }

    @Override
    public void insertCoin(VendingMachine machine, Coin coin) {
        System.out.println("Accepted coin: " + coin.name() + " (" + coin.value + "¢)");
        machine.getCoinList().add(coin);
    }

    @Override
    public void clickOnStartProductSelectionButton(VendingMachine machine) {
        machine.setVendingMachineState(new SelectionState());
    }

    @Override
    public List<Coin> refundFullMoney(VendingMachine machine) {
        System.out.println("Refunding all coins back.");
        List<Coin> refund = new ArrayList<>(machine.getCoinList());
        machine.setVendingMachineState(new IdleState(machine));
        return refund;
    }
}

// ========================== SELECTION STATE ==========================

class SelectionState extends State {

    public SelectionState() {
        System.out.println("Vending Machine → SelectionState");
    }

    @Override
    public void chooseProduct(VendingMachine machine, int codeNumber) {
        Item item = machine.getInventory().getItem(codeNumber);
        if (item == null) {
            refundFullMoney(machine);
            return;
        }

        int paidByUser = 0;
        for (Coin coin : machine.getCoinList()) {
            paidByUser += coin.value;
        }

        if (paidByUser < item.getPrice()) {
            System.out.println("Insufficient amount! Item price: " + item.getPrice()
                    + "¢, Paid: " + paidByUser + "¢");
            refundFullMoney(machine);
            return;
        }

        if (paidByUser > item.getPrice()) {
            getChange(paidByUser - item.getPrice());
        }

        new DispenseState(machine, codeNumber);
    }

    @Override
    public int getChange(int returnExtraMoney) {
        System.out.println("Change returned: " + returnExtraMoney + "¢");
        return returnExtraMoney;
    }

    @Override
    public List<Coin> refundFullMoney(VendingMachine machine) {
        System.out.println("Refunding all coins back.");
        List<Coin> refund = new ArrayList<>(machine.getCoinList());
        machine.setVendingMachineState(new IdleState(machine));
        return refund;
    }
}

// ========================== DISPENSE STATE ==========================

class DispenseState extends State {

    public DispenseState(VendingMachine machine, int codeNumber) {
        System.out.println("Vending Machine → DispenseState");
        dispenseProduct(machine, codeNumber);
    }

    @Override
    public Item dispenseProduct(VendingMachine machine, int codeNumber) {
        Item item = machine.getInventory().getItem(codeNumber);
        machine.getInventory().markSoldOut(codeNumber);
        machine.setVendingMachineState(new IdleState(machine));
        System.out.println("Product dispensed: " + item.getType().name());
        return item;
    }
}

// ========================== VENDING MACHINE ==========================

class VendingMachine {
    private State      vendingMachineState;
    private Inventory  inventory;
    private List<Coin> coinList;

    public VendingMachine(int inventorySize) {
        vendingMachineState = new IdleState();
        inventory           = new Inventory(inventorySize);
        coinList            = new ArrayList<>();
    }

    public State      getVendingMachineState()                  { return vendingMachineState;       }
    public void       setVendingMachineState(State state)       { this.vendingMachineState = state; }
    public Inventory  getInventory()                            { return inventory;                 }
    public List<Coin> getCoinList()                             { return coinList;                  }
}

// ========================== DRIVER ==========================

public class Main {
    public static void main(String[] args) {
        System.out.println("=== VENDING MACHINE - STATE DESIGN PATTERN ===\n");

        VendingMachine machine = new VendingMachine(10);

        // --- Fill Inventory ---
        System.out.println("--- Filling Inventory ---");
        State state = machine.getVendingMachineState();
        state.updateInventory(machine, new Item(101, ItemType.COKE,  12), 0);
        state.updateInventory(machine, new Item(102, ItemType.COKE,  12), 1);
        state.updateInventory(machine, new Item(103, ItemType.COKE,  12), 2);
        state.updateInventory(machine, new Item(104, ItemType.PEPSI,  9), 3);
        state.updateInventory(machine, new Item(105, ItemType.PEPSI,  9), 4);
        state.updateInventory(machine, new Item(106, ItemType.JUICE, 13), 5);
        state.updateInventory(machine, new Item(107, ItemType.JUICE, 13), 6);
        state.updateInventory(machine, new Item(108, ItemType.SODA,   7), 7);
        state.updateInventory(machine, new Item(109, ItemType.SODA,   7), 8);
        state.updateInventory(machine, new Item(110, ItemType.SODA,   7), 9);

        displayInventory(machine);

        // --- Scenario 1: Successful Purchase ---
        System.out.println("\n--- Scenario 1: Buy PEPSI (code 104, price 9¢) ---");
        state = machine.getVendingMachineState();
        state.clickOnInsertCoinButton(machine);

        state = machine.getVendingMachineState();
        state.insertCoin(machine, Coin.NICKEL);
        state.insertCoin(machine, Coin.NICKEL);

        state.clickOnStartProductSelectionButton(machine);

        state = machine.getVendingMachineState();
        state.chooseProduct(machine, 104);

        // --- Scenario 2: Purchase with Change ---
        System.out.println("\n--- Scenario 2: Buy SODA (code 108, price 7¢) with extra coins ---");
        state = machine.getVendingMachineState();
        state.clickOnInsertCoinButton(machine);

        state = machine.getVendingMachineState();
        state.insertCoin(machine, Coin.DIME);

        state.clickOnStartProductSelectionButton(machine);

        state = machine.getVendingMachineState();
        state.chooseProduct(machine, 108);

        // --- Scenario 3: Insufficient Funds ---
        System.out.println("\n--- Scenario 3: Try buying JUICE (code 106, price 13¢) with only 5¢ ---");
        state = machine.getVendingMachineState();
        state.clickOnInsertCoinButton(machine);

        state = machine.getVendingMachineState();
        state.insertCoin(machine, Coin.NICKEL);

        state.clickOnStartProductSelectionButton(machine);

        state = machine.getVendingMachineState();
        state.chooseProduct(machine, 106);

        // --- Final Inventory ---
        System.out.println("\n--- Final Inventory ---");
        displayInventory(machine);
    }

    private static void displayInventory(VendingMachine machine) {
        System.out.println();
        System.out.printf("%-6s %-8s %-6s %-10s%n", "Code", "Item", "Price", "Available");
        System.out.println("─".repeat(34));
        for (Item item : machine.getInventory().getItems()) {
            if (item != null) {
                System.out.printf("%-6d %-8s %-6d %-10s%n",
                        item.getCode(),
                        item.getType().name(),
                        item.getPrice(),
                        !item.isSoldOut() ? "Yes" : "No");
            }
        }
    }
}
