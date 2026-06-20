import java.util.*;

// ========================== ENUMS ==========================

enum BookStatus { AVAILABLE, LOANED, RESERVED }

// ========================== STRATEGY PATTERN FOR FINE ==========================

interface PenaltyStrategy {
    double calculateFine(long daysOverdue);
}

class StandardPenaltyStrategy implements PenaltyStrategy {
    private double dailyRate;

    public StandardPenaltyStrategy(double dailyRate) {
        this.dailyRate = dailyRate;
    }

    public double calculateFine(long daysOverdue) {
        return daysOverdue * dailyRate;
    }
}

class PremiumPenaltyStrategy implements PenaltyStrategy {
    private double dailyRate;

    public PremiumPenaltyStrategy(double dailyRate) {
        this.dailyRate = dailyRate;
    }

    public double calculateFine(long daysOverdue) {
        if (daysOverdue <= 5) {
            return daysOverdue * dailyRate;
        }
        // First 5 days normal, after that double rate
        return (5 * dailyRate) + ((daysOverdue - 5) * dailyRate * 2.0);
    }
}

// ========================== BOOK ==========================

class Book {
    String isbn;
    String title;
    String author;
    BookStatus status;
    Queue<String> reservationQueue;

    public Book(String isbn, String title, String author) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.status = BookStatus.AVAILABLE;
        this.reservationQueue = new LinkedList<>();
    }

    public void reserveMember(String memberId) {
        reservationQueue.add(memberId);
        if (status == BookStatus.AVAILABLE) {
            status = BookStatus.RESERVED;
        }
    }

    public String peekReservation() {
        return reservationQueue.peek();
    }

    public String pollReservation() {
        return reservationQueue.poll();
    }

    public boolean hasReservations() {
        return !reservationQueue.isEmpty();
    }
}

// ========================== LOAN ==========================

class Loan {
    String bookIsbn;
    String memberId;
    long issueDate;
    long dueDate;
    long returnDate;

    public Loan(String bookIsbn, String memberId, long issueDate, long dueDate) {
        this.bookIsbn = bookIsbn;
        this.memberId = memberId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = 0;
    }

    public double calculateFine(PenaltyStrategy strategy) {
        if (returnDate > dueDate) {
            long daysOverdue = (returnDate - dueDate) / (1000L * 60 * 60 * 24);
            return strategy.calculateFine(daysOverdue);
        }
        return 0.0;
    }
}

// ========================== MEMBER ==========================

class Member {
    String id;
    String name;
    List<Loan> activeLoans;
    double unpaidFines;

    public Member(String id, String name) {
        this.id = id;
        this.name = name;
        this.activeLoans = new ArrayList<>();
        this.unpaidFines = 0.0;
    }

    public void addLoan(Loan loan) {
        activeLoans.add(loan);
    }

    public void removeLoan(Loan loan) {
        activeLoans.remove(loan);
    }

    public void addFine(double amount) {
        unpaidFines += amount;
    }
}

// ========================== SEARCH ==========================

class SearchService {
    Map<String, Book> books;

    public SearchService(Map<String, Book> books) {
        this.books = books;
    }

    public List<Book> searchByTitle(String title) {
        List<Book> results = new ArrayList<>();
        for (Book book : books.values()) {
            if (book.title.toLowerCase().contains(title.toLowerCase())) {
                results.add(book);
            }
        }
        return results;
    }

    public List<Book> searchByAuthor(String author) {
        List<Book> results = new ArrayList<>();
        for (Book book : books.values()) {
            if (book.author.toLowerCase().contains(author.toLowerCase())) {
                results.add(book);
            }
        }
        return results;
    }
}

// ========================== LIBRARY SERVICE ==========================

class LibraryService {
    Map<String, Book> books;
    Map<String, Member> members;
    PenaltyStrategy penaltyStrategy;
    SearchService searchService;

    static final int MAX_BOOKS_PER_MEMBER = 5;
    static final double MAX_FINE_ALLOWED = 50.0;

    public LibraryService(PenaltyStrategy penaltyStrategy) {
        this.books = new HashMap<>();
        this.members = new HashMap<>();
        this.penaltyStrategy = penaltyStrategy;
        this.searchService = new SearchService(this.books);
    }

    public void addBook(Book book) {
        books.put(book.isbn, book);
    }

    public void addMember(Member member) {
        members.put(member.id, member);
    }

    public boolean checkoutBook(String memberId, String isbn) {
        Book book = books.get(isbn);
        Member member = members.get(memberId);

        if (book == null || member == null) return false;

        // Block if fines too high
        if (member.unpaidFines > MAX_FINE_ALLOWED) {
            System.out.println("Checkout rejected: " + member.name + " has fines of $" + member.unpaidFines);
            return false;
        }

        // Block if member already has max books
        if (member.activeLoans.size() >= MAX_BOOKS_PER_MEMBER) {
            System.out.println("Checkout rejected: " + member.name + " already has " + MAX_BOOKS_PER_MEMBER + " books.");
            return false;
        }

        // Check book availability
        if (book.status == BookStatus.AVAILABLE) {
            book.status = BookStatus.LOANED;

        } else if (book.status == BookStatus.RESERVED) {
            String nextInLine = book.peekReservation();
            if (memberId.equals(nextInLine)) {
                book.pollReservation(); // remove only after confirming it's their turn
                book.status = BookStatus.LOANED;
            } else {
                System.out.println("Checkout rejected: Book is reserved for someone else.");
                return false;
            }

        } else {
            System.out.println("Checkout rejected: Book is already loaned out.");
            return false;
        }

        // Create loan with 14-day period
        long now = System.currentTimeMillis();
        long dueDate = now + (14L * 24 * 60 * 60 * 1000);
        Loan loan = new Loan(isbn, memberId, now, dueDate);
        member.addLoan(loan);

        System.out.println("Checkout successful: \"" + book.title + "\" to " + member.name);
        return true;
    }

    public void returnBook(String memberId, String isbn, long returnTime) {
        Book book = books.get(isbn);
        Member member = members.get(memberId);

        if (book == null || member == null) return;

        // Find the active loan for this book
        Loan targetLoan = null;
        for (Loan loan : member.activeLoans) {
            if (loan.bookIsbn.equals(isbn)) {
                targetLoan = loan;
                break;
            }
        }

        if (targetLoan == null) return;

        // Set return date and calculate fine
        targetLoan.returnDate = returnTime;
        double fine = targetLoan.calculateFine(penaltyStrategy);

        if (fine > 0) {
            member.addFine(fine);
            System.out.println("Overdue! \"" + book.title + "\" returned. Fine: $" + fine);
        } else {
            System.out.println("\"" + book.title + "\" returned on time. No fine.");
        }

        member.removeLoan(targetLoan);

        // Update book status
        if (book.hasReservations()) {
            book.status = BookStatus.RESERVED;
        } else {
            book.status = BookStatus.AVAILABLE;
        }
    }

    public boolean renewBook(String memberId, String isbn) {
        Book book = books.get(isbn);
        Member member = members.get(memberId);

        if (book == null || member == null) return false;

        // Can't renew if someone else is waiting for this book
        if (book.hasReservations()) {
            System.out.println("Renew rejected: \"" + book.title + "\" has pending reservations.");
            return false;
        }

        // Find the active loan
        Loan targetLoan = null;
        for (Loan loan : member.activeLoans) {
            if (loan.bookIsbn.equals(isbn)) {
                targetLoan = loan;
                break;
            }
        }

        if (targetLoan == null) {
            System.out.println("Renew rejected: No active loan found.");
            return false;
        }

        // Extend due date by another 14 days from today
        long now = System.currentTimeMillis();
        targetLoan.dueDate = now + (14L * 24 * 60 * 60 * 1000);

        System.out.println("Renewed: \"" + book.title + "\" for " + member.name + " (14 more days)");
        return true;
    }

    public void reserveBook(String memberId, String isbn) {
        Book book = books.get(isbn);
        if (book == null) return;

        book.reserveMember(memberId);
        Member member = members.get(memberId);
        System.out.println("\"" + book.title + "\" reserved for " + member.name);
    }
}

// ========================== DRIVER ==========================

public class Main {
    public static void main(String[] args) {
        System.out.println("=== LIBRARY MANAGEMENT SYSTEM ===\n");

        // Setup library with premium fine strategy ($2/day, doubles after 5 days)
        PenaltyStrategy strategy = new PremiumPenaltyStrategy(2.0);
        LibraryService library = new LibraryService(strategy);

        // Add books
        Book b1 = new Book("ISBN-11", "Clean Architecture", "Robert C. Martin");
        Book b2 = new Book("ISBN-22", "Design Patterns", "Gang of Four");
        library.addBook(b1);
        library.addBook(b2);

        // Add members
        Member alice = new Member("MEM-1", "Alice");
        Member bob = new Member("MEM-2", "Bob");
        library.addMember(alice);
        library.addMember(bob);

        // --- Scenario 1: Normal checkout and late return ---
        System.out.println("--- Scenario 1: Checkout + Late Return ---");
        library.checkoutBook("MEM-1", "ISBN-11");

        // Alice returns 8 days late (14 day loan + 8 days overdue = 22 days total)
        long now = System.currentTimeMillis();
        long lateReturn = now + (22L * 24 * 60 * 60 * 1000);
        library.returnBook("MEM-1", "ISBN-11", lateReturn);
        System.out.println("Alice fines: $" + alice.unpaidFines);
        // Fine = (5 * $2) + (3 * $4) = $10 + $12 = $22

        // --- Scenario 2: Reservation flow ---
        System.out.println("\n--- Scenario 2: Reservation ---");
        library.checkoutBook("MEM-1", "ISBN-22");   // Alice takes Design Patterns
        library.reserveBook("MEM-2", "ISBN-22");    // Bob reserves it

        // Alice returns on time
        long onTimeReturn = now + (10L * 24 * 60 * 60 * 1000);
        library.returnBook("MEM-1", "ISBN-22", onTimeReturn);

        // Bob can now checkout (he's next in queue)
        library.checkoutBook("MEM-2", "ISBN-22");

        // --- Scenario 3: Blocked by fines ---
        System.out.println("\n--- Scenario 3: Blocked by fines ---");
        alice.unpaidFines = 60.0; // simulate high fines
        library.checkoutBook("MEM-1", "ISBN-11"); // should be rejected

        // --- Scenario 4: Search ---
        System.out.println("\n--- Scenario 4: Search ---");
        alice.unpaidFines = 0; // reset for demo
        List<Book> results = library.searchService.searchByAuthor("Robert");
        System.out.println("Search by author 'Robert': Found " + results.size() + " book(s)");
        for (Book b : results) {
            System.out.println("  -> " + b.title + " by " + b.author);
        }

        results = library.searchService.searchByTitle("design");
        System.out.println("Search by title 'design': Found " + results.size() + " book(s)");
        for (Book b : results) {
            System.out.println("  -> " + b.title + " by " + b.author);
        }

        // --- Scenario 5: Renew ---
        System.out.println("\n--- Scenario 5: Renew Book ---");
        library.checkoutBook("MEM-1", "ISBN-11");  // Alice takes Clean Architecture
        library.renewBook("MEM-1", "ISBN-11");     // Alice renews it (14 more days)

        // Try renewing a book that someone else reserved
        library.reserveBook("MEM-2", "ISBN-11");   // Bob reserves it
        library.renewBook("MEM-1", "ISBN-11");     // Alice tries to renew — rejected
    }
}
