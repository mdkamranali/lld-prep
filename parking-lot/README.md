# Parking Lot — Low Level Design

## UML Class Diagram

```
┌─────────────────────┐
│      <<enum>>       │
│     VehicleType     │
│─────────────────────│
│  TWO_WHEELER        │
│  FOUR_WHEELER       │
└─────────────────────┘

┌─────────────────────┐
│      <<enum>>       │
│      SpotType       │
│─────────────────────│
│  TWO_WHEELER        │
│  FOUR_WHEELER       │
└─────────────────────┘

┌─────────────────────┐
│      <<enum>>       │
│     PaymentType     │
│─────────────────────│
│  CASH               │
│  CARD               │
│  UPI                │
└─────────────────────┘

┌──────────────────────────┐
│         Vehicle          │
│──────────────────────────│
│ - plate: String          │
│ - type: VehicleType      │
│──────────────────────────│
│ + getPlate(): String     │
│ + getType(): VehicleType │
└──────────────────────────┘
           │ parked in
           ▼
┌────────────────────────────────┐
│          ParkingSpot           │
│────────────────────────────────│
│ - id: String                   │
│ - type: SpotType               │
│ - distanceToElevator: int      │
│ - distanceToEntrance: int      │
│ - distanceToExit: int          │
│ - free: boolean                │
│ - vehicle: Vehicle             │
│────────────────────────────────│
│ + isAvailable(): boolean       │
│ + reserve(v: Vehicle): void    │
│ + release(): void              │
│ + getVehicle(): Vehicle        │
│ + getDistanceToElevator(): int │
│ + getDistanceToEntrance(): int │
│ + getDistanceToExit(): int     │
└────────────────────────────────┘
           │ picked by
           ▼
┌──────────────────────────────────────┐
│       <<interface>>                  │
│        ParkingStrategy               │
│──────────────────────────────────────│
│ + findSpot(spots: List): ParkingSpot │
└──────────────────────────────────────┘
        ▲         ▲         ▲
        │         │         │
┌──────────┐ ┌──────────┐ ┌──────────┐
│ Nearest  │ │ Nearest  │ │ Nearest  │
│Elevator  │ │Entrance  │ │  Exit    │
└──────────┘ └──────────┘ └──────────┘

┌──────────────────────────────┐
│            Ticket            │
│──────────────────────────────│
│ - id: String                 │
│ - vehicle: Vehicle           │
│ - spot: ParkingSpot          │
│ - entryTime: long            │
│ - exitTime: long             │
│ - fee: double                │
│──────────────────────────────│
│ + getId(): String            │
│ + getEntryTime(): long       │
│ + setExitTime(t: long): void │
│ + setFee(f: double): void    │
└──────────────────────────────┘

┌──────────────────────────────┐
│         HourlyPricing        │
│──────────────────────────────│
│ - ratePerHour: double        │
│──────────────────────────────│
│ + calculate(ms: long): double│
└──────────────────────────────┘

┌──────────────────────────────┐
│           Payment            │
│──────────────────────────────│
│ - type: PaymentType          │
│ - amount: double             │
│──────────────────────────────│
│ + process(): boolean         │
└──────────────────────────────┘

┌─────────────────────────────────────────────┐
│             ParkingLotManager               │
│─────────────────────────────────────────────│
│ - vehicleType: VehicleType                  │
│ - spots: List<ParkingSpot>                  │
│ - activeTickets: Map<String, Ticket>        │
│ - pricing: HourlyPricing                    │
│ - strategy: ParkingStrategy                 │
│─────────────────────────────────────────────│
│ + addSpot(spot: ParkingSpot): void          │
│ + setStrategy(s: ParkingStrategy): void     │
│ + park(vehicle: Vehicle): Ticket            │
│ + checkout(ticketId, paymentType): void     │
└─────────────────────────────────────────────┘
           ▲
           │ creates
┌─────────────────────────────────────────────┐
│          ParkingLotManagerFactory           │
│─────────────────────────────────────────────│
│ + createTwoWheelerManager(): Manager        │
│ + createFourWheelerManager(): Manager       │
└─────────────────────────────────────────────┘
```

---

## Flow — Start to End

### 1. Setup
Factory creates two separate managers — one for two-wheelers ($2/hr), one for four-wheelers ($5/hr).
Each manager gets its own list of `ParkingSpot` objects with distance values pre-set.

```
ParkingLotManagerFactory
  ├── createTwoWheelerManager(NearestToEntrance)   → $2/hr
  └── createFourWheelerManager(NearestToElevator)  → $5/hr
```

### 2. Vehicle Parks — `manager.park(vehicle)`
- Manager asks the **strategy** to scan all spots and return the best one.
- Strategy loops through spots:
  ```java
  for (ParkingSpot spot : spots) {
      if (!spot.isAvailable()) continue;
      if (best == null || spot.getDistance_() < best.getDistance_()) {
          best = spot;
      }
  }
  ```
- Chosen spot → `spot.reserve(vehicle)` sets `free = false` and stores the vehicle.
- A `Ticket` is created with `entryTime = System.currentTimeMillis()`.
- Ticket stored in `activeTickets` map and returned to caller.

### 3. Checkout — `manager.checkout(ticketId, paymentType)`
- Ticket looked up from `activeTickets` map by ID.
- `exitTime` set (real system uses current time; here we simulate 2 hours).
- Fee calculated:
  ```
  duration  = exitTime - entryTime         → e.g. 7,200,000 ms
  hours     = ceil(7200000 / 3600000.0)   → ceil(2.0) = 2
  fee       = max(1, 2) * ratePerHour     → 2 * $5 = $10
  ```
- `Payment.process()` prints the payment line.
- `spot.release()` sets `free = true`, clears vehicle reference.
- Ticket removed from `activeTickets`.

### 4. Strategy Swap (runtime)
Strategy can be changed anytime without touching any other class:
```java
manager.setStrategy(new NearestToExit());
```

---

## Design Patterns Used

| Pattern   | Where                                      | Why                                              |
|-----------|--------------------------------------------|--------------------------------------------------|
| Strategy  | `ParkingStrategy` + 3 implementations     | Swap spot-selection logic without changing manager |
| Factory   | `ParkingLotManagerFactory`                 | Centralise manager creation with correct defaults |
| Singleton | Can be added to `ParkingLotManager` if needed | One manager instance per vehicle type           |

---

## Key Classes at a Glance

| Class                    | Responsibility                                      |
|--------------------------|-----------------------------------------------------|
| `Vehicle`                | Holds plate + vehicle type                          |
| `ParkingSpot`            | Holds spot id, type, distances, parked vehicle      |
| `ParkingStrategy`        | Interface — picks best spot from a list             |
| `NearestToElevator/Entrance/Exit` | Concrete strategies using for-loop       |
| `Ticket`                 | Records entry/exit time, fee, vehicle, spot         |
| `HourlyPricing`          | Converts ms duration → fee                          |
| `Payment`                | Processes payment by type                           |
| `ParkingLotManager`      | Core — parks vehicles, manages tickets, checkout    |
| `ParkingLotManagerFactory` | Creates pre-configured managers                   |
