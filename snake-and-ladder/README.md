# Snake and Ladder - Low Level Design

## Overview
A simplified Snake and Ladder game implementation in Java.

## Design

### Classes
- **Cell** - Represents a cell on the board with `start` and `end` positions. By default, `start == end == index`. If a snake or ladder exists, `end` points to the destination.
- **Dice** - Handles dice rolling logic.
- **Player** - Represents a player with an id and current position.
- **Game** - Orchestrates the game. Holds the Cell array, snakes list, ladders list, players, and game logic.

### Simplified Approach
- No separate Board or Jump class.
- The Game class maintains a flat `Cell[]` array of size `boardSize`.
- Snakes and ladders are stored as `List<int[]>` (pairs of start, end).
- During initialization, cells affected by snakes/ladders have their `end` field updated to reflect the jump destination.

## How to Run
```bash
javac src/*.java
java -cp src Main
```
