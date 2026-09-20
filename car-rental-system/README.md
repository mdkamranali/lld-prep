# Car Rental System — Low Level Design

## UML Class Diagram

```
┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐
│      <<enum>>       │   │      <<enum>>       │   │      <<enum>>       │
│     VehicleType     │   │       Status        │   │   ReservationType   │
│─────────────────────│   │─────────────────────│   │─────────────────────│
│  CAR                │   │  ACTIVE             │   │  HOURLY             │
│  BIKE               │   │  INACTIVE           │   │  DAILY              │
└─────────────────────┘   └─────────────────────┘   └─────────────────────┘

┌─────────────────────┐   ┌─────────────────────┐
│      <<enum>>       │   │      <<enum>>       │
│  ReservationStatus  │   │     PaymentMode     │
│─────────────────────│   │─────────────────────│
│  SCHEDULED          │   │  CASH               │
│  INPROGRESS         │   │  ONLINE             │
│  COMPLETED          │   └─────────────────────┘
│  CANCELLED          │
└─────────────────────┘

┌──────────────────────────────────────┐
│           VehicleRentalSystem        │  ← entry point
│──────────────────────────────────────│
│ - storeList: List<Store>             │
│ - userList: List<User>               │
│──────────────────────────────────────│
│ + getStores(loc): List<Store>        │
│ + addUser(u): void                   │
│ + removeUser(userId): boolean        │
│ + addStore(s): void                  │
│ + removeStore(storeId): boolean      │
└──────────────────────────────────────┘
           │ has many
           ▼
┌─────────────────────────────────────────────────────┐
│                       Store                         │
│─────────────────────────────────────────────────────│
│ - storeId: int                                      │
│ - storeLocation: Location                           │
│ - inventoryManagement: VehicleInventoryManagement   │
│ - reservations: List<Reservation>                   │
│─────────────────────────────────────────────────────│
│ + getVehicles(type): List<Vehicle>                  │
│ + setVehicles(list): void                           │
│ + addVehicle(v): void                               │
│ + updateVehicleStatus(vehicleID, status): boolean   │
│ + createReservation(v, u, type, from, to, drop)     │
│ + completeReservation(reservationID): boolean       │
│ + cancelReservation(reservationID): boolean         │
│ - getReservation(reservationID): Reservation        │
└─────────────────────────────────────────────────────┘
           │ delegates inventory work
           ▼
┌──────────────────────────────────────────┐
│        VehicleInventoryManagement        │
│──────────────────────────────────────────│
│ - vehicles: List<Vehicle>                │
│──────────────────────────────────────────│
│ + getVehicles(type): List<Vehicle>       │  ← filters by type + ACTIVE
│ + setVehicles(list): void                │
│ + addVehicle(v): void                    │
│ + getVehicle(vehicleID): Vehicle         │
└──────────────────────────────────────────┘
           │ holds
           ▼
┌──────────────────────────────┐        ┌──────────────────────────────┐
│           Vehicle            │        │            User              │
│──────────────────────────────│        │──────────────────────────────│
│ - vehicleID: int             │        │ - userId: int                │
│ - vehicleType: VehicleType   │        │ - userName: String           │
│ - dailyRentalCost: int       │        │ - drivingLicense: String     │
│ - hourlyRentalCost: int      │        │──────────────────────────────│
│ - noOfSeat: int              │        │ + getters / setters          │
│ - status: Status             │        └──────────────────────────────┘
│──────────────────────────────│
│ + getters / setters          │
└──────────────────────────────┘

┌──────────────────────────────────────────┐
│               Reservation                │
│──────────────────────────────────────────│
│ - reservationId: int    (random 5 digit) │
│ - user: User                             │
│ - vehicle: Vehicle                       │
│ - bookingDate: Date                      │
│ - dateBookedFrom / dateBookedTo: Date    │
│ - fromTimeStamp / toTimeStamp: Long      │
│ - pickUpLocation / dropLocation: Location│
│ - reservationType: ReservationType       │
│ - reservationStatus: ReservationStatus   │
│──────────────────────────────────────────│
│ + completeReserve(): void                │
│ + cancelReserve(): void                  │
└──────────────────────────────────────────┘
           │ billed by
           ▼
┌──────────────────────────────┐        ┌────────────────────────────────────┐
│            Bill              │        │              Payment               │
│──────────────────────────────│        │────────────────────────────────────│
│ - reservation: Reservation   │        │ + payBill(bill, mode): void        │
│ - totalBillAmount: double    │        └────────────────────────────────────┘
│ - isBillPaid: boolean        │
│──────────────────────────────│
│ - computeBillAmount(): double│
└──────────────────────────────┘

┌──────────────────────────────┐
│           Location           │
│──────────────────────────────│
│ - address: String            │
│ - pincode: int               │
│ - city / state / country     │
│──────────────────────────────│
│ Location(pin, city, st, cty) │  ← area search
│ Location(addr, pin, ...)     │  ← physical place
└──────────────────────────────┘
```

---

## Flow — Start to End

### 1. Setup
`main` builds the seed data and hands it to the system:

```
addVehicles()  → 2 cars (2400/day, 1900/day) + 1 bike (700/day), all ACTIVE
addUsers()     → 1 user with id, name and driving license
addStores()    → 1 store in Bangalore, owning the 3 vehicles
                 new VehicleRentalSystem(stores, users)
```

### 2. User Searches Stores — `rentalSystem.getStores(location)`
A city can have many stores, so the search returns a **list** and the user picks one.

```java
for (Store store : storeList) {
    if (store.storeLocation != null && store.storeLocation.city.equalsIgnoreCase(location.city)) {
        nearByStores.add(store);
    }
}
```

Matching is on **city** (not pincode) so a city-wide search returns every branch.
If the list comes back empty the flow stops early.

### 3. Search Vehicles — `store.getVehicles(VehicleType.CAR)`
`Store` delegates to `VehicleInventoryManagement`, which filters on **two** things:

```java
if (vehicle.getVehicleType() == vehicleType && vehicle.getStatus() == Status.ACTIVE) {
    filteredVehicles.add(vehicle);
}
```

There are no `Car` / `Bike` subclasses — `VehicleType` alone separates one vehicle
from another. The `ACTIVE` check means an already-booked vehicle disappears from
search results automatically.

3 vehicles in inventory → `getVehicles(CAR)` returns **2**.

### 4. Reserve — `store.createReservation(...)`
The `Reservation` is built entirely through its **constructor**, so a half-built
booking with null dates can never exist:

```java
this.reservationId   = 10000 + RANDOM.nextInt(90000);   // 5 digit id
this.bookingDate     = new Date();                      // when it was booked
this.fromTimeStamp   = dateBookedFrom.getTime();        // derived, never drifts
this.toTimeStamp     = dateBookedTo.getTime();
this.reservationStatus = ReservationStatus.SCHEDULED;
vehicle.setStatus(Status.INACTIVE);                     // car is now taken
```

`pickUpLocation` is filled in by the `Store` with its own location — that is by
definition where the car is collected. Only `dropLocation` is the caller's choice.

### 5. Generate Bill — `new Bill(reservation)`
Cost comes from the booked duration and the vehicle's own rate:

```
milliSeconds = toTimeStamp - fromTimeStamp     → e.g. 259200000 ms (1 Oct → 4 Oct)

HOURLY → TimeUnit.MILLISECONDS.toHours(ms) * hourlyRentalCost
DAILY  → TimeUnit.MILLISECONDS.toDays(ms)  * dailyRentalCost   → 3 * 2400 = 7200
```

### 6. Pay — `payment.payBill(bill, PaymentMode.ONLINE)`
Flips `isBillPaid` to `true` and prints the receipt line. A real gateway call
would slot in here.

### 7. Complete Trip — `store.completeReservation(reservationId)`
Looks the reservation up in the store's list, then:

```
reservationStatus → COMPLETED
vehicle status    → ACTIVE      (back in the search results)
```

Returns `false` for an unknown id. `cancelReservation` works the same way but
sets `CANCELLED`. The reservation stays in the list either way, so there is
still a record to audit.

### Output

```
Cars available at store = 2
Reservation 95999 amount = 7200.0
Bill of 7200.0 paid by ONLINE
```

---

## Design Patterns Used

| Pattern      | Where                             | Why                                                        |
|--------------|-----------------------------------|------------------------------------------------------------|
| Facade       | `VehicleRentalSystem`             | One entry point over stores + users, hides the lookup work  |
| Controller   | `Store`                           | Coordinates inventory and reservations, owns the use cases  |
| Composition  | `Store` → `VehicleInventoryManagement` | Inventory logic lives in its own class, store delegates |
| Enum State   | `Status`, `ReservationStatus`     | Vehicle and booking lifecycle instead of boolean flags      |

---

## Key Classes at a Glance

| Class                        | Responsibility                                            |
|------------------------------|-----------------------------------------------------------|
| `VehicleRentalSystem`        | Entry point — find stores by location, manage users/stores |
| `Store`                      | Reserve, complete, cancel; owns inventory and reservations |
| `VehicleInventoryManagement` | Holds vehicles, filters them by type and availability      |
| `Vehicle`                    | Id, type, rates, seats, availability status                |
| `User`                       | Id, name, driving license                                  |
| `Location`                   | Address, pincode, city, state, country                     |
| `Reservation`                | Who booked what, from when to when, and its status         |
| `Bill`                       | Turns a reservation's duration into an amount              |
| `Payment`                    | Settles the bill and marks it paid                         |

---

## Run

```bash
java src/Main.java
```
