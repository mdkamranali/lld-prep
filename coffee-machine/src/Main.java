import java.util.*;

// ─── DRINK INTERFACE (Base for all drinks) ───────────────────────────────────
// Any drink in our coffee machine must have a price and a name.

interface Drink {
    double getPrice();
    String getName();
}

// ─── BASE DRINKS ─────────────────────────────────────────────────────────────
// These are the core drinks our machine can make.
// Espresso = a small, strong black coffee shot
// Americano = espresso + hot water (lighter, larger coffee)

class Espresso implements Drink {
    @Override
    public double getPrice() { return 2.00; }

    @Override
    public String getName() { return "Espresso"; }
}

class Americano implements Drink {
    @Override
    public double getPrice() { return 2.50; }

    @Override
    public String getName() { return "Americano"; }
}

// ─── ADD-ONS (Decorator Pattern) ─────────────────────────────────────────────
// Add-ons wrap around a base drink to add extra stuff (milk, sugar, syrup).
// Each add-on increases the price and updates the name.

abstract class DrinkAddOn implements Drink {
    protected Drink baseDrink;

    public DrinkAddOn(Drink baseDrink) {
        this.baseDrink = baseDrink;
    }
}

class MilkAddOn extends DrinkAddOn {
    public MilkAddOn(Drink baseDrink) { super(baseDrink); }

    @Override
    public double getPrice() { return baseDrink.getPrice() + 0.50; }

    @Override
    public String getName() { return baseDrink.getName() + " + Milk"; }
}

class SugarAddOn extends DrinkAddOn {
    public SugarAddOn(Drink baseDrink) { super(baseDrink); }

    @Override
    public double getPrice() { return baseDrink.getPrice() + 0.20; }

    @Override
    public String getName() { return baseDrink.getName() + " + Sugar"; }
}

class FlavorAddOn extends DrinkAddOn {
    public FlavorAddOn(Drink baseDrink) { super(baseDrink); }

    @Override
    public double getPrice() { return baseDrink.getPrice() + 0.60; }

    @Override
    public String getName() { return baseDrink.getName() + " + Caramel Flavor"; }
}

// ─── MACHINE STATE (State Pattern) ──────────────────────────────────────────
// The coffee machine behaves differently depending on its current state.
// States: Ready -> WaitingForMoney -> MakingDrink -> back to Ready

abstract class MachineState {
    // Default behavior: if an action is not allowed in the current state, show warning.
    // Each state only overrides the methods that ARE allowed in that state.

    void selectDrink(CoffeeMachine machine, Drink drink) {
        System.out.println("[Warning] Action not allowed in current state.");
    }

    void insertMoney(CoffeeMachine machine, double amount) {
        System.out.println("[Warning] Action not allowed in current state.");
    }

    void makeDrink(CoffeeMachine machine) {
        System.out.println("[Warning] Action not allowed in current state.");
    }
}

// ─── STATE 1: READY (Machine is idle, waiting for customer to pick a drink) ──
// Allowed action: selectDrink()

class ReadyState extends MachineState {
    @Override
    void selectDrink(CoffeeMachine machine, Drink drink) {
        machine.setSelectedDrink(drink);
        machine.setState(new WaitingForMoneyState());
        System.out.println("You selected: " + drink.getName() + " | Price: $" + drink.getPrice());
        System.out.println("Please insert money.");
    }
}

// ─── STATE 2: WAITING FOR MONEY (Drink selected, waiting for payment) ────────
// Allowed action: insertMoney()

class WaitingForMoneyState extends MachineState {
    @Override
    void insertMoney(CoffeeMachine machine, double amount) {
        machine.addMoney(amount);
        double price = machine.getSelectedDrink().getPrice();
        double paid = machine.getMoneyInserted();

        System.out.println(String.format("Inserted: $%.2f | Total paid: $%.2f | Price: $%.2f",
                amount, paid, price));

        if (paid >= price) {
            System.out.println("Payment complete! Making your drink...");
            machine.setState(new MakingDrinkState());
        }
    }
}

// ─── STATE 3: MAKING DRINK (Payment done, now preparing the coffee) ──────────
// Allowed action: makeDrink()

class MakingDrinkState extends MachineState {
    @Override
    void makeDrink(CoffeeMachine machine) {
        Drink drink = machine.getSelectedDrink();

        if (machine.hasIngredients()) {
            System.out.println("Making your " + drink.getName() + "...");
            machine.useIngredients();

            // Return change if customer paid extra
            double change = machine.getMoneyInserted() - drink.getPrice();
            if (change > 0) {
                System.out.println(String.format("Returning change: $%.2f", change));
            }

            System.out.println("Done! Enjoy your coffee!\n");

            // Reset machine for next customer
            machine.resetMoney();
            machine.setSelectedDrink(null);
            machine.setState(new ReadyState());
        } else {
            System.out.println("Sorry! Out of ingredients. Returning your money.");
            machine.resetMoney();
            machine.setSelectedDrink(null);
            machine.setState(new ReadyState());
        }
    }
}

// ─── COFFEE MACHINE (Main class that ties everything together) ───────────────

class CoffeeMachine {
    private MachineState currentState;
    private Drink selectedDrink;
    private double moneyInserted;
    private int coffeeBeansLeft;  // how many cups the machine can still make before needing a refill

    public CoffeeMachine() {
        this.currentState = new ReadyState();
        this.selectedDrink = null;
        this.moneyInserted = 0.0;
        this.coffeeBeansLeft = 10;  // machine starts with 10 servings
    }

    // --- State management ---
    public void setState(MachineState state) { this.currentState = state; }
    public MachineState getState() { return currentState; }

    // --- Drink selection ---
    public void setSelectedDrink(Drink drink) { this.selectedDrink = drink; }
    public Drink getSelectedDrink() { return selectedDrink; }

    // --- Money handling ---
    public void addMoney(double amount) { this.moneyInserted += amount; }
    public double getMoneyInserted() { return moneyInserted; }
    public void resetMoney() { this.moneyInserted = 0.0; }

    // --- Ingredients ---
    public boolean hasIngredients() { return coffeeBeansLeft > 0; }
    public void useIngredients() { coffeeBeansLeft--; }

    public void refill() {
        coffeeBeansLeft = 10;
        System.out.println("Machine refilled with fresh ingredients!");
    }

}

// ─── MAIN (Demo) ─────────────────────────────────────────────────────────────

public class Main {
    public static void main(String[] args) {
        System.out.println("========== COFFEE MACHINE ==========\n");

        CoffeeMachine machine = new CoffeeMachine();
        MachineState state;

        // --- Order 1: Espresso with Milk and Sugar ---
        System.out.println("--- Order 1: Espresso + Milk + Sugar ---");
        Drink myDrink = new SugarAddOn(new MilkAddOn(new Espresso()));
        // Price: 2.00 (Espresso) + 0.50 (Milk) + 0.20 (Sugar) = $2.70

        state = machine.getState();
        state.selectDrink(machine, myDrink);

        state = machine.getState();
        state.insertMoney(machine, 1.00);
        state.insertMoney(machine, 2.00);  // Total $3.00, price is $2.70 -> change $0.30

        state = machine.getState();
        state.makeDrink(machine);

        // --- Order 2: Americano with Caramel Flavor ---
        System.out.println("--- Order 2: Americano + Caramel Flavor ---");
        Drink secondDrink = new FlavorAddOn(new Americano());
        // Price: 2.50 (Americano) + 0.60 (Flavor) = $3.10

        state = machine.getState();
        state.selectDrink(machine, secondDrink);

        state = machine.getState();
        state.insertMoney(machine, 3.10);  // Exact amount

        state = machine.getState();
        state.makeDrink(machine);

        // --- Show what happens with wrong actions ---
        System.out.println("--- Edge Cases ---");
        state = machine.getState();
        state.insertMoney(machine, 1.00);  // Warning: not allowed

        state = machine.getState();
        state.makeDrink(machine);           // Warning: not allowed
    }
}
