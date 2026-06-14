import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// ── Enums ──────────────────────────────────────────────────────────────────

enum VehicleType { TWO_WHEELER, FOUR_WHEELER }
enum SpotType    { TWO_WHEELER, FOUR_WHEELER }
enum PaymentType { CASH, CARD, UPI }

// ── Vehicle ────────────────────────────────────────────────────────────────

class Vehicle {
    private final String      plate;
    private final VehicleType type;

    public Vehicle(String plate, VehicleType type) {
        this.plate = plate;
        this.type  = type;
    }

    public String      getPlate() { return plate; }
    public VehicleType getType()  { return type;  }
}

// ── Parking Spot ───────────────────────────────────────────────────────────

class ParkingSpot {
    private final String   id;
    private final SpotType type;
    private final int      distanceToElevator;
    private final int      distanceToEntrance;
    private final int      distanceToExit;
    private boolean        free    = true;
    private Vehicle        vehicle = null;

    public ParkingSpot(String id, SpotType type,
                       int distanceToElevator,
                       int distanceToEntrance,
                       int distanceToExit) {
        this.id                 = id;
        this.type               = type;
        this.distanceToElevator = distanceToElevator;
        this.distanceToEntrance = distanceToEntrance;
        this.distanceToExit     = distanceToExit;
    }

    public String   getId()                 { return id;                 }
    public SpotType getType()               { return type;               }
    public boolean  isAvailable()           { return free;               }
    public Vehicle  getVehicle()            { return vehicle;            }
    public int      getDistanceToElevator() { return distanceToElevator; }
    public int      getDistanceToEntrance() { return distanceToEntrance; }
    public int      getDistanceToExit()     { return distanceToExit;     }

    public void reserve(Vehicle v) { this.vehicle = v; this.free = false; }
    public void release()          { this.vehicle = null; this.free = true; }
}

// ── Parking Strategy ───────────────────────────────────────────────────────

interface ParkingStrategy {
    ParkingSpot findSpot(List<ParkingSpot> spots);
}

class NearestToElevator implements ParkingStrategy {
    @Override
    public ParkingSpot findSpot(List<ParkingSpot> spots) {
        ParkingSpot best = null;
        for (ParkingSpot spot : spots) {
            if (!spot.isAvailable()) continue;
            if (best == null || spot.getDistanceToElevator() < best.getDistanceToElevator()) {
                best = spot;
            }
        }
        return best;
    }
}

class NearestToEntrance implements ParkingStrategy {
    @Override
    public ParkingSpot findSpot(List<ParkingSpot> spots) {
        ParkingSpot best = null;
        for (ParkingSpot spot : spots) {
            if (!spot.isAvailable()) continue;
            if (best == null || spot.getDistanceToEntrance() < best.getDistanceToEntrance()) {
                best = spot;
            }
        }
        return best;
    }
}

class NearestToExit implements ParkingStrategy {
    @Override
    public ParkingSpot findSpot(List<ParkingSpot> spots) {
        ParkingSpot best = null;
        for (ParkingSpot spot : spots) {
            if (!spot.isAvailable()) continue;
            if (best == null || spot.getDistanceToExit() < best.getDistanceToExit()) {
                best = spot;
            }
        }
        return best;
    }
}

// ── Ticket ─────────────────────────────────────────────────────────────────

class Ticket {
    private static final AtomicInteger counter = new AtomicInteger(1);

    private final String      id;
    private final Vehicle     vehicle;
    private final ParkingSpot spot;
    private final long        entryTime;
    private long              exitTime;
    private double            fee;

    public Ticket(Vehicle vehicle, ParkingSpot spot) {
        this.id        = "TKT-" + counter.getAndIncrement();
        this.vehicle   = vehicle;
        this.spot      = spot;
        this.entryTime = System.currentTimeMillis();
    }

    public String      getId()        { return id;        }
    public Vehicle     getVehicle()   { return vehicle;   }
    public ParkingSpot getSpot()      { return spot;      }
    public long        getEntryTime() { return entryTime; }

    public void   setExitTime(long t) { this.exitTime = t; }
    public long   getExitTime()       { return exitTime;   }
    public void   setFee(double f)    { this.fee = f;      }
    public double getFee()            { return fee;        }
}

// ── Payment ────────────────────────────────────────────────────────────────

class Payment {
    private final PaymentType type;
    private final double      amount;

    public Payment(PaymentType type, double amount) {
        this.type   = type;
        this.amount = amount;
    }

    public boolean process() {
        System.out.printf("[%s] Paid $%.2f%n", type, amount);
        return true;
    }
}

// ── Pricing ────────────────────────────────────────────────────────────────

class HourlyPricing {
    private final double ratePerHour;

    public HourlyPricing(double rate) { this.ratePerHour = rate; }

    // durationMs → convert to hours (ceil) → multiply by rate
    // e.g. 7_200_000 ms → 2.0 hrs → 2 * $5 = $10
    public double calculate(long durationMs) {
        double hours = Math.ceil(durationMs / 3_600_000.0); // 1 hr = 3,600,000 ms
        return Math.max(1, hours) * ratePerHour;            // minimum 1 hour charge
    }
}

// ── Parking Lot Manager ────────────────────────────────────────────────────
// One manager per vehicle type — owns its own spot pool, pricing, and strategy

class ParkingLotManager {
    private final VehicleType         vehicleType;
    private final List<ParkingSpot>   spots         = new ArrayList<>();
    private final Map<String, Ticket> activeTickets = new HashMap<>();
    private final HourlyPricing       pricing;
    private       ParkingStrategy     strategy;

    public ParkingLotManager(VehicleType vehicleType,
                             double ratePerHour,
                             ParkingStrategy strategy) {
        this.vehicleType = vehicleType;
        this.pricing     = new HourlyPricing(ratePerHour);
        this.strategy    = strategy;
    }

    public void setStrategy(ParkingStrategy strategy) { this.strategy = strategy; }
    public void addSpot(ParkingSpot spot)             { spots.add(spot);          }

    public Ticket park(Vehicle vehicle) {
        if (vehicle.getType() != vehicleType) {
            System.out.println("[Error] Wrong manager for " + vehicle.getType());
            return null;
        }

        // strategy scans all spots and picks the best available one
        ParkingSpot chosen = strategy.findSpot(spots);

        if (chosen == null) {
            System.out.println("[Full] No spot available for " + vehicle.getPlate());
            return null;
        }

        chosen.reserve(vehicle);
        Ticket ticket = new Ticket(vehicle, chosen);
        activeTickets.put(ticket.getId(), ticket);

        System.out.printf("[Parked] %s (%s) → spot %s | Ticket: %s%n",
            vehicle.getPlate(), vehicleType, chosen.getId(), ticket.getId());
        return ticket;
    }

    public void checkout(String ticketId, PaymentType paymentType) {
        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null) {
            System.out.println("[Error] Ticket not found: " + ticketId);
            return;
        }

        // simulate 2 hours parking (in real system use System.currentTimeMillis())
        long exitTime = ticket.getEntryTime() + 7_200_000;
        ticket.setExitTime(exitTime);

        double fee = pricing.calculate(exitTime - ticket.getEntryTime());
        ticket.setFee(fee);

        new Payment(paymentType, fee).process();

        ticket.getSpot().release();
        activeTickets.remove(ticketId);

        System.out.printf("[Out] Spot %s is now free | Fee: $%.2f%n",
            ticket.getSpot().getId(), fee);
    }
}

// ── Factory ────────────────────────────────────────────────────────────────

class ParkingLotManagerFactory {
    public static ParkingLotManager createTwoWheelerManager(ParkingStrategy strategy) {
        return new ParkingLotManager(VehicleType.TWO_WHEELER, 2.0, strategy);
    }

    public static ParkingLotManager createFourWheelerManager(ParkingStrategy strategy) {
        return new ParkingLotManager(VehicleType.FOUR_WHEELER, 5.0, strategy);
    }
}

// ── Main ───────────────────────────────────────────────────────────────────

public class Main {
    public static void main(String[] args) {

        // create managers via factory, each with a default strategy
        ParkingLotManager twoWheelerMgr  = ParkingLotManagerFactory
                                            .createTwoWheelerManager(new NearestToEntrance());
        ParkingLotManager fourWheelerMgr = ParkingLotManagerFactory
                                            .createFourWheelerManager(new NearestToElevator());

        // add spots: (id, type, distToElevator, distToEntrance, distToExit)
        twoWheelerMgr.addSpot(new ParkingSpot("2W-1", SpotType.TWO_WHEELER, 10, 3, 8));
        twoWheelerMgr.addSpot(new ParkingSpot("2W-2", SpotType.TWO_WHEELER,  5, 7, 2));
        twoWheelerMgr.addSpot(new ParkingSpot("2W-3", SpotType.TWO_WHEELER,  8, 1, 5));

        fourWheelerMgr.addSpot(new ParkingSpot("4W-1", SpotType.FOUR_WHEELER, 2, 10, 6));
        fourWheelerMgr.addSpot(new ParkingSpot("4W-2", SpotType.FOUR_WHEELER, 7,  4, 3));
        fourWheelerMgr.addSpot(new ParkingSpot("4W-3", SpotType.FOUR_WHEELER, 9,  6, 1));

        // park vehicles
        Vehicle bike = new Vehicle("KA-01-XYZ-567", VehicleType.TWO_WHEELER);
        Vehicle car  = new Vehicle("KA-01-AB-1234", VehicleType.FOUR_WHEELER);
        Vehicle car2 = new Vehicle("MH-02-CD-5678", VehicleType.FOUR_WHEELER);

        Ticket t1 = twoWheelerMgr.park(bike);    // NearestToEntrance → picks 2W-3 (dist=1)
        Ticket t2 = fourWheelerMgr.park(car);    // NearestToElevator → picks 4W-1 (dist=2)

        // swap strategy at runtime
        fourWheelerMgr.setStrategy(new NearestToExit());
        Ticket t3 = fourWheelerMgr.park(car2);   // NearestToExit     → picks 4W-3 (dist=1)

        // checkout
        twoWheelerMgr.checkout(t1.getId(),  PaymentType.CARD);
        fourWheelerMgr.checkout(t2.getId(), PaymentType.UPI);
        fourWheelerMgr.checkout(t3.getId(), PaymentType.CASH);
    }
}
