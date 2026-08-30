// ==================== ENUM ====================
enum TransactionType {
    CASH_WITHDRAWAL,
    BALANCE_CHECK,
}

// ==================== ATM Room Components ====================
class UserBankAccount {
    int balance;

    public void withdrawalBalance(int amount) {
        balance = balance - amount;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }
}

class Card {
    static int PIN_NUMBER = 112211;
    private UserBankAccount bankAccount;

    public boolean isCorrectPINEntered(int pin) {
        return pin == PIN_NUMBER;
    }

    public int getBankBalance() {
        return bankAccount.balance;
    }

    public void deductBankBalance(int amount) {
        bankAccount.withdrawalBalance(amount);
    }

    public void setBankAccount(UserBankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }
}

class User {
    Card card;

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }
}

class ATM {

    ATMState currentATMState;
    int noOfTwoThousandNotes;
    int noOfFiveHundredNotes;
    int noOfOneHundredNotes;
    private int atmBalance;

    public ATM() {
        this.currentATMState = new IdleState();
    }

    public ATMState getCurrentATMState() {
        return currentATMState;
    }

    public void setCurrentATMState(ATMState currentATMState) {
        this.currentATMState = currentATMState;
    }

    public int getAtmBalance() {
        return atmBalance;
    }

    public void setAtmBalance(int atmBalance, int noOfTwoThousandNotes, int noOfFiveHundredNotes, int noOfOneHundredNotes) {
        this.atmBalance = atmBalance;
        this.noOfTwoThousandNotes = noOfTwoThousandNotes;
        this.noOfFiveHundredNotes = noOfFiveHundredNotes;
        this.noOfOneHundredNotes = noOfOneHundredNotes;
    }

    public int getNoOfTwoThousandNotes() {
        return noOfTwoThousandNotes;
    }

    public int getNoOfFiveHundredNotes() {
        return noOfFiveHundredNotes;
    }

    public int getNoOfOneHundredNotes() {
        return noOfOneHundredNotes;
    }

    public void deductATMBalance(int amount) {
        atmBalance = atmBalance - amount;
    }

    public void deductTwoThousandNotes(int number) {
        noOfTwoThousandNotes = noOfTwoThousandNotes - number;
    }

    public void deductFiveHundredNotes(int number) {
        noOfFiveHundredNotes = noOfFiveHundredNotes - number;
    }

    public void deductOneHundredNotes(int number) {
        noOfOneHundredNotes = noOfOneHundredNotes - number;
    }

    public void printCurrentATMStatus() {
        System.out.println("Balance: " + atmBalance);
        System.out.println("2kNotes: " + noOfTwoThousandNotes);
        System.out.println("500Notes: " + noOfFiveHundredNotes);
        System.out.println("100Notes: " + noOfOneHundredNotes);
    }
}

// ==================== ATM States (State Pattern) ====================
abstract class ATMState {
    public void insertCard(ATM atm, Card card) {
        System.out.println("OOPS!! Something went wrong");
    }

    public void authenticatePin(ATM atm, Card card, int pin) {
        System.out.println("OOPS!! Something went wrong");
    }

    public void selectOperation(ATM atm, Card card, TransactionType txnType) {
        System.out.println("OOPS!! Something went wrong");
    }

    public void cashWithdrawal(ATM atm, Card card, int withdrawAmount) {
        System.out.println("OOPS!! Something went wrong");
    }

    public void displayBalance(ATM atm, Card card) {
        System.out.println("OOPS!! Something went wrong");
    }

    public void returnCard() {
        System.out.println("OOPS!! Something went wrong");
    }

    public void exit(ATM atm) {
        System.out.println("OOPS!! Something went wrong");
    }
}

class IdleState extends ATMState {
    @Override
    public void insertCard(ATM atm, Card card) {
        System.out.println("Card is inserted");
        atm.setCurrentATMState(new HasCardState());
    }
}

class HasCardState extends ATMState {
    public HasCardState() {
        System.out.println("enter your card pin number");
    }

    @Override
    public void authenticatePin(ATM atm, Card card, int pin) {
        boolean isCorrectPinEntered = card.isCorrectPINEntered(pin);

        if (isCorrectPinEntered) {
            atm.setCurrentATMState(new SelectOperationState());
        } else {
            System.out.println("Invalid PIN Number");
            exit(atm);
        }
    }

    @Override
    public void exit(ATM atm) {
        returnCard();
        atm.setCurrentATMState(new IdleState());
        System.out.println("Exit happens");
    }

    @Override
    public void returnCard() {
        System.out.println("Please collect your card");
    }
}

class SelectOperationState extends ATMState {
    public SelectOperationState() {
        showOperations();
    }

    @Override
    public void selectOperation(ATM atmObject, Card card, TransactionType txnType) {
        switch (txnType) {
            case CASH_WITHDRAWAL:
                atmObject.setCurrentATMState(new CashWithdrawalState());
                break;
            case BALANCE_CHECK:
                atmObject.setCurrentATMState(new CheckBalanceState());
                break;
            default: {
                System.out.println("Invalid Option");
                exit(atmObject);
            }
        }
    }

    @Override
    public void exit(ATM atmObject) {
        returnCard();
        atmObject.setCurrentATMState(new IdleState());
        System.out.println("Exit happens");
    }

    @Override
    public void returnCard() {
        System.out.println("Please collect your card");
    }

    private void showOperations() {
        System.out.println("Please select the Operation");
        TransactionType.showAllTransactionTypes();
    }
}

class CashWithdrawalState extends ATMState {
    public CashWithdrawalState() {
        System.out.println("Please enter the Withdrawal Amount");
    }

    @Override
    public void cashWithdrawal(ATM atmObject, Card card, int withdrawalAmountRequest) {
        if (atmObject.getAtmBalance() < withdrawalAmountRequest) {
            System.out.println("Insufficient fund in the ATM Machine");
            exit(atmObject);
        } else if (card.getBankBalance() < withdrawalAmountRequest) {
            System.out.println("Insufficient fund in the your Bank Account");
            exit(atmObject);
        } else {
            card.deductBankBalance(withdrawalAmountRequest);
            atmObject.deductATMBalance(withdrawalAmountRequest);

            // Chain of Responsibility: decide how many 2k/500/100 notes to dispense.
            // Assumption: ATM inventory can always fulfill the requested amount.
            CashWithdrawProcessor withdrawProcessor =
                    new TwoThousandWithdrawProcessor(
                            new FiveHundredWithdrawProcessor(
                                    new OneHundredWithdrawProcessor(null)));

            withdrawProcessor.withdraw(atmObject, withdrawalAmountRequest);
            exit(atmObject);
        }
    }

    @Override
    public void exit(ATM atmObject) {
        returnCard();
        atmObject.setCurrentATMState(new IdleState());
        System.out.println("Exit happens");
    }

    @Override
    public void returnCard() {
        System.out.println("Please collect your card");
    }
}

class CheckBalanceState extends ATMState {
    @Override
    public void displayBalance(ATM atm, Card card) {
        System.out.println("Your Balance is: " + card.getBankBalance());
        exit(atm);
    }

    @Override
    public void exit(ATM atmObject) {
        returnCard();
        atmObject.setCurrentATMState(new IdleState());
        System.out.println("Exit happens");
    }

    @Override
    public void returnCard() {
        System.out.println("Please collect your card");
    }
}

// ==================== Amount Withdrawal (Chain of Responsibility) ====================
abstract class CashWithdrawProcessor {
    CashWithdrawProcessor nextCashWithdrawalProcessor;

    CashWithdrawProcessor(CashWithdrawProcessor cashWithdrawalProcessor) {
        this.nextCashWithdrawalProcessor = cashWithdrawalProcessor;
    }

    public void withdraw(ATM atm, int remainingAmount) {
        if (nextCashWithdrawalProcessor != null) {
            nextCashWithdrawalProcessor.withdraw(atm, remainingAmount);
        }
    }
}

class TwoThousandWithdrawProcessor extends CashWithdrawProcessor {
    public TwoThousandWithdrawProcessor(CashWithdrawProcessor nextCashWithdrawProcessor) {
        super(nextCashWithdrawProcessor);
    }

    @Override
    public void withdraw(ATM atm, int remainingAmount) {
        int required = remainingAmount / 2000;
        int balance = remainingAmount % 2000;

        if (required <= atm.getNoOfTwoThousandNotes()) {
            atm.deductTwoThousandNotes(required);
        } else {
            int available = atm.getNoOfTwoThousandNotes();
            atm.deductTwoThousandNotes(available);
            balance = balance + (required - available) * 2000;
        }

        if (balance != 0) {
            super.withdraw(atm, balance);
        }
    }
}

class FiveHundredWithdrawProcessor extends CashWithdrawProcessor {
    public FiveHundredWithdrawProcessor(CashWithdrawProcessor nextCashWithdrawProcessor) {
        super(nextCashWithdrawProcessor);
    }

    @Override
    public void withdraw(ATM atm, int remainingAmount) {
        int required = remainingAmount / 500;
        int balance = remainingAmount % 500;

        if (required <= atm.getNoOfFiveHundredNotes()) {
            atm.deductFiveHundredNotes(required);
        } else {
            int available = atm.getNoOfFiveHundredNotes();
            atm.deductFiveHundredNotes(available);
            balance = balance + (required - available) * 500;
        }

        if (balance != 0) {
            super.withdraw(atm, balance);
        }
    }
}

class OneHundredWithdrawProcessor extends CashWithdrawProcessor {
    public OneHundredWithdrawProcessor(CashWithdrawProcessor nextCashWithdrawProcessor) {
        super(nextCashWithdrawProcessor);
    }

    @Override
    public void withdraw(ATM atm, int remainingAmount) {
        int required = remainingAmount / 100;
        int balance = remainingAmount % 100;

        if (required <= atm.getNoOfOneHundredNotes()) {
            atm.deductOneHundredNotes(required);
        } else {
            int available = atm.getNoOfOneHundredNotes();
            atm.deductOneHundredNotes(available);
            balance = balance + (required - available) * 100;
        }

        // Assumption: ATM inventory always covers the requested amount,
        // so we don't expect any leftover here.
    }
}

// ==================== Driver ====================
public class Main {
    public static void main(String[] args) {

        // Setup ATM
        ATM atm = new ATM();
        atm.setAtmBalance(3500, 1, 2, 5);

        // Setup User + Card + Bank Account
        UserBankAccount bankAccount = new UserBankAccount();
        bankAccount.setBalance(3000);

        Card card = new Card();
        card.setBankAccount(bankAccount);

        User user = new User();
        user.setCard(card);

        // Run ATM flow
        atm.printCurrentATMStatus();
        atm.getCurrentATMState().insertCard(atm, user.getCard());
        atm.getCurrentATMState().authenticatePin(atm, user.getCard(), 112211);
        atm.getCurrentATMState().selectOperation(atm, user.getCard(), TransactionType.CASH_WITHDRAWAL);
        atm.getCurrentATMState().cashWithdrawal(atm, user.getCard(), 2700);
        atm.printCurrentATMStatus();
    }
}
