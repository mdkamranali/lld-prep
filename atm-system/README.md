# ATM System - Low Level Design

## Problem
Design an ATM that lets a user insert their card, authenticate with a PIN, choose an operation (cash withdrawal or balance check), and — for withdrawals — dispense the exact amount using an inventory of ₹2000, ₹500, and ₹100 notes.

## Design Patterns Used

### 1. State Pattern (ATM lifecycle)
The ATM behaves differently depending on where the user is in the flow. Each state exposes only the operations that are valid in that state; every other operation on the base class prints `"OOPS!! Something went wrong"`.

```
ATMState (abstract)
├── IdleState              → waiting for a card
├── HasCardState           → card inserted, waiting for PIN
├── SelectOperationState   → PIN verified, waiting for txn choice
├── CashWithdrawalState    → withdrawal chosen, waiting for amount
└── CheckBalanceState      → balance-check chosen
```

### 2. Chain of Responsibility (Cash dispensing)
For withdrawal, the requested amount is broken down denomination-by-denomination. Each processor handles its own denomination and forwards any remainder to the next processor in the chain.

```
TwoThousandWithdrawProcessor → FiveHundredWithdrawProcessor → OneHundredWithdrawProcessor → null
```

**Example:** withdraw ₹2700 with ATM inventory `{2000: 1, 500: 2, 100: 5}`
- 2k processor: dispenses 1×2000, forwards ₹700
- 500 processor: dispenses 1×500, forwards ₹200
- 100 processor: dispenses 2×100, remainder = 0 ✓

## Flow

```
[Idle] --insertCard()--> [HasCard] --authenticatePin(correct)--> [SelectOperation]
                                        │
                                        ├── selectOperation(CASH_WITHDRAWAL) --> [CashWithdrawal] --cashWithdrawal(amount)--> [Idle]
                                        └── selectOperation(BALANCE_CHECK)   --> [CheckBalance]   --displayBalance()--------> [Idle]
```

1. User inserts card → `IdleState → HasCardState`
2. User enters PIN
   - correct → `SelectOperationState`
   - wrong → card returned, back to `IdleState`
3. User picks operation
   - `CASH_WITHDRAWAL` → `CashWithdrawalState`
   - `BALANCE_CHECK` → `CheckBalanceState`
4. Withdrawal path:
   - Validate ATM has enough total balance
   - Validate user's bank has enough balance
   - Deduct from both, then run the note-dispensing chain
   - Return card, go back to `IdleState`

> Assumption: the ATM inventory can always fulfill the requested amount with the available `{2000, 500, 100}` note counts.

## Class Diagram

```
┌────────────────────┐         ┌───────────────┐
│        ATM         │────────►│   ATMState    │  (abstract)
├────────────────────┤         └───────────────┘
│ - currentATMState  │                ▲
│ - atmBalance       │                ├── IdleState
│ - noOfTwoThousand… │                ├── HasCardState
│ - noOfFiveHundred… │                ├── SelectOperationState
│ - noOfOneHundred…  │                ├── CashWithdrawalState
├────────────────────┤                └── CheckBalanceState
│ + setAtmBalance()  │
│ + deduct*Notes()   │
│ + printStatus()    │
└────────────────────┘

┌──────────────┐   ┌──────────────────┐   ┌─────────────────────────┐
│    User      │──►│      Card        │──►│    UserBankAccount      │
├──────────────┤   ├──────────────────┤   ├─────────────────────────┤
│ - card       │   │ - PIN_NUMBER     │   │ - balance               │
└──────────────┘   │ - bankAccount    │   │ + withdrawalBalance()   │
                   │ + isCorrectPIN() │   │ + getBalance()          │
                   │ + getBankBalance │   └─────────────────────────┘
                   │ + deductBankBal. │
                   └──────────────────┘

┌─────────────────────────┐
│  CashWithdrawProcessor  │  (abstract, Chain of Responsibility)
├─────────────────────────┤
│ - next                  │
│ + withdraw(atm, amount) │
└─────────────────────────┘
           ▲
           ├── TwoThousandWithdrawProcessor
           ├── FiveHundredWithdrawProcessor
           └── OneHundredWithdrawProcessor
```

## How to Run

```bash
cd src
javac Main.java
java Main
```

## Sample Output

For the driver in `Main.java` — ATM starts with `{Balance: 3500, 2k: 1, 500: 2, 100: 5}`, user requests ₹2700:

```
Balance: 3500
2kNotes: 1
500Notes: 2
100Notes: 5
Card is inserted
enter your card pin number
Please select the Operation
CASH_WITHDRAWAL
BALANCE_CHECK
Please enter the Withdrawal Amount
Please collect your card
Exit happens
Balance: 800
2kNotes: 0
500Notes: 1
100Notes: 2
```

After withdrawal, ATM dispensed `1×2000 + 1×500 + 2×100 = 2700`, leaving `{Balance: 800, 2k: 0, 500: 1, 100: 2}`.
