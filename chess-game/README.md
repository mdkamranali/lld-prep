# Chess Game - Low Level Design

## Overview
A console Chess engine for 2 players on a standard 8x8 board. It validates every
move, refuses illegal ones with a reason, and detects check, checkmate and
stalemate. Any move can be undone.

## Design Decisions
- Friendly piece names are used so the rules read in plain English:
  `Elephant` = Rook, `Camel` = Bishop, `Horse` = Knight, `Soldier` = Pawn
- Each piece subclass owns only its own movement rule (`canMove`), so adding a
  new piece means adding one small class and changing nothing else
- No `instanceof` anywhere. Pieces answer questions about themselves through
  `isKing()` and `canBePromoted()`, so no other class needs to know the list of
  piece types
- Pieces do not store their own position. The `Board` grid is the single source
  of truth, which removes any chance of the two going out of sync on an undo
- `Move` remembers the piece it killed, so it can undo itself. The same
  mechanism powers both the undo feature and move legality checking
- A move is legal only if it does not leave your own King attacked. Every
  candidate is actually played on the board, checked, and rolled back. That one
  trick gives pins, checkmate and stalemate without any special-case code
- Board coordinates: row 0 is rank 8 (Black's back row), row 7 is rank 1
  (White's back row), so the square "e2" is row 6, col 4

## Classes
| Class | Responsibility |
|-------|---------------|
| `Color` | Enum for WHITE and BLACK |
| `Piece` | Abstract base: colour, board letter, shared path and landing helpers |
| `Elephant` | Rook - straight lines, any distance, cannot jump |
| `Camel` | Bishop - diagonals, any distance, cannot jump |
| `Queen` | Straight lines plus diagonals |
| `Horse` | Knight - L shape, the only piece that jumps |
| `King` | One square in any direction |
| `Soldier` | Pawn - moves forward, kills diagonally, promotes at the far end |
| `Board` | The 8x8 grid, initial setup, printing, "is this King attacked?" |
| `Move` | Executes and undoes a single move, remembering the killed piece |
| `ChessGame` | Turn order, validation pipeline, check / checkmate / stalemate, undo |
| `Main` | Entry point with a scripted demo |

## Move Validation Pipeline
Cheapest checks run first, the expensive one runs last:
1. Is the game still running?
2. Are the row/col numbers on the board?
3. Is there a piece of the current player's colour on the source square?
4. Does that piece move like that, with a clear path, onto a legal square?
5. Simulate it: does the move leave the player's own King attacked?

## Game Ending Rules
Both endings share the same first condition - the player to move has no legal
move at all. The only difference is whether that player's King is attacked:

| No legal moves | King attacked | Result |
|----------------|---------------|--------|
| yes | yes | Checkmate |
| yes | no  | Stalemate (draw) |

## Not Implemented
Castling, en passant and threefold repetition. Each one has a comment in the
code marking exactly where it would plug in.

## How to Run
```bash
cd src
javac Main.java
java Main
```

The demo prints the starting board, shows several illegal moves being refused,
then plays Fool's mate (the fastest checkmate in chess) and undoes it.

<img width="2114" height="1364" alt="image" src="https://github.com/user-attachments/assets/ff85380a-ee14-4f91-826e-34eadeb3f731" />


<img width="2170" height="1430" alt="image" src="https://github.com/user-attachments/assets/3d7ec258-3c37-4633-9201-d267a816f97f" />

