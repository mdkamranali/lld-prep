# Coffee Machine - Low Level Design

## Problem
Design a coffee machine that lets customers select a drink, add extras (milk, sugar, syrup), pay for it, and get their coffee.

## Design Patterns Used

### 1. Decorator Pattern (Add-Ons)
Used to add extras on top of a base drink without modifying the base drink class.

```
Drink (interface)
├── Espresso          (base drink - $2.00)
├── Americano         (base drink - $2.50)
│
DrinkAddOn (abstract, wraps a Drink)
├── MilkAddOn         (+$0.50)
├── SugarAddOn        (+$0.20)
├── SyrupAddOn        (+$0.60)
```

**Example:** Espresso + Milk + Sugar = `new SugarAddOn(new MilkAddOn(new Espresso()))`
- Price = 2.00 + 0.50 + 0.20 = $2.70

### 2. State Pattern (Machine States)
The machine behaves differently based on its current state.

```
MachineState (interface)
├── ReadyState              → waiting for customer to pick a drink
├── WaitingForMoneyState    → drink selected, waiting for payment
├── MakingDrinkState        → payment done, preparing the coffee
```

## Flow

```
[Ready] --selectDrink()--> [WaitingForMoney] --insertMoney(enough)--> [MakingDrink] --makeDrink()--> [Ready]
```

1. Customer selects a drink (with optional add-ons)
2. Machine shows price and asks for money
3. Customer inserts money (can insert multiple times)
4. Once enough money is inserted, machine moves to making state
5. Machine makes the drink, returns change if any
6. Machine goes back to Ready state

## Class Diagram

```
┌─────────────────┐         ┌──────────────┐
│  CoffeeMachine  │────────►│ MachineState │ (interface)
├─────────────────┤         └──────────────┘
│ - currentState  │              ▲
│ - selectedDrink │              ├── ReadyState
│ - moneyInserted │              ├── WaitingForMoneyState
│ - coffeeBeansLeft│             └── MakingDrinkState
├─────────────────┤
│ + selectDrink() │
│ + insertMoney() │         ┌──────────────┐
│ + makeDrink()   │────────►│    Drink     │ (interface)
│ + refill()      │         └──────────────┘
└─────────────────┘              ▲
                                 ├── Espresso
                                 ├── Americano
                                 └── DrinkAddOn (abstract)
                                      ├── MilkAddOn
                                      ├── SugarAddOn
                                      └── SyrupAddOn
```

## How to Run

```bash
cd src
javac Main.java
java Main
```

## Sample Output

```
========== COFFEE MACHINE ==========

--- Order 1: Espresso + Milk + Sugar ---
You selected: Espresso + Milk + Sugar | Price: $2.7
Please insert money.
Inserted: $1.00 | Total paid: $1.00 | Price: $2.70
Inserted: $2.00 | Total paid: $3.00 | Price: $2.70
Payment complete! Making your drink...
Making your Espresso + Milk + Sugar...
Returning change: $0.30
Done! Enjoy your coffee!

--- Order 2: Americano + Caramel Syrup ---
You selected: Americano + Caramel Syrup | Price: $3.1
Please insert money.
Inserted: $3.10 | Total paid: $3.10 | Price: $3.10
Payment complete! Making your drink...
Making your Americano + Caramel Syrup...
Done! Enjoy your coffee!

--- Edge Cases ---
Please select a drink first!
Please select a drink and pay first!
```
