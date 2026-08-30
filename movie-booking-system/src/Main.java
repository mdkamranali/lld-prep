import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 =============================================================================
                    CONCURRENCY HANDLING IN BOOK-MY-SHOW
                    (Based on LLD discussion / my notes)
 =============================================================================

 PROBLEM:
 --------
 When booking a seat, how can we ensure that no two users get the same seat
 number when they try to book at the exact same time?

 SOLUTION: OPTIMISTIC CONCURRENCY CONTROL (OCC)
 ----------------------------------------------
 We do NOT lock the seat while the user is browsing/selecting. Instead we
 assume that conflicts are RARE (optimistic assumption) and only verify at
 the very last moment (payment/commit time) whether the state has changed
 since the user first read it.

 END-TO-END FLOW (real-world example):
 -------------------------------------
   1. You & your friend are both looking at the LAST available seat.
   2. The system does NOT hold the seat for anyone -> NO locking during
      browse/selection.
   3. Both of you see "Available" on your screens.
   4. You go to pay; your friend also goes to pay.
   5. Whoever clicks "Pay" first gets the seat.
   6. The other person, when they try to pay, gets:
         "Sorry, someone else booked this seat just now"

 HOW OCC KNOWS WHO WINS (the trick behind the scenes):
 -----------------------------------------------------
   Step 1: When you clicked on the seat, the system SECRETLY REMEMBERS
           the state (version) that you saw ("Available", version = v1).

   Step 2: When you pay, the system checks:
           "Is the seat still in the SAME state that this person saw?"
             - If YES  -> you get it (commit succeeds)
             - If NO   -> you LOSE, because someone else already changed it

 EXAMPLE WITH VERSIONS:
 ----------------------
   Consider seat #30 currently at version v1.

     U1 -> READ  seat#30   (sees v1)          <-- remembers v1
     U2 -> READ  seat#30   (also sees v1)     <-- remembers v1

     U2 -> UPDATE (i.e. pay & book)
             At this point the DB checks:
               "Is the current row-version == the v1 that U2 read?"
             Yes -> DB briefly LOCKS this single row, flips version
                    v1 -> v2, marks it booked, and UNLOCKS.

     U1 -> UPDATE (i.e. pay & book)
             The DB again checks:
               "Is the current row-version == the v1 that U1 read?"
             But the row is now at v2 (U2 already updated it).
             So U1's update FAILS. The system asks U1 to READ AGAIN.
             On re-read U1 now sees v2 -> "seat already booked".

 KEY POINTS:
 -----------
   - In OPTIMISTIC concurrency control, MULTIPLE transactions are allowed
     to READ the same row concurrently -- there is no read-lock, no
     seat-hold.
   - A VERSION NUMBER is maintained for EVERY ROW (here: every seat/booking
     record).
   - The tiny row-level lock is taken only at the instant of UPDATE, and
     released immediately after the version bump. So contention window is
     extremely small.
   - Contrast with PESSIMISTIC locking: it would lock the seat the moment
     a user clicks it, blocking everyone else -> bad UX, low throughput.

 NOTE: The code below does NOT yet implement OCC. It is a single-threaded
 skeleton for the design. In a real system, `Show` would carry a `version`
 field and the booking commit would be a compare-and-swap on that version.
 =============================================================================
*/
class BookMyShow {

    MovieController movieController;
    TheatreController theatreController;
    List<Booking> allBookings; // stores every successful booking made by users

    BookMyShow() {
        movieController = new MovieController();
        theatreController = new TheatreController();
        allBookings = new ArrayList<>();
    }

    /*
     * Real-world booking flow:
     *   city  -> movie  -> theatre  -> show  -> seat(s)
     *
     * So the API needs all five pieces of info from the user:
     *   userCity      : which city they are in
     *   movieName     : which movie they want to watch
     *   theatreId     : which theatre they picked
     *   showId        : which show (time) inside that theatre
     *   seatNumbers   : the specific seat number(s) they want to book
     */
    void createBooking(City userCity, String movieName, int theatreId,
                       int showId, List<Integer> seatNumbers) {

        // 1. search movies available in the user's city
        List<Movie> movies = movieController.getMoviesByCity(userCity);
        if (movies == null || movies.isEmpty()) {
            System.out.println("No movies available in " + userCity);
            return;
        }

        // 2. select the movie the user wants to watch
        Movie interestedMovie = null;
        for (Movie movie : movies) {
            if (movie.getMovieName().equals(movieName)) {
                interestedMovie = movie;
                break;
            }
        }
        if (interestedMovie == null) {
            System.out.println("Movie " + movieName + " not found in " + userCity);
            return;
        }

        // 3. get all theatres × shows for this movie in this city
        Map<Theatre, List<Show>> showsTheatreWise =
                theatreController.getAllShow(interestedMovie, userCity);
        if (showsTheatreWise.isEmpty()) {
            System.out.println("No shows found for " + movieName + " in " + userCity);
            return;
        }

        // 4. select the theatre the user picked, then the specific show inside it
        Theatre interestedTheatre = null;
        Show interestedShow = null;
        for (Map.Entry<Theatre, List<Show>> entry : showsTheatreWise.entrySet()) {
            if (entry.getKey().getTheatreId() == theatreId) {
                interestedTheatre = entry.getKey();
                for (Show show : entry.getValue()) {
                    if (show.getShowId() == showId) {
                        interestedShow = show;
                        break;
                    }
                }
                break;
            }
        }
        if (interestedTheatre == null) {
            System.out.println("Theatre " + theatreId + " does not run " + movieName);
            return;
        }
        if (interestedShow == null) {
            System.out.println("Show " + showId + " not found in theatre " + theatreId);
            return;
        }

        // 5. check that the requested seats are still available
        // NOTE: concurrency (Optimistic Concurrency Control) is intentionally
        //       NOT implemented here yet -- see the notes at the top of the file
        //       for how it would work when we add it.
        List<Integer> bookedSeats = interestedShow.getBookedSeatIds();
        for (int seatNumber : seatNumbers) {
            if (bookedSeats.contains(seatNumber)) {
                System.out.println("seat " + seatNumber + " already booked, try again");
                return;
            }
        }

        // 6. calculate final cost based on each seat's category (SILVER/GOLD/PLATINUM)
        int totalCost = 0;
        for (int seatNumber : seatNumbers) {
            for (Seat screenSeat : interestedShow.getScreen().getSeats()) {
                if (screenSeat.getSeatId() == seatNumber) {
                    totalCost += screenSeat.getSeatCategory().getPrice();
                    break;
                }
            }
        }

        // 7. attempt payment (in real world this calls a payment gateway with
        //    totalCost, and only returns true after money is actually received).
        //    Here it's a dummy that just flips the status field.
        Payment payment = new Payment();
        payment.setPaymentId((int) (Math.random() * 100000));
        payment.setAmount(totalCost);
        boolean paid = payment.process();

        // 8. if payment FAILED -> do NOT book any seats, just return
        if (!paid) {
            System.out.println("PAYMENT FAILED -> booking cancelled, seats NOT booked"
                    + " (amount=Rs." + totalCost + ", paymentId=" + payment.getPaymentId() + ")");
            return;
        }

        // 9. payment SUCCEEDED -> now (and ONLY now) confirm the booking:
        //    a) mark seats as booked on the show
        //    b) create the booking record linking show + seats + payment
        for (int seatNumber : seatNumbers) {
            bookedSeats.add(seatNumber);
        }
        interestedShow.setBookedSeatIds(bookedSeats);

        Booking booking = new Booking();
        booking.setShow(interestedShow);
        booking.setBookedSeatIds(seatNumbers);
        booking.setPayment(payment);
        allBookings.add(booking); // persist the booking so we don't lose it

        System.out.println("BOOKING SUCCESSFUL -> movie=" + movieName
                + ", theatreId=" + theatreId
                + ", showId=" + showId
                + ", seats=" + seatNumbers
                + ", amount=Rs." + totalCost
                + ", paymentId=" + payment.getPaymentId());
    }

    void initialize() {
        createMovies();
        createTheatre();
    }

    private void createTheatre() {

        Movie avengerMovie = movieController.getMovieByName("AVENGERS");
        Movie baahubali = movieController.getMovieByName("BAAHUBALI");

        Theatre inoxTheatre = new Theatre();
        inoxTheatre.setTheatreId(1);
        inoxTheatre.setScreen(createScreen());
        inoxTheatre.setCity(City.Bangalore);
        List<Show> inoxShows = new ArrayList<>();
        inoxShows.add(createShows(1, inoxTheatre.getScreen().get(0), avengerMovie, 8));
        inoxShows.add(createShows(2, inoxTheatre.getScreen().get(0), baahubali, 16));
        inoxTheatre.setShows(inoxShows);

        Theatre pvrTheatre = new Theatre();
        pvrTheatre.setTheatreId(2);
        pvrTheatre.setScreen(createScreen());
        pvrTheatre.setCity(City.Delhi);
        List<Show> pvrShows = new ArrayList<>();
        pvrShows.add(createShows(3, pvrTheatre.getScreen().get(0), avengerMovie, 13));
        pvrShows.add(createShows(4, pvrTheatre.getScreen().get(0), baahubali, 20));
        pvrTheatre.setShows(pvrShows);

        theatreController.addTheatre(inoxTheatre, City.Bangalore);
        theatreController.addTheatre(pvrTheatre, City.Delhi);
    }

    private List<Screen> createScreen() {
        List<Screen> screens = new ArrayList<>();
        Screen screen1 = new Screen();
        screen1.setScreenId(1);
        screen1.setSeats(createSeats());
        screens.add(screen1);
        return screens;
    }

    private Show createShows(int showId, Screen screen, Movie movie, int showStartTime) {
        Show show = new Show();
        show.setShowId(showId);
        show.setScreen(screen);
        show.setMovie(movie);
        show.setShowStartTime(showStartTime); // 24-hr time, e.g. 14 = 2PM
        return show;
    }

    // 100 seats: 0-39 SILVER, 40-69 GOLD, 70-99 PLATINUM
    private List<Seat> createSeats() {
        List<Seat> seats = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            Seat seat = new Seat();
            seat.setSeatId(i);
            seat.setSeatCategory(SeatCategory.SILVER);
            seats.add(seat);
        }
        for (int i = 40; i < 70; i++) {
            Seat seat = new Seat();
            seat.setSeatId(i);
            seat.setSeatCategory(SeatCategory.GOLD);
            seats.add(seat);
        }
        for (int i = 70; i < 100; i++) {
            Seat seat = new Seat();
            seat.setSeatId(i);
            seat.setSeatCategory(SeatCategory.PLATINUM);
            seats.add(seat);
        }
        return seats;
    }

    private void createMovies() {
        Movie avengers = new Movie();
        avengers.setMovieId(1);
        avengers.setMovieName("AVENGERS");
        avengers.setMovieDuration(128);

        Movie baahubali = new Movie();
        baahubali.setMovieId(2);
        baahubali.setMovieName("BAAHUBALI");
        baahubali.setMovieDuration(180);

        movieController.addMovie(avengers, City.Bangalore);
        movieController.addMovie(avengers, City.Delhi);
        movieController.addMovie(baahubali, City.Bangalore);
        movieController.addMovie(baahubali, City.Delhi);
    }
}

// ============================== ENUMS ==============================

enum City {
    Bangalore,
    Delhi
}

enum SeatCategory {
    // each category has a per-seat price (in rupees)
    SILVER(100),
    GOLD(150),
    PLATINUM(250);

    private final int price;

    SeatCategory(int price) {
        this.price = price;
    }

    public int getPrice() {
        return price;
    }
}

// ============================== MODELS ==============================

class Movie {
    int movieId;
    String movieName;
    int movieDurationInMinutes;

    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }

    public String getMovieName() { return movieName; }
    public void setMovieName(String movieName) { this.movieName = movieName; }

    public int getMovieDuration() { return movieDurationInMinutes; }
    public void setMovieDuration(int movieDuration) { this.movieDurationInMinutes = movieDuration; }
}

class Seat {
    int seatId;
    int row;
    SeatCategory seatCategory;

    public int getSeatId() { return seatId; }
    public void setSeatId(int seatId) { this.seatId = seatId; }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public SeatCategory getSeatCategory() { return seatCategory; }
    public void setSeatCategory(SeatCategory seatCategory) { this.seatCategory = seatCategory; }
}

class Screen {
    int screenId;
    List<Seat> seats = new ArrayList<>();

    public int getScreenId() { return screenId; }
    public void setScreenId(int screenId) { this.screenId = screenId; }

    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> seats) { this.seats = seats; }
}

class Show {
    int showId;
    Movie movie;
    Screen screen;
    int showStartTime;
    List<Integer> bookedSeatIds = new ArrayList<>();

    public int getShowId() { return showId; }
    public void setShowId(int showId) { this.showId = showId; }

    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }

    public Screen getScreen() { return screen; }
    public void setScreen(Screen screen) { this.screen = screen; }

    public int getShowStartTime() { return showStartTime; }
    public void setShowStartTime(int showStartTime) { this.showStartTime = showStartTime; }

    public List<Integer> getBookedSeatIds() { return bookedSeatIds; }
    public void setBookedSeatIds(List<Integer> bookedSeatIds) { this.bookedSeatIds = bookedSeatIds; }
}

class Theatre {
    int theatreId;
    String address;
    City city;
    List<Screen> screen = new ArrayList<>();
    List<Show> shows = new ArrayList<>();

    public int getTheatreId() { return theatreId; }
    public void setTheatreId(int theatreId) { this.theatreId = theatreId; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public List<Screen> getScreen() { return screen; }
    public void setScreen(List<Screen> screen) { this.screen = screen; }

    public List<Show> getShows() { return shows; }
    public void setShows(List<Show> shows) { this.shows = shows; }

    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }
}

class Payment {
    int paymentId;
    int amount;     // final amount charged (sum of seat prices)
    String status;  // "SUCCESS" / "FAILED" -- set by process(), not by caller

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public String getStatus() { return status; }

    /*
     * Simulates a payment gateway call.
     *   - In real life: hit Razorpay / Stripe / UPI, wait for confirmation,
     *     handle timeouts, retries, refunds, etc.
     *   - For this demo we just approve it and set status = SUCCESS.
     *
     * Returns true if payment went through, false otherwise.
     * IMPORTANT: caller must only confirm the booking when this returns true.
     */
    public boolean process() {
        // TODO: replace with real gateway integration
        this.status = "SUCCESS";
        return true;
    }
}

class Booking {
    Show show;
    List<Integer> bookedSeatIds = new ArrayList<>();
    Payment payment;

    public Show getShow() { return show; }
    public void setShow(Show show) { this.show = show; }

    public List<Integer> getBookedSeatIds() { return bookedSeatIds; }
    public void setBookedSeatIds(List<Integer> bookedSeatIds) { this.bookedSeatIds = bookedSeatIds; }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
}

// ============================== CONTROLLERS ==============================

class MovieController {
    Map<City, List<Movie>> cityVsMovies;
    List<Movie> allMovies;

    MovieController() {
        cityVsMovies = new HashMap<>();
        allMovies = new ArrayList<>();
    }

    void addMovie(Movie movie, City city) {
        if (!allMovies.contains(movie)) {
            allMovies.add(movie);
        }

        // If this city isn't in the map yet, create a fresh list for it.
        List<Movie> movies = cityVsMovies.get(city);
        if (movies == null) {
            movies = new ArrayList<>();
        }
        movies.add(movie);
        cityVsMovies.put(city, movies); // put the updated list back into the map
    }

    Movie getMovieByName(String movieName) {
        for (Movie movie : allMovies) {
            if (movie.getMovieName().equals(movieName)) {
                return movie;
            }
        }
        return null;
    }

    List<Movie> getMoviesByCity(City city) {
        return cityVsMovies.get(city);
    }
}

class TheatreController {
    Map<City, List<Theatre>> cityVsTheatre;
    List<Theatre> allTheatre;

    TheatreController() {
        cityVsTheatre = new HashMap<>();
        allTheatre = new ArrayList<>();
    }

    void addTheatre(Theatre theatre, City city) {
        if (!allTheatre.contains(theatre)) {
            allTheatre.add(theatre);
        }

        // If this city isn't in the map yet, create a fresh list for it.
        List<Theatre> theatres = cityVsTheatre.get(city);
        if (theatres == null) {
            theatres = new ArrayList<>();
        }
        theatres.add(theatre);
        cityVsTheatre.put(city, theatres); // put the updated list back into the map
    }

    Map<Theatre, List<Show>> getAllShow(Movie movie, City city) {
        Map<Theatre, List<Show>> theatreVsShows = new HashMap<>();
        List<Theatre> theatres = cityVsTheatre.get(city);
        if (theatres == null) return theatreVsShows;

        for (Theatre theatre : theatres) {
            List<Show> givenMovieShows = new ArrayList<>();
            for (Show show : theatre.getShows()) {
                if (show.movie.getMovieId() == movie.getMovieId()) {
                    givenMovieShows.add(show);
                }
            }
            if (!givenMovieShows.isEmpty()) {
                theatreVsShows.put(theatre, givenMovieShows);
            }
        }
        return theatreVsShows;
    }
}

// ============================== DRIVER ==============================

public class Main {
    public static void main(String[] args) {

        BookMyShow bookMyShow = new BookMyShow();
        bookMyShow.initialize();

        // In Bangalore, Inox (theatreId=1) plays BAAHUBALI as showId=2 (4 PM).
        // user1 books seat 30 -> success.
        // user2 tries to book the same seat 30 -> "already booked".
        // (With OCC properly implemented, whoever commits first wins.)
        bookMyShow.createBooking(
                City.Bangalore, "BAAHUBALI",
                /* theatreId */ 1, /* showId */ 2,
                List.of(30));

        bookMyShow.createBooking(
                City.Bangalore, "BAAHUBALI",
                /* theatreId */ 1, /* showId */ 2,
                List.of(30));
    }
}
