import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

// --- Enums ---

enum Direction {
    UP, DOWN, IDLE
}

enum DoorState {
    OPEN, CLOSED
}

// --- Request ---

class Request {
    private final int targetFloor;
    private final boolean internal;

    public Request(int targetFloor, boolean internal) {
        this.targetFloor = targetFloor;
        this.internal = internal;
    }

    public int getTargetFloor() { return targetFloor; }
    public boolean isInternal() { return internal; }

    @Override
    public String toString() {
        return "Request[targetFloor=" + targetFloor + ", internal=" + internal + "]";
    }
}

// --- Scheduling Strategy (Strategy Pattern) ---

interface SchedulingStrategy {
    Elevator assignElevator(List<Elevator> elevators, Request request);
}

class NearestSuitableStrategy implements SchedulingStrategy {
    @Override
    public Elevator assignElevator(List<Elevator> elevators, Request request) {
        Elevator best = null;
        int bestScore = Integer.MAX_VALUE;
        for (Elevator e : elevators) {
            int score = e.scoreForRequest(request);
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }
}

class LeastBusyStrategy implements SchedulingStrategy {
    @Override
    public Elevator assignElevator(List<Elevator> elevators, Request request) {
        Elevator best = null;
        int minLoad = Integer.MAX_VALUE;
        for (Elevator e : elevators) {
            int load = e.pendingRequestCount();
            if (load < minLoad) {
                minLoad = load;
                best = e;
            }
        }
        return best;
    }
}

// --- Dispatcher ---

class Dispatcher {
    private SchedulingStrategy strategy;

    public Dispatcher(SchedulingStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(SchedulingStrategy strategy) {
        this.strategy = strategy;
    }

    public Elevator dispatch(Request request, List<Elevator> elevators) {
        return strategy.assignElevator(elevators, request);
    }
}

// --- Elevator ---

class Elevator {
    private final int id;
    private int currentFloor;
    private Direction direction = Direction.IDLE;
    private DoorState doorState = DoorState.CLOSED;

    private final TreeSet<Integer> upRequests = new TreeSet<>();
    private final TreeSet<Integer> downRequests = new TreeSet<>(Collections.reverseOrder());

    private final int minFloor = 1;
    private final int maxFloor;

    public Elevator(int id, int initialFloor, int maxFloor) {
        this.id = id;
        this.currentFloor = initialFloor;
        this.maxFloor = maxFloor;
    }

    public int getId() { return id; }
    public int getCurrentFloor() { return currentFloor; }
    public Direction getDirection() { return direction; }
    public DoorState getDoorState() { return doorState; }

    public void addExternalRequest(Request req) {
        int floor = req.getTargetFloor();
        if (floor == currentFloor) {
            openDoor();
            return;
        }
        if (floor > currentFloor) {
            upRequests.add(floor);
            if (direction == Direction.IDLE) direction = Direction.UP;
        } else {
            downRequests.add(floor);
            if (direction == Direction.IDLE) direction = Direction.DOWN;
        }
    }

    public void addInternalRequest(int floor) {
        if (floor == currentFloor) {
            openDoor();
            return;
        }
        if (floor > currentFloor) {
            upRequests.add(floor);
            if (direction == Direction.IDLE) direction = Direction.UP;
        } else {
            downRequests.add(floor);
            if (direction == Direction.IDLE) direction = Direction.DOWN;
        }
    }

    public void step() {
        if (doorState == DoorState.OPEN) {
            closeDoor();
            return;
        }

        if (direction == Direction.IDLE) {
            if (!upRequests.isEmpty()) direction = Direction.UP;
            else if (!downRequests.isEmpty()) direction = Direction.DOWN;
            else return;
        }

        if (direction == Direction.UP) {
            if (currentFloor < maxFloor) currentFloor++;
            System.out.println("Elevator " + id + " moved UP to " + currentFloor);
            if (upRequests.contains(currentFloor)) {
                upRequests.remove(currentFloor);
                openDoor();
            }
            if (upRequests.isEmpty() && !downRequests.isEmpty() && doorState == DoorState.CLOSED) {
                direction = Direction.DOWN;
            }
        } else if (direction == Direction.DOWN) {
            if (currentFloor > minFloor) currentFloor--;
            System.out.println("Elevator " + id + " moved DOWN to " + currentFloor);
            if (downRequests.contains(currentFloor)) {
                downRequests.remove(currentFloor);
                openDoor();
            }
            if (downRequests.isEmpty() && !upRequests.isEmpty() && doorState == DoorState.CLOSED) {
                direction = Direction.UP;
            }
        }

        if (upRequests.isEmpty() && downRequests.isEmpty() && doorState == DoorState.CLOSED) {
            direction = Direction.IDLE;
        }
    }

    private void openDoor() {
        doorState = DoorState.OPEN;
        System.out.println("Elevator " + id + " DOOR OPEN at " + currentFloor);
    }

    private void closeDoor() {
        doorState = DoorState.CLOSED;
        System.out.println("Elevator " + id + " DOOR CLOSED at " + currentFloor);
    }

    public int scoreForRequest(Request req) {
        int distance = Math.abs(currentFloor - req.getTargetFloor());
        if (direction == Direction.IDLE) return distance;
        if (direction == Direction.UP && req.getTargetFloor() >= currentFloor) {
            return distance;
        }
        if (direction == Direction.DOWN && req.getTargetFloor() <= currentFloor) {
            return distance;
        }
        return distance + 1000; // penalty for needing direction reversal
    }

    public int pendingRequestCount() {
        return upRequests.size() + downRequests.size();
    }

    public String status() {
        return "E" + id + "[floor=" + currentFloor + ", dir=" + direction + ", door=" + doorState +
                ", up=" + upRequests + ", down=" + downRequests + "]";
    }
}

// --- Elevator Controller ---

class ElevatorController {
    private final List<Elevator> elevators = new ArrayList<>();
    private final Dispatcher dispatcher;

    public ElevatorController(int elevatorCount, int floors, SchedulingStrategy strategy) {
        for (int i = 1; i <= elevatorCount; i++) {
            elevators.add(new Elevator(i, 1, floors));
        }
        this.dispatcher = new Dispatcher(strategy);
    }

    public List<Elevator> getElevators() { return elevators; }
    public Dispatcher getDispatcher() { return dispatcher; }

    public void externalRequest(int floor) {
        Request req = new Request(floor, false);
        System.out.println("Controller received external " + req);
        Elevator chosen = dispatcher.dispatch(req, elevators);
        System.out.println("Dispatcher assigned Elevator " + chosen.getId() + " for " + req);
        chosen.addExternalRequest(req);
    }

    public void internalRequest(int elevatorId, int floor) {
        Elevator e = findElevator(elevatorId);
        System.out.println("Controller received internal request for E" + elevatorId + " -> floor " + floor);
        e.addInternalRequest(floor);
    }

    public void stepSimulation() {
        for (Elevator e : elevators) {
            e.step();
        }
        printStatus();
    }

    private Elevator findElevator(int id) {
        for (Elevator e : elevators) if (e.getId() == id) return e;
        return null;
    }

    private void printStatus() {
        System.out.println("-- Status --");
        for (Elevator e : elevators) {
            System.out.println("  " + e.status());
        }
    }
}

// --- Entry Point ---

public class ElevatorSystemApp {
    public static void main(String[] args) {
        int floors = 10;

        // Start with NearestSuitable strategy
        ElevatorController controller = new ElevatorController(3, floors, new NearestSuitableStrategy());

        // External requests (someone presses button on a floor)
        controller.externalRequest(3);
        controller.externalRequest(7);

        // Internal requests (someone inside elevator presses a floor button)
        controller.internalRequest(1, 9);
        controller.internalRequest(2, 1);

        // Run simulation
        for (int i = 0; i < 15; i++) {
            System.out.println("\n--- Step " + (i + 1) + " ---");
            controller.stepSimulation();
        }

        // Switch strategy mid-run
        System.out.println("\n=== Switching to LeastBusyStrategy ===\n");
        controller.getDispatcher().setStrategy(new LeastBusyStrategy());

        controller.externalRequest(2);
        controller.externalRequest(8);

        for (int i = 15; i < 30; i++) {
            System.out.println("\n--- Step " + (i + 1) + " ---");
            controller.stepSimulation();
        }

        System.out.println("\nSimulation ended.");
    }
}
