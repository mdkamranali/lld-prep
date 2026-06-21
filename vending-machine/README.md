# Vending Machine - Low Level Design

## Design Pattern
**State Pattern** — The vending machine transitions between discrete states (Idle, HasMoney, Selection, Dispense), and each state defines which operations are valid.

## Classes
- `Item` / `ItemShelf` / `Inventory` — product catalog and slot management
- `State` (abstract) — base class with default no-op for every action
- `IdleState` — accepts coin-insert button press and inventory updates
- `HasMoneyState` — accepts coins and transitions to selection
- `SelectionState` — validates payment against item price, handles change/refund
- `DispenseState` — dispenses product and returns to idle
- `VendingMachine` — context that holds current state, inventory, and inserted coins

## How to Run
```bash
cd src
javac Main.java
java Main
```
