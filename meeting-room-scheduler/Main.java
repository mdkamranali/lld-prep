import java.util.ArrayList;
import java.util.List;

// --- Data classes ---

class TimeSlot {
    int startTime;
    int endTime;

    TimeSlot(int startTime, int endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    boolean overlapsWith(TimeSlot other) {
        if (this.endTime < other.startTime || this.startTime > other.endTime) {
            return false;
        }
        return true;
    }
}

class Location {
    int floorId;
    int buildingId;

    Location(int floorId, int buildingId) {
        this.floorId = floorId;
        this.buildingId = buildingId;
    }
}

class Calendar {
    List<Meeting> meetings = new ArrayList<>();

    void addMeeting(Meeting meeting) {
        meetings.add(meeting);
    }

    void removeMeeting(Meeting meeting) {
        meetings.remove(meeting);
    }

    void viewCalendar(String ownerName) {
        System.out.println("\n--- Calendar for " + ownerName + " ---");
        if (meetings.isEmpty()) {
            System.out.println("  No meetings scheduled.");
        }
        for (Meeting m : meetings) {
            System.out.println("  " + m.meetingId + " | " + m.title + " | " + m.timeSlot.startTime + "-" + m.timeSlot.endTime);
        }
    }
}

class Participant {
    String name;
    String email;
    Calendar calendar;

    Participant(String name, String email) {
        this.name = name;
        this.email = email;
        this.calendar = new Calendar();
    }

    void update(String message) {
        System.out.println("  Email sent to " + this.email + ": " + message);
    }

    void viewCalendar() {
        calendar.viewCalendar(this.name);
    }
}

class Meeting {
    String meetingId;
    String title;
    List<Participant> participants;
    TimeSlot timeSlot;
    MeetingRoom room;

    Meeting(String meetingId, String title, List<Participant> participants, TimeSlot timeSlot, MeetingRoom room) {
        this.meetingId = meetingId;
        this.title = title;
        this.participants = participants;
        this.timeSlot = timeSlot;
        this.room = room;
    }

    void addParticipant(Participant participant) {
        this.participants.add(participant);
    }

    void removeParticipant(Participant participant) {
        this.participants.remove(participant);
    }

    void notifyParticipants(String message) {
        for (Participant participant : participants) {
            participant.update(message);
        }
    }
}

class MeetingRoom {
    String roomId;
    int capacity;
    Location location;
    List<TimeSlot> bookedSlots;
    Calendar calendar;

    MeetingRoom(String roomId, Location location, int capacity) {
        this.roomId = roomId;
        this.capacity = capacity;
        this.location = location;
        this.bookedSlots = new ArrayList<>();
        this.calendar = new Calendar();
    }

    boolean isAvailable(TimeSlot slot) {
        for (TimeSlot booked : bookedSlots) {
            if (booked.overlapsWith(slot)) {
                return false;
            }
        }
        return true;
    }

    void book(TimeSlot slot) {
        bookedSlots.add(slot);
    }

    void releaseSlot(TimeSlot slot) {
        bookedSlots.remove(slot);
    }

    void viewCalendar() {
        calendar.viewCalendar(this.roomId);
    }
}

// --- Controller: manages all rooms ---

class MeetingRoomController {
    List<MeetingRoom> meetingRooms = new ArrayList<>();

    void addMeetingRoom(MeetingRoom room) {
        meetingRooms.add(room);
    }

    List<MeetingRoom> getAvailableRooms(TimeSlot timeSlot, int numParticipants) {
        List<MeetingRoom> available = new ArrayList<>();
        for (MeetingRoom room : meetingRooms) {
            if (room.capacity >= numParticipants && room.isAvailable(timeSlot)) {
                available.add(room);
            }
        }
        return available;
    }
}

// --- Scheduler: uses controller to find/book/cancel rooms ---

class MeetingScheduler {
    MeetingRoomController controller;

    MeetingScheduler(MeetingRoomController controller) {
        this.controller = controller;
    }

    Meeting scheduleMeeting(String meetingId, String title, List<Participant> participants, TimeSlot timeSlot) {
        List<MeetingRoom> availableRooms = controller.getAvailableRooms(timeSlot, participants.size());

        if (availableRooms.isEmpty()) {
            System.out.println("No room available for '" + title + "' at the requested time.");
            return null;
        }

        // Pick the first available room
        MeetingRoom selectedRoom = availableRooms.get(0);
        selectedRoom.book(timeSlot);

        Meeting meeting = new Meeting(meetingId, title, participants, timeSlot, selectedRoom);

        // Add meeting to room's calendar
        selectedRoom.calendar.addMeeting(meeting);

        // Add meeting to each participant's calendar
        for (Participant p : participants) {
            p.calendar.addMeeting(meeting);
        }

        // Notify participants
        meeting.notifyParticipants("Meeting '" + title + "' scheduled in " + selectedRoom.roomId +
                " from " + timeSlot.startTime + " to " + timeSlot.endTime);

        return meeting;
    }

    void cancelMeeting(Meeting meeting) {
        System.out.println("\nCancelling meeting: " + meeting.meetingId + " - " + meeting.title);

        // Release the room slot
        meeting.room.releaseSlot(meeting.timeSlot);
        meeting.room.calendar.removeMeeting(meeting);

        // Remove from each participant's calendar
        for (Participant p : meeting.participants) {
            p.calendar.removeMeeting(meeting);
        }

        // Notify participants about cancellation
        meeting.notifyParticipants("Meeting '" + meeting.title + "' has been cancelled.");
    }
}

// --- Entry point ---

public class MeetingSchedulerApp {
    public static void main(String[] args) {

        // Create rooms with location
        MeetingRoom room1 = new MeetingRoom("Room-A", new Location(1, 101), 3);
        MeetingRoom room2 = new MeetingRoom("Room-B", new Location(1, 101), 2);
        MeetingRoom room3 = new MeetingRoom("Room-C", new Location(2, 101), 5);

        // Controller manages rooms
        MeetingRoomController controller = new MeetingRoomController();
        controller.addMeetingRoom(room1);
        controller.addMeetingRoom(room2);
        controller.addMeetingRoom(room3);

        // Scheduler uses controller
        MeetingScheduler scheduler = new MeetingScheduler(controller);

        // Create participants
        Participant p1 = new Participant("Alice", "alice@example.com");
        Participant p2 = new Participant("Bob", "bob@example.com");

        List<Participant> participants = new ArrayList<>();
        participants.add(p1);
        participants.add(p2);

        // Schedule meetings
        Meeting m1 = scheduler.scheduleMeeting("M1", "Archiving Discussion", participants, new TimeSlot(2, 3));
        Meeting m2 = scheduler.scheduleMeeting("M2", "Workflow Discussion", participants, new TimeSlot(4, 5));

        // View participant calendars
        p1.viewCalendar();
        p2.viewCalendar();

        // View meeting room's calendar
        room1.viewCalendar();

        // Cancel a meeting
        scheduler.cancelMeeting(m1);

        // View calendars after cancellation
        p1.viewCalendar();
        room1.viewCalendar();
    }
}
