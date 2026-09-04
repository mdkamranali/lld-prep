import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        Splitwise splitwise = new Splitwise();
        splitwise.demo();
    }

    // ==================== USER ====================

    static class User {
        String userId;
        String userName;
        UserExpenseBalanceSheet userExpenseBalanceSheet;

        public User(String userId, String userName) {
            this.userId = userId;
            this.userName = userName;
            this.userExpenseBalanceSheet = new UserExpenseBalanceSheet();
        }

        public String getUserId() { return userId; }
        public String getUserName() { return userName; }
        public UserExpenseBalanceSheet getUserExpenseBalanceSheet() { return userExpenseBalanceSheet; }
    }

    static class UserController {
        List<User> userList = new ArrayList<>();

        public void addUser(User user) { userList.add(user); }

        public User getUser(String userId) {
            for (User user : userList) {
                if (user.getUserId().equals(userId)) return user;
            }
            return null;
        }

        public List<User> getAllUsers() { return userList; }
    }

    // ==================== BALANCE ====================

    static class UserExpenseBalanceSheet {
        // For each other user: how much they will give BACK to me (they owe me).
        Map<String, Double> getBackFrom = new HashMap<>();
        // For each other user: how much I must GIVE to them (I owe them).
        Map<String, Double> owedTo = new HashMap<>();

        public Map<String, Double> getGetBackFrom() { return getBackFrom; }
        public Map<String, Double> getOwedTo() { return owedTo; }
    }

    // ==================== SPLIT ====================

    static class Split {
        User user;
        double amountOwe;

        public Split(User user, double amountOwe) {
            this.user = user;
            this.amountOwe = amountOwe;
        }

        public User getUser() { return user; }
        public double getAmountOwe() { return amountOwe; }
    }

    // ==================== EXPENSE SPLIT (Strategy) ====================
    // Each strategy carries its own input data and knows how to produce the final splits.

    interface ExpenseSplit {
        List<Split> computeSplits(double totalAmount);
    }

    static class EqualExpenseSplit implements ExpenseSplit {
        List<User> participants;

        public EqualExpenseSplit(List<User> participants) {
            this.participants = participants;
        }

        @Override
        public List<Split> computeSplits(double totalAmount) {
            double share = totalAmount / participants.size();
            List<Split> splits = new ArrayList<>();
            for (User u : participants) splits.add(new Split(u, share));
            return splits;
        }
    }

    static class UnequalExpenseSplit implements ExpenseSplit {
        Map<User, Double> userAmounts;

        public UnequalExpenseSplit(Map<User, Double> userAmounts) {
            this.userAmounts = userAmounts;
        }

        @Override
        public List<Split> computeSplits(double totalAmount) {
            double sum = 0;
            for (double a : userAmounts.values()) sum += a;
            if (sum != totalAmount) {
                throw new IllegalArgumentException("Unequal split total (" + sum + ") != expense amount (" + totalAmount + ")");
            }
            List<Split> splits = new ArrayList<>();
            for (var entry : userAmounts.entrySet()) {
             splits.add(new Split(entry.getKey(), entry.getValue()));
            }
            return splits;
        }
    }

    static class PercentageExpenseSplit implements ExpenseSplit {
        Map<User, Double> userPercents;

        public PercentageExpenseSplit(Map<User, Double> userPercents) {
            this.userPercents = userPercents;
        }

        @Override
        public List<Split> computeSplits(double totalAmount) {
            double sum = 0;
            for (double p : userPercents.values()) sum += p;
            if (sum != 100) {
                throw new IllegalArgumentException("Percentage split total (" + sum + ") != 100");
            }
            List<Split> splits = new ArrayList<>();
            for (Map.Entry<User, Double> e : userPercents.entrySet()) {
                splits.add(new Split(e.getKey(), (e.getValue() / 100.0) * totalAmount));
            }
            return splits;
        }
    }

    // ==================== EXPENSE ====================

    static class Expense {
        String expenseId;
        String description;
        double expenseAmount;
        User paidByUser;
        List<Split> splitDetails;

        public Expense(String expenseId, String description, double expenseAmount,
                       User paidByUser, List<Split> splitDetails) {
            this.expenseId = expenseId;
            this.description = description;
            this.expenseAmount = expenseAmount;
            this.paidByUser = paidByUser;
            this.splitDetails = splitDetails;
        }
    }

    static class ExpenseController {
        BalanceSheetController balanceSheetController = new BalanceSheetController();

        public Expense createExpense(String expenseId, String description, double expenseAmount,
                                     ExpenseSplit splitStrategy, User paidByUser) {
            List<Split> splits = splitStrategy.computeSplits(expenseAmount);
            Expense expense = new Expense(expenseId, description, expenseAmount, paidByUser, splits);
            balanceSheetController.updateUserExpenseBalanceSheet(paidByUser, splits);
            return expense;
        }
    }

    // ==================== GROUP ====================

    static class Group {
        String groupId;
        String groupName;
        List<User> groupMembers = new ArrayList<>();
        List<Expense> expenseList = new ArrayList<>();
        ExpenseController expenseController = new ExpenseController();

        public void addMember(User member) { groupMembers.add(member); }
        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) { this.groupId = groupId; }
        public void setGroupName(String groupName) { this.groupName = groupName; }

        public Expense createExpense(String expenseId, String description, double expenseAmount,
                                     ExpenseSplit splitStrategy, User paidByUser) {
            Expense expense = expenseController.createExpense(
                    expenseId, description, expenseAmount, splitStrategy, paidByUser);
            expenseList.add(expense);
            return expense;
        }
    }

    static class GroupController {
        List<Group> groupList = new ArrayList<>();

        public void createNewGroup(String groupId, String groupName, User createdByUser) {
            Group group = new Group();
            group.setGroupId(groupId);
            group.setGroupName(groupName);
            group.addMember(createdByUser);
            groupList.add(group);
        }

        public Group getGroup(String groupId) {
            for (Group group : groupList) {
                if (group.getGroupId().equals(groupId)) return group;
            }
            return null;
        }
    }

    // ==================== BALANCE SHEET CONTROLLER ====================

    static class BalanceSheetController {

        public void updateUserExpenseBalanceSheet(User paidBy, List<Split> splits) {
            for (Split split : splits) {
                User ower = split.getUser();
                if (paidBy.getUserId().equals(ower.getUserId())) continue;  // skip self

                double amount = split.getAmountOwe();

                // paidBy will GET BACK `amount` from `ower`
                addAmount(paidBy.getUserExpenseBalanceSheet().getGetBackFrom(), ower.getUserId(), amount);

                // `ower` must GIVE `amount` to paidBy
                addAmount(ower.getUserExpenseBalanceSheet().getOwedTo(), paidBy.getUserId(), amount);
            }
        }

        private void addAmount(Map<String, Double> map, String userId, double amount) {
            Double existing = map.get(userId);
            if (existing == null) existing = 0.0;
            map.put(userId, existing + amount);
        }

        public void showBalanceSheetOfUser(User user) {
            System.out.println("---------------------------------------");
            System.out.println("Balance sheet of user : " + user.getUserId());
            UserExpenseBalanceSheet sheet = user.getUserExpenseBalanceSheet();

            for (Map.Entry<String, Double> e : sheet.getGetBackFrom().entrySet()) {
                System.out.println("  Get back from " + e.getKey() + " : " + e.getValue());
            }
            for (Map.Entry<String, Double> e : sheet.getOwedTo().entrySet()) {
                System.out.println("  Owe to        " + e.getKey() + " : " + e.getValue());
            }
            System.out.println("---------------------------------------");
        }
    }

    // ==================== SPLITWISE (Facade) ====================

    static class Splitwise {
        UserController userController = new UserController();
        GroupController groupController = new GroupController();
        BalanceSheetController balanceSheetController = new BalanceSheetController();

        public void demo() {
            setupUserAndGroup();

            Group group = groupController.getGroup("G1001");
            User u1 = userController.getUser("U1001");
            User u2 = userController.getUser("U2001");
            User u3 = userController.getUser("U3001");

            group.addMember(u2);
            group.addMember(u3);

            // EQUAL — just list the participants.
            group.createExpense("Exp1001", "Breakfast", 900,
                    new EqualExpenseSplit(List.of(u1, u2, u3)), u1);

            // UNEQUAL — per-user amounts.
            Map<User, Double> lunchAmounts = new LinkedHashMap<>();
            lunchAmounts.put(u1, 400.0);
            lunchAmounts.put(u2, 100.0);
            group.createExpense("Exp1002", "Lunch", 500,
                    new UnequalExpenseSplit(lunchAmounts), u2);

            // PERCENTAGE — per-user percentages (must sum to 100).
            Map<User, Double> dinnerPercents = new LinkedHashMap<>();
            dinnerPercents.put(u1, 50.0);   // u1 pays 50% of 600 = 300
            dinnerPercents.put(u2, 30.0);   // u2 pays 30% of 600 = 180
            dinnerPercents.put(u3, 20.0);   // u3 pays 20% of 600 = 120
            group.createExpense("Exp1003", "Dinner", 600,
                    new PercentageExpenseSplit(dinnerPercents), u3);

            for (User user : userController.getAllUsers()) {
                balanceSheetController.showBalanceSheetOfUser(user);
            }
        }

        private void setupUserAndGroup() {
            userController.addUser(new User("U1001", "User1"));
            userController.addUser(new User("U2001", "User2"));
            userController.addUser(new User("U3001", "User3"));
            groupController.createNewGroup("G1001", "Outing with Friends", userController.getUser("U1001"));
        }
    }
}
