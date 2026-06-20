# Library Management System - Low Level Design

## UML Class Diagram (Text Representation)

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           LIBRARY MANAGEMENT SYSTEM                                  │
└─────────────────────────────────────────────────────────────────────────────────────┘

                            ┌──────────────────────┐
                            │      <<enum>>        │
                            │     BookStatus       │
                            ├──────────────────────┤
                            │ AVAILABLE            │
                            │ ISSUED               │
                            │ RESERVED             │
                            │ LOST                 │
                            └──────────────────────┘

┌──────────────────┐       ┌──────────────────────┐       ┌──────────────────┐
│   <<enum>>       │       │      <<enum>>        │       │   <<enum>>       │
│   BookType       │       │  ReservationStatus   │       │   BookFormat     │
├──────────────────┤       ├──────────────────────┤       ├──────────────────┤
│ SCI_FI           │       │ WAITING              │       │ HARDCOVER        │
│ ROMANTIC         │       │ COMPLETED            │       │ PAPERBACK        │
│ FANTASY          │       │ CANCELLED            │       │ NEWSPAPER        │
│ DRAMA            │       └──────────────────────┘       │ JOURNAL          │
│ TECHNICAL        │                                       └──────────────────┘
│ HISTORY          │
└──────────────────┘

┌──────────────────────┐          ┌──────────────────────┐
│      Address         │          │        Rack          │
├──────────────────────┤          ├──────────────────────┤
│ pinCode: int         │          │ number: int          │
│ street: String       │          │ locationId: String   │
│ city: String         │          └──────────────────────┘
│ state: String        │                    │
│ country: String      │                    │ placed on
└──────────────────────┘                    │
         │                                  ▼
         │ has                 ┌──────────────────────────────┐
         │                     │           Book               │
         │                     ├──────────────────────────────┤
         │                     │ isbn: String                 │
         │                     │ title: String                │
         │                     │ author: String               │
         │                     │ bookType: BookType           │
         │                     │ bookFormat: BookFormat        │
         │                     │ status: BookStatus           │
         │                     │ publicationDate: Date        │
         │                     │ rack: Rack                   │
         │                     │ reservationQueue: Queue      │
         │                     ├──────────────────────────────┤
         │                     │ +addReservation(memberId)    │
         │                     │ +getNextReservation(): String│
         │                     │ +hasReservations(): boolean  │
         │                     └──────────────────────────────┘
         │                                  │
         ▼                                  │ contains *
┌─────────────────────────────────────────────────────────────────┐
│                          Library                                 │
├─────────────────────────────────────────────────────────────────┤
│ name: String                                                    │
│ address: Address                                                │
│ bookMap: Map<String, Book>                                      │
│ memberMap: Map<String, Member>                                  │
│ fineStrategy: FineStrategy                                      │
│ MAX_BOOKS_PER_MEMBER = 5                                        │
│ LOAN_PERIOD_DAYS = 14                                           │
│ MAX_FINE_ALLOWED = 50.0                                         │
├─────────────────────────────────────────────────────────────────┤
│ +addBook(book)                                                  │
│ +removeBook(isbn)                                               │
│ +registerMember(member)                                         │
│ +issueBook(memberId, isbn): BookIssueDetail                     │
│ +returnBook(memberId, isbn)                                     │
│ +returnBookWithDate(memberId, isbn, date)                       │
│ +reserveBook(memberId, isbn): BookReservationDetail             │
│ +renewBook(memberId, isbn): BookIssueDetail                     │
│ +getSearchService(): SearchService                              │
└─────────────────────────────────────────────────────────────────┘
         │                                           │
         │ has *                                     │ uses
         ▼                                           ▼
┌──────────────────────────┐          ┌──────────────────────────────┐
│        Person            │          │   <<interface>>              │
├──────────────────────────┤          │     FineStrategy             │
│ firstName: String        │          ├──────────────────────────────┤
│ lastName: String         │          │ +calculateFine(days): double │
│ email: String            │          └──────────────────────────────┘
│ phoneNumber: String      │                   ▲           ▲
├──────────────────────────┤                   │           │
│ +getFullName(): String   │                   │           │
└──────────────────────────┘          ┌────────┘           └────────┐
         ▲              ▲             │                             │
         │              │    ┌────────────────────┐   ┌─────────────────────────┐
         │              │    │ StandardFineStrategy│   │  PremiumFineStrategy    │
         │              │    ├────────────────────┤   ├─────────────────────────┤
         │              │    │ dailyRate: double  │   │ dailyRate: double       │
         │              │    ├────────────────────┤   ├─────────────────────────┤
         │              │    │ +calculateFine()   │   │ +calculateFine()        │
         │              │    └────────────────────┘   │ (doubles after 5 days)  │
         │              │                              └─────────────────────────┘
         │              │
┌────────┴─────────┐  ┌┴─────────────────────┐
│     Member       │  │     Librarian         │
├──────────────────┤  ├───────────────────────┤
│ memberId: String │  │ employeeId: String    │
│ account: Account │  │ account: Account      │
│ totalBooksOut:int│  ├───────────────────────┤
│ unpaidFines:dbl  │  │ +addBook(lib, book)   │
│ activeLoans: List│  │ +removeBook(lib, isbn)│
├──────────────────┤  └───────────────────────┘
│ +addLoan(loan)   │
│ +removeLoan(loan)│           ┌──────────────────────────┐
│ +addFine(amount) │           │       Account            │
│ +payFine(amount) │           ├──────────────────────────┤
└──────────────────┘           │ username: String         │
         │                     │ password: String         │
         │ has *               │ accountId: int           │
         ▼                     └──────────────────────────┘
┌───────────────────────────┐
│     BookIssueDetail       │
├───────────────────────────┤        ┌────────────────────────────┐
│ book: Book                │        │   BookReservationDetail     │
│ member: Member            │        ├────────────────────────────┤
│ issueDate: Date           │        │ book: Book                 │
│ dueDate: Date             │        │ member: Member             │
│ returnDate: Date          │        │ reservationDate: Date      │
└───────────────────────────┘        │ status: ReservationStatus  │
                                     └────────────────────────────┘

┌──────────────────────────────────────┐
│          SearchService               │
├──────────────────────────────────────┤
│ books: List<Book>                    │
├──────────────────────────────────────┤
│ +searchByTitle(title): List<Book>    │
│ +searchByAuthor(author): List<Book>  │
│ +searchByType(type): List<Book>      │
│ +searchByStatus(status): List<Book>  │
└──────────────────────────────────────┘
```

---

## How It All Connects (Simple Explanation)

### The Big Picture

Think of this system as a real library:

```
Library (the building)
  ├── Has Books (on racks)
  ├── Has Members (who borrow books)
  ├── Has Librarians (who manage books)
  └── Has Rules (fines, limits)
```

---

## Class-by-Class Breakdown

### 1. Enums (Fixed Categories)

| Enum | Purpose |
|------|---------|
| `BookStatus` | Is the book available, issued, reserved, or lost? |
| `BookType` | Genre of book (sci-fi, drama, technical, etc.) |
| `BookFormat` | Physical format (hardcover, paperback, etc.) |
| `ReservationStatus` | State of a reservation (waiting, completed, cancelled) |

---

### 2. Book (Single class - no inheritance needed!)

Instead of `Book` + `BookItem extends Book`, we have ONE class:

```java
class Book {
    String isbn;          // unique ID
    String title;
    String author;
    BookType bookType;    // genre
    BookFormat bookFormat; // physical type
    BookStatus status;    // current availability
    Rack rack;            // physical location
    Queue<String> reservationQueue;  // who's waiting for this book
}
```

**Key idea**: The `reservationQueue` maintains a FIFO list of members waiting for this book.

---

### 3. Person → Member / Librarian

```
Person (base: name, email, phone)
   ├── Member (can borrow books, has fines, has active loans)
   └── Librarian (can add/remove books from library)
```

**Member** tracks:
- How many books they currently have
- Their unpaid fines (blocks checkout if > $50)
- List of active loans

**Librarian** can:
- Add new books to the library
- Remove books from the library

---

### 4. Library (The Central Service)

This is the **heart** of the system. It handles ALL operations:

| Method | What it does |
|--------|--------------|
| `issueBook()` | Lend a book to a member (with validations) |
| `returnBook()` | Accept a returned book (calculates fine if late) |
| `reserveBook()` | Queue a member for a book that's not available |
| `renewBook()` | Extend the loan (if no one else is waiting) |
| `getSearchService()` | Get search functionality |

**Validations before issuing:**
1. Member's unpaid fines must be < $50
2. Member can't have more than 5 books checked out
3. Book must be AVAILABLE (or RESERVED for this specific member)

---

### 5. FineStrategy (Strategy Pattern)

Instead of hardcoding fine logic, we use the **Strategy Pattern**:

```
FineStrategy (interface)
   ├── StandardFineStrategy  → flat rate per day
   └── PremiumFineStrategy   → normal for 5 days, doubles after that
```

**Example with PremiumFineStrategy ($2/day):**
- 3 days late → 3 × $2 = $6
- 8 days late → (5 × $2) + (3 × $4) = $10 + $12 = $22

This makes it easy to swap fine calculation logic without changing any other code.

---

### 6. SearchService

Provides multiple ways to find books:
- By title (partial match)
- By author (partial match)
- By genre/type
- By status (find all available books)

---

### 7. Lending Records

| Class | Purpose |
|-------|---------|
| `BookIssueDetail` | Records who borrowed what, when, and due date |
| `BookReservationDetail` | Records who reserved what and current status |

---

## Key Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Strategy** | FineStrategy | Swap fine calculation without changing Library code |
| **Composition** | Library has Books, Members | Flexible relationships, easy to extend |
| **Single Responsibility** | Each class does one thing | Book manages itself, Library orchestrates |
| **Encapsulation** | Member tracks own loans/fines | Data + behavior together |

---

## Flow: What Happens When Someone Borrows a Book?

```
Member wants to borrow "Clean Code"
         │
         ▼
┌─ Library.issueBook("MEM-001", "ISBN-001") ─┐
│                                             │
│  1. Validate member exists                  │
│  2. Check unpaid fines < $50               │
│  3. Check books checked out < 5             │
│  4. Check book status == AVAILABLE          │
│  5. Set book status → ISSUED                │
│  6. Create BookIssueDetail (due in 14 days) │
│  7. Add loan to member's active loans       │
│  8. Return the issue detail                 │
│                                             │
└─────────────────────────────────────────────┘
```

## Flow: What Happens on Return?

```
Member returns "Clean Code"
         │
         ▼
┌─ Library.returnBook("MEM-001", "ISBN-001") ─┐
│                                              │
│  1. Find the active loan for this book       │
│  2. Check if return date > due date          │
│     ├── YES → Calculate fine using strategy  │
│     │         Add fine to member             │
│     └── NO  → No fine                       │
│  3. Remove loan from member                  │
│  4. Check book's reservation queue           │
│     ├── Has waiters → status = RESERVED      │
│     └── Empty       → status = AVAILABLE     │
│                                              │
└──────────────────────────────────────────────┘
```

## Flow: Reservation + Checkout

```
Book "X" is ISSUED to Alice
         │
Bob calls reserveBook("X")
         │
         ▼
Queue: [Bob]  →  Book stays ISSUED (Alice still has it)
         │
Alice returns Book "X"
         │
         ▼
Book status → RESERVED (because queue not empty)
         │
Bob calls issueBook("X")
         │
         ▼
System checks: Is Bob next in queue? → YES → Issue to Bob
```

---

## How to Run

```bash
cd library-management-system/src
javac Main.java
java Main
```

Expected output demonstrates all 6 scenarios:
1. Normal issue and return
2. Late return with fine calculation
3. Reservation workflow
4. Book renewal
5. Search functionality
6. Librarian adding books
