# Tic Tac Toe - Low Level Design

## Overview
A simplified console-based Tic Tac Toe game for 2 players.

## Design Decisions
- Uses `PieceType` enum (`X`, `O`) directly instead of a class hierarchy (`PlayingPiece`, `PlayingPieceX`, `PlayingPieceO`)
- `Player` holds a `PieceType` enum value directly
- `Board` stores a `PieceType[][]` grid instead of object references
- No external dependencies (removed antlr `Pair` usage)

## Classes
| Class | Responsibility |
|-------|---------------|
| `PieceType` | Enum representing X and O |
| `Player` | Holds player name and their piece type |
| `Board` | Manages the grid, placement, free cells, printing |
| `TicTacToeGame` | Game loop, turn management, win detection |
| `Main` | Entry point |

## How to Run
```bash
cd src
javac Main.java
java Main
```
