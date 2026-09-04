# Splitwise — Low Level Design

Design an expense-sharing app like **Splitwise**: users can form groups,
record who paid for what, split the expense **equally / unequally / by
percentage**, and see a clear per-user balance sheet of "who owes whom" and
"who gets money back from whom".

## UML Class Diagram

```
┌──────────────────────────────┐        ┌──────────────────────────────────┐
│            User              │        │      UserExpenseBalanceSheet     │
│──────────────────────────────│  1─▶1  │──────────────────────────────────│
│ - userId: String             │───────▶│ - getBackFrom: Map<userId,Double>│
│ - userName: String           │        │ - owedTo:      Map<userId,Double>│
│ - userExpenseBalanceSheet    │        └──────────────────────────────────┘
└──────────────────────────────┘
        ▲
        │ managed by
┌──────────────────────────────┐
│        UserController        │
│──────────────────────────────│
│ - userList: List<User>       │
│──────────────────────────────│
│ + addUser(u)                 │
│ + getUser(id): User          │
│ + getAllUsers(): List<User>  │
└──────────────────────────────┘


┌──────────────────────────────┐        ┌──────────────────────────────┐
│            Split             │        │            Expense           │
│──────────────────────────────│        │──────────────────────────────│
│ - user: User                 │◀───────│ - expenseId: String          │
│ - amountOwe: double          │ has *  │ - description: String        │
└──────────────────────────────┘        │ - expenseAmount: double      │
                                        │ - paidByUser: User           │
                                        │ - splitDetails: List<Split>  │
                                        └──────────────────────────────┘
                                                     ▲
                                                     │ created by
┌────────────────────────────────────┐               │
│           <<interface>>            │       ┌──────────────────────────────┐
│           ExpenseSplit             │       │      ExpenseController       │
│────────────────────────────────────│  used │──────────────────────────────│
│ + computeSplits(total): List<Split>│──────▶│ - balanceSheetController     │
└────────────────────────────────────┘       │──────────────────────────────│
        ▲          ▲          ▲              │ + createExpense(id, desc,    │
        │          │          │              │       amount, splitStrategy, │
        │          │          │              │       paidBy): Expense       │
   ┌────┴──┐  ┌────┴──┐  ┌────┴──────┐       └──────────────────────────────┘
   │ Equal │  │Unequal│  │Percentage │
   │ Split │  │ Split │  │   Split   │
   └───────┘  └───────┘  └───────────┘
   participants userAmts   userPercents


┌──────────────────────────────┐        ┌──────────────────────────────┐
│            Group             │        │        GroupController       │
│──────────────────────────────│◀───────│──────────────────────────────│
│ - groupId: String            │  owns  │ - groupList: List<Group>     │
│ - groupName: String          │        │──────────────────────────────│
│ - groupMembers: List<User>   │        │ + createNewGroup(id, name, u)│
│ - expenseList: List<Expense> │        │ + getGroup(id): Group        │
│ - expenseController          │        └──────────────────────────────┘
│──────────────────────────────│
│ + addMember(u)               │
│ + createExpense(...)         │
└──────────────────────────────┘


┌───────────────────────────────────────────────────────────────┐
│                    BalanceSheetController                     │
│───────────────────────────────────────────────────────────────│
│ + updateUserExpenseBalanceSheet(paidBy, splits)               │
│ + showBalanceSheetOfUser(user)                                │
└───────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────┐
│                     Splitwise (Facade)                        │
│───────────────────────────────────────────────────────────────│
│ - userController: UserController                              │
│ - groupController: GroupController                            │
│ - balanceSheetController: BalanceSheetController              │
│───────────────────────────────────────────────────────────────│
│ + demo(): void                                                │
└───────────────────────────────────────────────────────────────┘
```

---

## Flow — Start to End

### 1. Setup — `Splitwise.demo()`

- **Users** `U1001, U2001, U3001` are onboarded through `UserController.addUser`.
- A **Group** `G1001 ("Outing with Friends")` is created via
  `GroupController.createNewGroup`. The creator is auto-added as the first
  member; `U2001` and `U3001` are added next.
- Every `User` is created with its own empty `UserExpenseBalanceSheet` that
  carries two maps:
  - `getBackFrom: Map<userId, Double>` — how much each other user owes me
  - `owedTo:      Map<userId, Double>` — how much I owe each other user

### 2. Create an Expense — `group.createExpense(id, desc, amount, splitStrategy, paidBy)`

There's exactly **one** entry point for all three split types. The caller
picks a **Strategy** at the call site — no enum, no factory, no `if`s:

```
group.createExpense("Exp1001", "Breakfast", 900,
    new EqualExpenseSplit(List.of(u1, u2, u3)), u1);

group.createExpense("Exp1002", "Lunch", 500,
    new UnequalExpenseSplit(Map.of(u1, 400.0, u2, 100.0)), u2);

group.createExpense("Exp1003", "Dinner", 600,
    new PercentageExpenseSplit(Map.of(u1, 50.0, u2, 30.0, u3, 20.0)), u3);
```

Inside `ExpenseController.createExpense`:

1. **Ask the strategy to produce final splits** —
   `splitStrategy.computeSplits(totalAmount)` returns a `List<Split>` with
   the actual owed amount per user. Each strategy handles its own validation
   and math:
   - `EqualExpenseSplit` → `share = total / participants.size()`
   - `UnequalExpenseSplit` → validates `Σ amounts == total`
   - `PercentageExpenseSplit` → validates `Σ percents == 100`, converts
     `percent → (percent / 100) * total`
2. **Build the `Expense`** with paidBy + the finished splits.
3. **Update balance sheets** via `BalanceSheetController.updateUserExpenseBalanceSheet`.

### 3. Balance-Sheet Update — the whole rule in 2 lines

For every split in the expense (except the payer themselves):

- On the **payer's** sheet:
  `getBackFrom[ower] += amount`  ← "they will give me back this much"
- On the **ower's** sheet:
  `owedTo[payer]     += amount`  ← "I must return this much"

That's the entire ledger — no running totals, no double accounting, no
netting. Two maps per user, updated symmetrically.

### 4. Display — `balanceSheetController.showBalanceSheetOfUser(user)`

For each user prints:
```
Balance sheet of user : U1001
  Get back from U2001 : 300.0
  Get back from U3001 : 300.0
  Owe to        U2001 : 400.0
  Owe to        U3001 : 120.0
```

Reading `U1001`'s sheet: "U2001 will give me back 300 (from breakfast),
U3001 will give me back 300 (from breakfast). I owe U2001 400 (from lunch)
and U3001 120 (from dinner)."

---

## Design Ideas Used

| Pattern / Idea                  | Where                                            | Why                                                                     |
|---------------------------------|--------------------------------------------------|-------------------------------------------------------------------------|
| Strategy                        | `ExpenseSplit` + 3 impls                         | Each split type owns its own data + math; add a new type = one class    |
| Facade                          | `Splitwise`, `Group.createExpense`               | Hides `ExpenseController` + `BalanceSheetController` behind one call    |
| Controller pattern              | `UserController`, `GroupController`, `ExpenseController`, `BalanceSheetController` | Isolates lookup / lifecycle from the domain objects |
| Data-in-strategy                | `EqualExpenseSplit(List<User>)`, `UnequalExpenseSplit(Map<User,Double>)`, `PercentageExpenseSplit(Map<User,Double>)` | Removes the need for an enum + factory; the caller instantiates the exact strategy |
| Two-map ledger                  | `UserExpenseBalanceSheet.getBackFrom` / `owedTo` | Simplest possible "who owes whom" representation                        |

---

## Key Classes at a Glance

| Class                      | Responsibility                                                                 |
|----------------------------|--------------------------------------------------------------------------------|
| `User`                     | Identity + personal balance sheet                                              |
| `UserExpenseBalanceSheet`  | Two maps: `getBackFrom` and `owedTo`                                           |
| `Split`                    | (user, amountOwe) — one row inside an expense                                  |
| `ExpenseSplit` (interface) | `computeSplits(totalAmount): List<Split>`                                      |
| `EqualExpenseSplit`        | Takes participants; each pays `total / n`                                      |
| `UnequalExpenseSplit`      | Takes `Map<User, amount>`; validates sum == total                              |
| `PercentageExpenseSplit`   | Takes `Map<User, percent>`; validates sum == 100, converts to amounts          |
| `Expense`                  | id + description + total + paidBy + `List<Split>`                              |
| `ExpenseController`        | Runs the strategy, builds the `Expense`, updates balance sheets                |
| `Group`                    | Members + expenses; exposes `createExpense(..., ExpenseSplit, paidBy)`         |
| `GroupController`          | CRUD on groups                                                                 |
| `BalanceSheetController`   | Updates and prints per-user balance sheets                                     |
| `Splitwise`                | Facade / demo orchestrator                                                     |

---

## Sample Run (from `Main.main`)

```
Breakfast (900, EQUAL, paid by U1001)  → each owes 300
Lunch     (500, UNEQUAL 400/100, paid by U2001)
Dinner    (600, PERCENTAGE 50/30/20, paid by U3001) → 300 / 180 / 120
```

The final balance sheets show, for every user, exactly which other users they
must pay and which will pay them, with no aggregated totals to distract.

---

## Why the API doesn't discriminate by split type

Earlier iterations had per-type helpers (`createEqualExpense`,
`createUnequalExpense`, …) and an `ExpenseSplitType` enum + `SplitFactory`.
Both were removed in favor of the pure Strategy form:

- **One method** on `Group`/`ExpenseController` — `createExpense(..., ExpenseSplit, paidBy)`.
- **The strategy carries its own input** — no `List<Split>` construction at
  the call site, no dummy zero amounts.
- **Adding a new split type** (e.g. share-based, exact rupees + tax split,
  round-robin) is a single new class implementing `ExpenseSplit`. No enum
  to touch, no factory to update, no `Group` method to add.
