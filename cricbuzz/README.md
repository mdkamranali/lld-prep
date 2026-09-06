# Cricbuzz — Low Level Design

Design a live cricket scoring application (à la Cricbuzz). The system simulates a full match — toss, two innings, ball-by-ball delivery, wicket handling, strike rotation, batting & bowling scorecards, and declaring a winner.

## Patterns Used

| Pattern | Where |
| --- | --- |
| **Strategy** | `MatchType` (`T20MatchType`, `OneDayMatchType`) picks match rules — overs count and per-bowler over cap. |
| **Observer** | Every ball notifies a list of `ScoreUpdaterObserver`s (`BattingScoreUpdater`, `BowlingScoreUpdater`) so scorecards stay in sync without the ball knowing who cares. |
| **Controller (delegation)** | `PlayerBattingController` / `PlayerBowlingController` encapsulate "who bats next" and "which bowler comes on" so `Team` stays lean. |
| **Facade** | `Match#startMatch()` hides the toss → innings → over → ball orchestration behind a single call. |

## Domain Model

```
Match
 ├── teamA / teamB : Team
 │      ├── playing11 : Queue<Player>
 │      ├── battingController → striker / nonStriker / yet-to-play
 │      └── bowlingController → next bowler (round-robin, capped)
 ├── matchType     : MatchType   (T20 / ODI)
 └── innings[2]    : InningDetails
          └── overs : List<OverDetails>
                 └── balls : List<BallDetails>
                        ├── playedBy / bowledBy : Player
                        ├── runType             : RunType
                        └── wicketType          : WicketType?  // null => no wicket
```

`Player` merges person info (name / age / address) and cricket state (type, batting & bowling scorecards, `outBy` bowler).

## End-to-End Flow

1. `Main.main()` builds two teams of 11 (last four also bowl) and starts a `T20MatchType` match.
2. `Match#startMatch()` runs a random `toss()` and picks the batting order.
3. For each of the 2 innings, `InningDetails#start(runsToWin)`:
   - Sends the opening pair to the crease.
   - Runs `noOfOvers()` overs; before each over, `chooseNextBowler(maxOverCountBowlers())` picks a bowler round-robin, skipping anyone at cap.
   - After every over, striker & non-striker swap.
4. Each `OverDetails#startOver()` bowls 6 legal deliveries. On each ball:
   - `BallDetails#startBallDelivery()` decides: wicket (~20%) or a run (1 / 2 / 4 / 6). Odd runs swap strike.
   - Observers update the batting & bowling scorecards.
   - On a wicket, `chooseNextBatsMan()` sends the next batter in.
   - In the 2nd innings, if the chase target is reached, the innings ends immediately.
5. When both innings are done, the higher total wins; scorecards for both teams are printed.

## Compile & Run

```bash
cd src
javac Main.java && java Main
# or, on Java 11+:
java Main.java
```

## Sample Output

```
INNING 1 -- total Run: 139
---Batting ScoreCard : India---
PlayerName: India1 -- totalRuns: 25 -- totalBallsPlayed: 8 -- 4s: 2 -- 6s: 2 -- outby: SriLanka11
...
---Bowling ScoreCard : SriLanka---
PlayerName: SriLanka8 -- totalOversThrown: 2 -- totalRunsGiven: 44 -- WicketsTaken: 1
...
---WINNER--- India
```

## Simplifications (intentional)

- `BallType` is always `NORMAL` — the code has a slot to randomize wide/no-ball, but the guard is left in for a future extension.
- Every dismissal is treated as `BOLD` by the current bowler. `WicketType` supports `RUNOUT` / `CATCH` when needed.
- Runs are randomised via `Math.random()`; swap the RNG for a deterministic seed if you want repeatable simulations.
