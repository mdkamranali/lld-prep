# Meeting Room Scheduler - Low Level Design

## Problem Statement
Design a meeting room scheduler that allows users to book available meeting rooms, view calendars, and cancel meetings.

## Features
- Schedule a meeting in an available room based on capacity and time slot
- View participant's calendar
- View meeting room's calendar
- Cancel a meeting (releases room, notifies participants)
- Notify participants on booking/cancellation

## Class Diagram (UML)

```
┌─────────────┐       ┌─────────────┐
│  TimeSlot   │       │  Location   │
├─────────────┤       ├─────────────┤
│ startTime   │       │ floorId     │
│ endTime     │       │ buildingId  │
├─────────────┤       └─────────────┘
│ overlapsWith│
└─────────────┘

┌──────────────┐        ┌──────────────────┐
│  Calendar    │        │   Participant    │
├──────────────┤        ├──────────────────┤
│ meetings     │◄───────│ name             │
├──────────────┤        │ email            │
│ addMeeting() │        │ calendar         │
│ removeMeeting│        ├──────────────────┤
│ viewCalendar │        │ update()         │
└──────────────┘        │ viewCalendar()   │
       ▲                └──────────────────┘
       │
┌──────────────┐        ┌──────────────────┐
│ MeetingRoom  │        │    Meeting       │
├──────────────┤        ├──────────────────┤
│ roomId       │◄───────│ meetingId        │
│ capacity     │        │ title            │
│ location     │        │ participants     │
│ bookedSlots  │        │ timeSlot         │
│ calendar     │        │ room             │
├──────────────┤        ├──────────────────┤
│ isAvailable()│        │ addParticipant() │
│ book()       │        │ removeParticipant│
│ releaseSlot()│        │ notifyParticipants│
│ viewCalendar │        └──────────────────┘
└──────────────┘

┌──────────────────────┐       ┌──────────────────┐
│ MeetingRoomController│       │ MeetingScheduler │
├──────────────────────┤       ├──────────────────┤
│ meetingRooms         │◄──────│ controller       │
├──────────────────────┤       ├──────────────────┤
│ addMeetingRoom()     │       │ scheduleMeeting()│
│ getAvailableRooms()  │       │ cancelMeeting()  │
└──────────────────────┘       └──────────────────┘
```

## Flow

### Schedule a Meeting
1. `MeetingScheduler.scheduleMeeting()` is called with participants, time slot, title
2. Asks `MeetingRoomController.getAvailableRooms()` for rooms matching capacity + time
3. Picks the first available room
4. Books the time slot on the room
5. Adds meeting to room's calendar and each participant's calendar
6. Notifies all participants via `participant.update()`

### Cancel a Meeting
1. `MeetingScheduler.cancelMeeting()` is called with the meeting
2. Releases the time slot from the room (`releaseSlot`)
3. Removes meeting from room's calendar
4. Removes meeting from each participant's calendar
5. Notifies all participants about cancellation

### Check Overlap
- Two time slots overlap if: `NOT (this.endTime < other.startTime OR this.startTime > other.endTime)`
- If either condition is true, they don't overlap

## Design Patterns Used
- **Controller Pattern** — `MeetingRoomController` manages room inventory and availability queries
- **Separation of Concerns** — Scheduler handles orchestration, Controller handles room state
