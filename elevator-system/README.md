# Elevator System - Low Level Design

## Problem Statement
Design an elevator system that manages multiple elevators, handles external (floor button) and internal (inside elevator) requests, and uses pluggable scheduling strategies.

## Features
- Multiple elevators with SCAN-based movement algorithm
- External requests (press button on a floor to call elevator)
- Internal requests (press floor button inside elevator)
- Strategy Pattern for dispatching: NearestSuitable, LeastBusy
- Runtime strategy switching
- Door open/close simulation

## Class Diagram (UML)

```
┌──────────────┐
│  Direction   │  (enum: UP, DOWN, IDLE)
└──────────────┘

┌──────────────┐
│  DoorState   │  (enum: OPEN, CLOSED)
└──────────────┘

┌──────────────────┐
│    Request       │
├──────────────────┤
│ targetFloor      │
│ internal         │
├──────────────────┤
│ getTargetFloor() │
│ isInternal()     │
└──────────────────┘

┌─────────────────────────┐
│  SchedulingStrategy     │  (interface)
├─────────────────────────┤
│ assignElevator()        │
└─────────────────────────┘
        ▲           ▲
        │           │
┌───────────────┐  ┌────────────────┐
│NearestSuitable│  │  LeastBusy     │
│   Strategy    │  │  Strategy      │
└───────────────┘  └────────────────┘

┌──────────────────┐       ┌──────────────────┐
│   Dispatcher     │       │    Elevator      │
├──────────────────┤       ├──────────────────┤
│ strategy         │       │ id, currentFloor │
├──────────────────┤       │ direction        │
│ dispatch()       │       │ doorState        │
│ setStrategy()    │       │ upRequests (Tree)│
└──────────────────┘       │ downRequests     │
                           ├──────────────────┤
                           │ addExternalReq() │
                           │ addInternalReq() │
                           │ step()           │
                           │ scoreForRequest()│
                           │ pendingReqCount()│
                           └──────────────────┘

┌─────────────────────────┐
│  ElevatorController     │
├─────────────────────────┤
│ elevators               │
│ dispatcher              │
├─────────────────────────┤
│ externalRequest()       │
│ internalRequest()       │
│ stepSimulation()        │
└─────────────────────────┘
```

## Flow

### External Request (someone presses button on a floor)
1. `ElevatorController.externalRequest(floor)` creates a Request
2. Passes to `Dispatcher.dispatch()` which uses the current strategy
3. Strategy scores all elevators and picks the best one
4. Elevator adds the floor to its upRequests or downRequests TreeSet

### Internal Request (someone inside elevator presses a floor)
1. `ElevatorController.internalRequest(elevatorId, floor)` finds the elevator
2. Directly adds floor to that elevator's request queue

### Elevator Movement (SCAN algorithm)
1. Each `step()` moves elevator one floor in current direction
2. If current floor matches a request, removes it and opens door
3. When no more requests in current direction, reverses
4. When no requests at all, goes IDLE

### Scoring (NearestSuitable)
- IDLE elevator: score = distance to request floor
- Moving toward request: score = distance
- Moving away from request: score = distance + 1000 (penalty)

## Design Patterns Used
- **Strategy Pattern** — Pluggable scheduling (NearestSuitable, LeastBusy)
- **SCAN Algorithm** — Elevator serves all requests in one direction before reversing
