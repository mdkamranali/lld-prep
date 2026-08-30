# Movie Booking System (BookMyShow) — Low Level Design

Design a movie ticket booking system where users can browse movies by city,
pick a theatre → show → seat(s), pay, and get a confirmed booking. Handle the
concurrency problem of two users trying to book the same seat at the same
time using **Optimistic Concurrency Control (OCC)**.

## UML Class Diagram

```
┌─────────────────────┐          ┌─────────────────────┐
│      <<enum>>       │          │      <<enum>>       │
│        City         │          │    SeatCategory     │
│─────────────────────│          │─────────────────────│
│  Bangalore          │          │  SILVER (100)       │
│  Delhi              │          │  GOLD   (150)       │
└─────────────────────┘          │  PLATINUM (250)     │
                                 │─────────────────────│
                                 │ + getPrice(): int   │
                                 └─────────────────────┘

┌────────────────────────────┐        ┌────────────────────────────┐
│           Movie            │        │            Seat            │
│────────────────────────────│        │────────────────────────────│
│ - movieId: int             │        │ - seatId: int              │
│ - movieName: String        │        │ - row: int                 │
│ - movieDurationInMinutes   │        │ - seatCategory:SeatCategory│
│────────────────────────────│        │────────────────────────────│
│ + getters / setters        │        │ + getters / setters        │
└────────────────────────────┘        └────────────────────────────┘
                                                 ▲
                                                 │ has many
┌────────────────────────────┐         ┌────────────────────────────┐
│          Screen            │◀────────│          Theatre           │
│────────────────────────────│  has    │────────────────────────────│
│ - screenId: int            │         │ - theatreId: int           │
│ - seats: List<Seat>        │         │ - address: String          │
│────────────────────────────│         │ - city: City               │
│ + getSeats(): List<Seat>   │         │ - screen: List<Screen>     │
└────────────────────────────┘         │ - shows: List<Show>        │
        ▲                              │────────────────────────────│
        │ runs on                      │ + getters / setters        │
        │                              └────────────────────────────┘
┌──────────────────────────────────┐             ▲
│              Show                │             │ managed by
│──────────────────────────────────│             │
│ - showId: int                    │   ┌───────────────────────────────┐
│ - movie: Movie                   │   │      TheatreController        │
│ - screen: Screen                 │   │───────────────────────────────│
│ - showStartTime: int (24-hr)     │   │ - cityVsTheatre:Map<City,List>│
│ - bookedSeatIds: List<Integer>   │   │ - allTheatre: List<Theatre>   │
│──────────────────────────────────│   │───────────────────────────────│
│ + getBookedSeatIds(): List<Int>  │   │ + addTheatre(t, city)         │
│ + setBookedSeatIds(...)          │   │ + getAllShow(movie, city):    │
└──────────────────────────────────┘   │      Map<Theatre,List<Show>>  │
                                       └───────────────────────────────┘

┌──────────────────────────────┐        ┌───────────────────────────────┐
│           Payment            │        │        MovieController        │
│──────────────────────────────│        │───────────────────────────────│
│ - paymentId: int             │        │ - cityVsMovies:Map<City,List> │
│ - amount: int                │        │ - allMovies: List<Movie>      │
│ - status: String             │        │───────────────────────────────│
│──────────────────────────────│        │ + addMovie(m, city)           │
│ + process(): boolean         │        │ + getMovieByName(name):Movie  │
│ + getters / setters          │        │ + getMoviesByCity(city):List  │
└──────────────────────────────┘        └───────────────────────────────┘
        ▲
        │ has one
        │
┌──────────────────────────────┐
│           Booking            │
│──────────────────────────────│
│ - show: Show                 │
│ - bookedSeatIds: List<Int>   │
│ - payment: Payment           │
│──────────────────────────────│
│ + getters / setters          │
└──────────────────────────────┘
        ▲
        │ persisted in
        │
┌────────────────────────────────────────────────┐
│                  BookMyShow                    │
│────────────────────────────────────────────────│
│ - movieController: MovieController             │
│ - theatreController: TheatreController         │
│ - allBookings: List<Booking>                   │
│────────────────────────────────────────────────│
│ + createBooking(city, movie, theatreId,        │
│                 showId, seatNumbers): void     │
│ + initialize(): void                           │
└────────────────────────────────────────────────┘
```

---

## Flow — Start to End

### 1. Setup — `bookMyShow.initialize()`

- **Movies** are created and mapped to cities (an Avengers movie is added to both
  Bangalore & Delhi, so are Baahubali). `MovieController` keeps a de-duplicated
  `allMovies` flat catalog plus a per-city `cityVsMovies` map.
- **Theatres** are created (Inox in Bangalore, PVR in Delhi). Each theatre has
  one **Screen** with **100 Seats**:
  - `0–39`  → SILVER (₹100)
  - `40–69` → GOLD   (₹150)
  - `70–99` → PLATINUM (₹250)
- Each theatre runs 2 **Shows** (Avengers morning, Baahubali evening). Shows
  point back to a `Screen` (physical seat layout) and carry their own
  `bookedSeatIds` (per-show availability).

### 2. Booking — `createBooking(city, movieName, theatreId, showId, seatNumbers)`

A user makes 5 choices — the API accepts all 5:

```
city ─▶ movie ─▶ theatre ─▶ show ─▶ seat(s)
```

Steps inside the method:

1. **Search movies in the user's city** — `MovieController.getMoviesByCity(city)`.
2. **Pick the requested movie** — linear scan on movie name.
3. **Get all theatres × shows for this movie in this city** —
   `TheatreController.getAllShow(movie, city)` returns `Map<Theatre, List<Show>>`.
4. **Filter to the specific theatre + show** the user picked (`theatreId`, `showId`).
5. **Check seat availability** — for each requested seat, ensure it isn't already in
   `show.bookedSeatIds`. If any is taken → fail fast, no booking.
6. **Calculate total cost** — for each seat, look up its `Seat` object from
   `screen.getSeats()`, then sum `seatCategory.getPrice()`.
7. **Attempt payment** — `Payment.process()` returns `true`/`false`. In this
   demo it's a stub that always returns `true`; in reality it would call a real
   payment gateway (Razorpay / Stripe / UPI) with `totalCost`.
8. **On payment failure** → do NOT book anything, print an error, return.
9. **On payment success** → NOW (and only now):
   - add all seat numbers to `show.bookedSeatIds`,
   - build a `Booking` linking `show + seatNumbers + payment`,
   - save it into `BookMyShow.allBookings` so it's actually persisted.

```
BOOKING SUCCESSFUL -> movie=BAAHUBALI, theatreId=1, showId=2, seats=[30], amount=Rs.100, paymentId=48213
```

---

## Concurrency — Optimistic Concurrency Control (OCC)

### Problem
> Two users clicking "Book" on the **same seat** at the same time — how do we
> make sure only ONE of them wins?

### Why not pessimistic locking?
Locking the seat the moment a user *clicks* it would kill UX (seat "held" for
someone who may never pay) and throughput (only one user allowed at a time
even during browsing). Bad idea.

### OCC in plain English
1. When you click a seat, the system **secretly remembers the state (version)
   you saw** (e.g. `Available @ v1`).
2. Both you and your friend see the seat as **Available** — no lock.
3. Whoever clicks **Pay** first is checked:
   *"Is the seat still in the same state (v1) that this person saw?"*
   - **YES** → their update goes through, DB row-version becomes `v2`, seat
     marked booked.
   - **NO**  → their commit is **rejected**, they must re-read the seat, which
     now shows "already booked".

### With a concrete example (seat #30, starts at `v1`)

```
U1  ──▶  READ  seat#30   (sees v1)   ─┐
                                       │  both readers can co-exist —
U2  ──▶  READ  seat#30   (sees v1)   ─┘  no lock during read

U2  ──▶  UPDATE (pay)
          DB checks: row.version == v1 ?  ✓
          → locks row briefly, flips v1 → v2, marks booked, unlocks

U1  ──▶  UPDATE (pay)
          DB checks: row.version == v1 ?  ✗ (row is now v2)
          → UPDATE fails; U1 must re-read
          → on re-read, seat shows already booked
```

### Key properties of OCC
- **Reads are lock-free.** Any number of users can browse simultaneously.
- **Version column per row** is the truth about "who saw what state".
- **Row lock is taken only during the tiny UPDATE window**, then released.
- Perfect fit when **conflicts are rare** (most booking clicks *don't* collide).

### Status in this codebase
The full OCC compare-and-swap logic is **documented** at the top of `Main.java`
and in this README, but is **not yet implemented in code** — the current
booking flow does a simple `contains` check followed by an `add`, which is a
placeholder. Adding OCC would mean:

- Add a `version` field on `Show`.
- Snapshot `version` at read time in `createBooking`.
- Replace the `bookedSeats.add(...)` block with a `synchronized`
  compare-and-swap: verify `version == snapshot` before bumping to
  `version + 1`, else force a re-read.

---

## Design Ideas Used

| Pattern / Idea                      | Where                                                | Why                                                            |
|-------------------------------------|------------------------------------------------------|----------------------------------------------------------------|
| Controller pattern (MVC-ish)        | `MovieController`, `TheatreController`               | Isolate persistence & query logic from the booking orchestration |
| Enum-with-data                      | `SeatCategory(SILVER/GOLD/PLATINUM)` carries `price` | Pricing lives with the category, not in a separate table       |
| Facade                              | `BookMyShow.createBooking`                           | One entry point hides the 9-step booking pipeline              |
| Two-phase commit (payment then book)| `payment.process()` → then `show.setBookedSeatIds`  | Seats are only marked taken **after** payment succeeds         |
| Optimistic Concurrency Control      | Documented (not yet coded)                           | Safe seat allocation under concurrent booking traffic          |

---

## Key Classes at a Glance

| Class                | Responsibility                                                       |
|----------------------|----------------------------------------------------------------------|
| `City` (enum)        | Bangalore / Delhi                                                    |
| `SeatCategory` (enum)| SILVER / GOLD / PLATINUM + per-seat price                            |
| `Movie`              | Movie metadata (id, name, duration)                                  |
| `Seat`               | A physical chair on a screen (id, row, category)                     |
| `Screen`             | Physical layout — one `List<Seat>` (100 seats in this demo)          |
| `Show`               | A specific movie+screen at a specific start time; holds `bookedSeatIds` |
| `Theatre`            | A venue in a city that owns screens and runs shows                   |
| `Payment`            | Payment record + `process()` gateway stub                            |
| `Booking`            | User's confirmed record: show + seat numbers + payment               |
| `MovieController`    | CRUD for movies, per-city lookup                                     |
| `TheatreController`  | CRUD for theatres, "shows for this movie in this city" query         |
| `BookMyShow`         | Orchestrator — holds controllers + `allBookings`, exposes `createBooking` |
| `Main`               | Driver — wires up sample data and triggers two competing bookings    |

---

## Sample Run (from `Main.main`)

Two users try to book **seat 30** for `BAAHUBALI` at `Inox, Bangalore, 4 PM`:

```
BOOKING SUCCESSFUL -> movie=BAAHUBALI, theatreId=1, showId=2, seats=[30], amount=Rs.100, paymentId=48213
seat 30 already booked, try again
```

The second user's request fails because seat 30 is now in `show.bookedSeatIds`.
Under real concurrent load, this is exactly the collision that OCC (see above)
resolves cleanly with a version check.
