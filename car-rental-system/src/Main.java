import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String args[]) {


        List<User> users = addUsers();
        List<Vehicle> vehicles = addVehicles();
        List<Store> stores = addStores(vehicles);

        VehicleRentalSystem rentalSystem = new VehicleRentalSystem(stores, users);

        //0. User comes
        User user = users.get(0);

        //1. user search stores based on location
        Location location = new Location(403012, "Bangalore", "Karnataka", "India");
        List<Store> nearByStores = rentalSystem.getStores(location);
        if (nearByStores.isEmpty()) {
            System.out.println("No store found at " + location.city);
            return;
        }

        //for simplicity user picks any one store from the list
        Store store = nearByStores.get(0);

        //2. get All vehicles you are interested in (based upon different filters)
        List<Vehicle> storeVehicles = store.getVehicles(VehicleType.CAR);
        System.out.println("Cars available at store = " + storeVehicles.size());


        //3.reserving the particular vehicle
        Date bookedFrom = new Date();
        Date bookedTo = new Date(bookedFrom.getTime() + TimeUnit.DAYS.toMillis(3));
        //car is returned to the same store it was picked up from
        Reservation reservation = store.createReservation(storeVehicles.get(0), users.get(0),
                ReservationType.DAILY, bookedFrom, bookedTo, store.storeLocation);

        //4. generate the bill
        Bill bill = new Bill(reservation);
        System.out.println("Reservation " + reservation.reservationId + " amount = " + bill.totalBillAmount);

        //5. make payment
        Payment payment = new Payment();
        payment.payBill(bill, PaymentMode.ONLINE);

        //6. trip completed, submit the vehicle and close the reservation
        store.completeReservation(reservation.reservationId);

    }


    public static List<Vehicle> addVehicles() {

        List<Vehicle> vehicles = new ArrayList<>();

        Vehicle vehicle1 = new Vehicle();
        vehicle1.setVehicleID(1);
        vehicle1.setVehicleType(VehicleType.CAR);
        vehicle1.setHourlyRentalCost(120);
        vehicle1.setDailyRentalCost(2400);
        vehicle1.setNoOfSeat(5);
        vehicle1.setStatus(Status.ACTIVE);

        Vehicle vehicle2 = new Vehicle();
        vehicle2.setVehicleID(2);
        vehicle2.setVehicleType(VehicleType.CAR);
        vehicle2.setHourlyRentalCost(100);
        vehicle2.setDailyRentalCost(1900);
        vehicle2.setNoOfSeat(5);
        vehicle2.setStatus(Status.ACTIVE);

        Vehicle vehicle3 = new Vehicle();
        vehicle3.setVehicleID(3);
        vehicle3.setVehicleType(VehicleType.BIKE);
        vehicle3.setHourlyRentalCost(45);
        vehicle3.setDailyRentalCost(700);
        vehicle3.setNoOfSeat(2);
        vehicle3.setStatus(Status.ACTIVE);

        vehicles.add(vehicle1);
        vehicles.add(vehicle2);
        vehicles.add(vehicle3);

        return vehicles;
    }

    public static List<User> addUsers() {

        List<User> users = new ArrayList<>();
        User user1 = new User();
        user1.setUserId(1);
        user1.setUserName("Aarav Sharma");
        user1.setDrivingLicense("KA-2031-0099");

        users.add(user1);
        return users;
    }

    public static List<Store> addStores(List<Vehicle> vehicles) {

        List<Store> stores = new ArrayList<>();
        Store store1 = new Store();
        store1.storeId = 1;
        store1.storeLocation = new Location("100 Feet Road, Indiranagar", 403012, "Bangalore", "Karnataka", "India");
        store1.setVehicles(vehicles);

        stores.add(store1);
        return stores;
    }

}

// ---------------------------------------------------------------- Product

class Vehicle {

    int vehicleID;
    VehicleType vehicleType;
    int dailyRentalCost;
    int hourlyRentalCost;
    int noOfSeat;
    Status status;

    //getters and setters


    public int getVehicleID() {
        return vehicleID;
    }

    public void setVehicleID(int vehicleID) {
        this.vehicleID = vehicleID;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public int getDailyRentalCost() {
        return dailyRentalCost;
    }

    public void setDailyRentalCost(int dailyRentalCost) {
        this.dailyRentalCost = dailyRentalCost;
    }

    public int getHourlyRentalCost() {
        return hourlyRentalCost;
    }

    public void setHourlyRentalCost(int hourlyRentalCost) {
        this.hourlyRentalCost = hourlyRentalCost;
    }

    public int getNoOfSeat() {
        return noOfSeat;
    }

    public void setNoOfSeat(int noOfSeat) {
        this.noOfSeat = noOfSeat;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}

//the type is what separates one vehicle from another, so no subclasses are needed
enum VehicleType {
    CAR,
    BIKE;
}

enum Status {

    ACTIVE,
    INACTIVE;
}

// ---------------------------------------------------------------- User & Location

class User {

    int userId;
    String userName;
    String drivingLicense;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDrivingLicense() {
        return drivingLicense;
    }

    public void setDrivingLicense(String drivingLicense) {
        this.drivingLicense = drivingLicense;
    }
}

class Location {

    String address;
    int pincode;
    String city;
    String state;
    String country;

    //used when a customer searches an area, the street address is not known yet
    Location(int pincode, String city, String state, String country) {
        this.pincode = pincode;
        this.city = city;
        this.state = state;
        this.country = country;

    }

    //used for a physical place like a store
    Location(String address, int pincode, String city, String state, String country) {
        this(pincode, city, state, country);
        this.address = address;
    }
}

// ---------------------------------------------------------------- Store & Inventory

class VehicleRentalSystem {

    List<Store> storeList;
    List<User> userList;

    VehicleRentalSystem(List<Store> stores, List<User> users) {

        this.storeList = stores;
        this.userList = users;
    }


    //a city can have many stores, so return all of them and let the user choose
    public List<Store> getStores(Location location) {

        List<Store> nearByStores = new ArrayList<>();
        for (Store store : storeList) {
            //The equalsIgnoreCase() method in Java belongs to the java.lang.String class and is used to compare two strings while ignoring upper and lower case differences
            if (store.storeLocation != null && store.storeLocation.city.equalsIgnoreCase(location.city)) {
                nearByStores.add(store);
            }
        }
        return nearByStores;
    }


    public void addUser(User user) {
        userList.add(user);
    }

    public boolean removeUser(int userId) {
        return userList.removeIf(user -> user.getUserId() == userId);
    }

    public void addStore(Store store) {
        storeList.add(store);
    }

    public boolean removeStore(int storeId) {
        return storeList.removeIf(store -> store.storeId == storeId);
    }

}

class Store {

    int storeId;
    VehicleInventoryManagement inventoryManagement;
    Location storeLocation;
    List<Reservation> reservations = new ArrayList<>();


    public List<Vehicle> getVehicles(VehicleType vehicleType) {

        return inventoryManagement.getVehicles(vehicleType);
    }


    public void setVehicles(List<Vehicle> vehicles) {
        inventoryManagement = new VehicleInventoryManagement(vehicles);
    }

    public void addVehicle(Vehicle vehicle) {
        inventoryManagement.addVehicle(vehicle);
    }

    public boolean updateVehicleStatus(int vehicleID, Status status) {
        Vehicle vehicle = inventoryManagement.getVehicle(vehicleID);
        if (vehicle == null) {
            return false;
        }
        vehicle.setStatus(status);
        return true;
    }

    public Reservation createReservation(Vehicle vehicle, User user, ReservationType reservationType,
                                         Date dateBookedFrom, Date dateBookedTo, Location dropLocation) {
        //the vehicle is always picked up from this store
        Reservation reservation = new Reservation(user, vehicle, reservationType, dateBookedFrom, dateBookedTo,
                storeLocation, dropLocation);
        reservations.add(reservation);
        return reservation;
    }

    public boolean completeReservation(int reservationID) {

        Reservation reservation = getReservation(reservationID);
        if (reservation == null) {
            return false;
        }
        reservation.completeReserve();
        return true;
    }

    public boolean cancelReservation(int reservationID) {
        Reservation reservation = getReservation(reservationID);
        if (reservation == null) {
            return false;
        }
        reservation.cancelReserve();
        return true;
    }

    private Reservation getReservation(int reservationID) {
        for (Reservation reservation : reservations) {
            if (reservation.reservationId == reservationID) {
                return reservation;
            }
        }
        return null;
    }

}

class VehicleInventoryManagement {

    List<Vehicle> vehicles;

    VehicleInventoryManagement(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<Vehicle> getVehicles(VehicleType vehicleType) {
        List<Vehicle> filteredVehicles = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getVehicleType() == vehicleType && vehicle.getStatus() == Status.ACTIVE) {
                filteredVehicles.add(vehicle);
            }
        }
        return filteredVehicles;
    }

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public void addVehicle(Vehicle vehicle) {
        vehicles.add(vehicle);
    }

    public Vehicle getVehicle(int vehicleID) {
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getVehicleID() == vehicleID) {
                return vehicle;
            }
        }
        return null;
    }
}

// ---------------------------------------------------------------- Reservation

class Reservation {

    private static final Random RANDOM = new Random();

    int reservationId;
    User user;
    Vehicle vehicle;
    Date bookingDate;
    Date dateBookedFrom;
    Date dateBookedTo;
    Long fromTimeStamp;
    Long toTimeStamp;
    Location pickUpLocation;
    Location dropLocation;
    ReservationType reservationType;
    ReservationStatus reservationStatus;

    Reservation(User user, Vehicle vehicle, ReservationType reservationType,
                Date dateBookedFrom, Date dateBookedTo,
                Location pickUpLocation, Location dropLocation) {

        //generate new id
        this.reservationId = 10000 + RANDOM.nextInt(90000);
        this.user = user;
        this.vehicle = vehicle;
        this.reservationType = reservationType;
        this.bookingDate = new Date();
        this.dateBookedFrom = dateBookedFrom;
        this.dateBookedTo = dateBookedTo;
        //kept in sync with the dates above, they are what the bill is calculated from
        this.fromTimeStamp = dateBookedFrom.getTime();
        this.toTimeStamp = dateBookedTo.getTime();
        this.pickUpLocation = pickUpLocation;
        this.dropLocation = dropLocation;
        this.reservationStatus = ReservationStatus.SCHEDULED;
        vehicle.setStatus(Status.INACTIVE);
    }

    // CRUD operations

    public void completeReserve() {
        reservationStatus = ReservationStatus.COMPLETED;
        vehicle.setStatus(Status.ACTIVE);
    }

    public void cancelReserve() {
        reservationStatus = ReservationStatus.CANCELLED;
        vehicle.setStatus(Status.ACTIVE);
    }

}

enum ReservationType {

    HOURLY,
    DAILY;
}

enum ReservationStatus {

    SCHEDULED,
    INPROGRESS,
    COMPLETED,
    CANCELLED;
}

// ---------------------------------------------------------------- Bill & Payment

class Bill {

    Reservation reservation;
    double totalBillAmount;
    boolean isBillPaid;

    Bill(Reservation reservation) {
        this.reservation = reservation;
        this.totalBillAmount = computeBillAmount();
        isBillPaid = false;
    }

    private double computeBillAmount() {

        //example: booked 1 Oct to 4 Oct => diff betwen 2 dates in ms = 259200000 ms, 1 sec=1000 ms 
       // convert seconds to hours or days post that
        long milliSeconds = reservation.toTimeStamp - reservation.fromTimeStamp;

        if (reservation.reservationType == ReservationType.HOURLY) {
            long hours = TimeUnit.MILLISECONDS.toHours(milliSeconds);
            return hours * reservation.vehicle.getHourlyRentalCost();
        }

        long days = TimeUnit.MILLISECONDS.toDays(milliSeconds);
        return days * reservation.vehicle.getDailyRentalCost();
    }

}

class Payment {

    public void payBill(Bill bill, PaymentMode paymentMode) {
        //a real gateway call would happen here, for now we only update the bill status
        bill.isBillPaid = true;
        System.out.println("Bill of " + bill.totalBillAmount + " paid by " + paymentMode);
    }
}

enum PaymentMode {

    CASH,
    ONLINE;
}
