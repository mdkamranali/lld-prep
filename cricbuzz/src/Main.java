/*
 * LLD Cricbuzz - Single-file consolidation of a low-level design for a
 * live-scoring cricket application (Cricbuzz-style).
 *
 * All entities, controllers, observers, patterns and the demo runner are
 * packed into this one file for easy review / interview practice.
 *
 * Compile & run:
 *     javac Main.java
 *     java Main
 * (or, on Java 11+: `java Main.java`)
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/* ==========================================================================
 *                                 ENUMS
 * ========================================================================== */

enum RunType {
    ZERO, ONE, TWO, THREE, FOUR, SIX
}

enum BallType {
    NORMAL, WIDEBALL, NOBALL
}

enum WicketType {
    RUNOUT, BOLD, CATCH
}

enum PlayerType {
    BATSMAN, BOWLER, WICKETKEEPER, CAPTAIN, ALLROUNDER
}

/* ==========================================================================
 *                              MATCH TYPES
 *   Strategy pattern - each match format defines its own rules.
 * ========================================================================== */

interface MatchType {
    int noOfOvers();
    int maxOverCountBowlers();
}

class T20MatchType implements MatchType {
    @Override public int noOfOvers()          { return 20; }
    @Override public int maxOverCountBowlers(){ return 4;  }
}

class OneDayMatchType implements MatchType {
    @Override public int noOfOvers()          { return 50; }
    @Override public int maxOverCountBowlers(){ return 10; }
}

/* ==========================================================================
 *                                PLAYER
 *   Single class describing a person + their cricket role + their scorecards.
 *   (Person has been merged in - name/age/address live here directly.)
 * ========================================================================== */

class BattingScoreCard {
    public int    totalRuns;
    public int    totalBallsPlayed;
    public int    totalFours;
    public int    totalSix;
    public double strikeRate;
}

class BowlingScoreCard {
    public int    totalOversCount;
    public int    runsGiven;
    public int    wicketsTaken;
    public int    noBallCount;
    public int    wideBallCount;
    public double economyRate;
}

class Player {
    // person info
    public String name;
    public int    age;
    public String address;

    // cricket info
    public PlayerType       playerType;
    public BattingScoreCard battingScoreCard;
    public BowlingScoreCard bowlingScoreCard;

    // if dismissed, the bowler who took the wicket (null => not out)
    public Player outBy;

    public Player(String name, PlayerType playerType) {
        this.name             = name;
        this.playerType       = playerType;
        this.battingScoreCard = new BattingScoreCard();
        this.bowlingScoreCard = new BowlingScoreCard();
    }

    public void printBattingScoreCard() {
        System.out.println("PlayerName: " + name
                + " -- totalRuns: "        + battingScoreCard.totalRuns
                + " -- totalBallsPlayed: " + battingScoreCard.totalBallsPlayed
                + " -- 4s: "               + battingScoreCard.totalFours
                + " -- 6s: "               + battingScoreCard.totalSix
                + " -- outby: " + (outBy != null ? outBy.name : "notout"));
    }

    public void printBowlingScoreCard() {
        System.out.println("PlayerName: " + name
                + " -- totalOversThrown: " + bowlingScoreCard.totalOversCount
                + " -- totalRunsGiven: "   + bowlingScoreCard.runsGiven
                + " -- WicketsTaken: "     + bowlingScoreCard.wicketsTaken);
    }
}

/* ==========================================================================
 *                       BATTING / BOWLING CONTROLLERS
 *   Encapsulate who bats next and which bowler gets the next over.
 * ========================================================================== */

class PlayerBattingController {
    Queue<Player> yetToPlay;
    Player        striker;
    Player        nonStriker;

    public PlayerBattingController(Queue<Player> playing11) {
        this.yetToPlay = new LinkedList<>();
        this.yetToPlay.addAll(playing11);
    }

    public void getNextPlayer() throws Exception {
        if (yetToPlay.isEmpty()) {
            throw new Exception("No more batsmen left");
        }
       if (this.striker == null)    this.striker    = yetToPlay.poll();
       if (this.nonStriker == null) this.nonStriker = yetToPlay.poll();
    }

    public Player getStriker()                { return striker; }
    public Player getNonStriker()             { return nonStriker; }
    public void   setStriker(Player p)        { striker = p; }
    public void   setNonStriker(Player p)     { nonStriker = p; }
}

class PlayerBowlingController {
    Deque<Player>             bowlersList;
    Map<Player, Integer>      bowlerVsOverCount;
    Player                    currentBowler;

    public PlayerBowlingController(List<Player> bowlers) {
        setBowlersList(bowlers);
    }

    private void setBowlersList(List<Player> bowlers) {
        this.bowlersList       = new LinkedList<>();
        this.bowlerVsOverCount = new HashMap<>();
        for (Player bowler : bowlers) {
            this.bowlersList.addLast(bowler);
            this.bowlerVsOverCount.put(bowler, 0);
        }
    }

    public void getNextBowler(int maxOverCountPerBowler) {
        Player p = bowlersList.poll();
        if (bowlerVsOverCount.get(p) + 1 == maxOverCountPerBowler) {
            currentBowler = p;
        } else {
            currentBowler = p;
            bowlersList.addLast(p);
            bowlerVsOverCount.put(p, bowlerVsOverCount.get(p) + 1);
        }
    }

    public Player getCurrentBowler() { return currentBowler; }
}

/* ==========================================================================
 *                                TEAM
 * ========================================================================== */

class Team {
    public String                  teamName;
    public Queue<Player>           playing11;
    public List<Player>            bench;
    public PlayerBattingController battingController;
    public PlayerBowlingController bowlingController;
    public boolean                 isWinner;

    public Team(String teamName,
                Queue<Player> playing11,
                List<Player> bench,
                List<Player> bowlers) {
        this.teamName          = teamName;
        this.playing11         = playing11;
        this.bench             = bench;
        this.battingController = new PlayerBattingController(playing11);
        this.bowlingController = new PlayerBowlingController(bowlers);
    }

    public String getTeamName() { return teamName; }

    public void chooseNextBatsMan() throws Exception {
        battingController.getNextPlayer();
    }

    public void chooseNextBowler(int maxOverCountPerBowler) {
        bowlingController.getNextBowler(maxOverCountPerBowler);
    }

    public Player getStriker()               { return battingController.getStriker(); }
    public Player getNonStriker()            { return battingController.getNonStriker(); }
    public void   setStriker(Player p)       { battingController.setStriker(p); }
    public void   setNonStriker(Player p)    { battingController.setNonStriker(p); }
    public Player getCurrentBowler()         { return bowlingController.getCurrentBowler(); }

    public void printBattingScoreCard() {
        for (Player p : playing11) {
            p.printBattingScoreCard();
        }
    }

    public void printBowlingScoreCard() {
        for (Player p : playing11) {
            if (p.bowlingScoreCard.totalOversCount > 0) {
                p.printBowlingScoreCard();
            }
        }
    }

    public int getTotalRuns() {
        int total = 0;
        for (Player p : playing11) {
            total += p.battingScoreCard.totalRuns;
        }
        return total;
    }
}

/* ==========================================================================
 *                    SCORE-UPDATER OBSERVERS (Observer Pattern)
 *   Every ball notifies both a batting and a bowling scoreboard observer.
 * ========================================================================== */

interface ScoreUpdaterObserver {
    void update(BallDetails ballDetails);
}

class BattingScoreUpdater implements ScoreUpdaterObserver {
    @Override
    public void update(BallDetails ballDetails) {
        int run = 0;
        if (RunType.ONE == ballDetails.runType) {
            run = 1;
        } else if (RunType.TWO == ballDetails.runType) {
            run = 2;
        } else if (RunType.FOUR == ballDetails.runType) {
            run = 4;
            ballDetails.playedBy.battingScoreCard.totalFours++;
        } else if (RunType.SIX == ballDetails.runType) {
            run = 6;
            ballDetails.playedBy.battingScoreCard.totalSix++;
        }
        ballDetails.playedBy.battingScoreCard.totalRuns        += run;
        ballDetails.playedBy.battingScoreCard.totalBallsPlayed++;

        // Record who dismissed the batsman (if any) directly on the Player.
        if (ballDetails.wicketType != null) {
            ballDetails.playedBy.outBy = ballDetails.bowledBy;
        }
    }
}

class BowlingScoreUpdater implements ScoreUpdaterObserver {
    @Override
    public void update(BallDetails ballDetails) {
        if (ballDetails.ballNumber == 6 && ballDetails.ballType == BallType.NORMAL) {
            ballDetails.bowledBy.bowlingScoreCard.totalOversCount++;
        }

        if (RunType.ONE == ballDetails.runType) {
            ballDetails.bowledBy.bowlingScoreCard.runsGiven += 1;
        } else if (RunType.TWO == ballDetails.runType) {
            ballDetails.bowledBy.bowlingScoreCard.runsGiven += 2;
        } else if (RunType.FOUR == ballDetails.runType) {
            ballDetails.bowledBy.bowlingScoreCard.runsGiven += 4;
        } else if (RunType.SIX == ballDetails.runType) {
            ballDetails.bowledBy.bowlingScoreCard.runsGiven += 6;
        }

        if (ballDetails.wicketType != null) {
            ballDetails.bowledBy.bowlingScoreCard.wicketsTaken++;
        }
        if (ballDetails.ballType == BallType.NOBALL) {
            ballDetails.bowledBy.bowlingScoreCard.noBallCount++;
        }
        if (ballDetails.ballType == BallType.WIDEBALL) {
            ballDetails.bowledBy.bowlingScoreCard.wideBallCount++;
        }
    }
}

/* ==========================================================================
 *                     BALL / OVER / INNING (core simulation)
 * ========================================================================== */

class BallDetails {
    public int                       ballNumber;
    public BallType                  ballType;
    public RunType                   runType;
    public Player                    playedBy;
    public Player                    bowledBy;
    public WicketType                wicketType; // null => no wicket on this ball
    List<ScoreUpdaterObserver>       scoreUpdaterObserverList = new ArrayList<>();

    public BallDetails(int ballNumber) {
        this.ballNumber = ballNumber;
        scoreUpdaterObserverList.add(new BowlingScoreUpdater());
        scoreUpdaterObserverList.add(new BattingScoreUpdater());
    }

    public void startBallDelivery(Team battingTeam, Team bowlingTeam, OverDetails over) {
        playedBy      = battingTeam.getStriker();
        this.bowledBy = over.bowledBy;

        // For now every ball is NORMAL - a real system would randomize wide/no-ball too.
        ballType = BallType.NORMAL;

        if (isWicketTaken()) {
            runType    = RunType.ZERO;
            // Simplification: every dismissal is BOLD by the current bowler.
            wicketType = WicketType.BOLD;
            battingTeam.setStriker(null);
        } else {
            runType = getRunType();
            if (runType == RunType.ONE || runType == RunType.THREE) {
                // Odd runs -> striker / non-striker swap.
                Player temp = battingTeam.getStriker();
                battingTeam.setStriker(battingTeam.getNonStriker());
                battingTeam.setNonStriker(temp);
            }
        }
        notifyUpdaters(this);
    }

    private void notifyUpdaters(BallDetails ballDetails) {
        for (ScoreUpdaterObserver observer : scoreUpdaterObserverList) {
            observer.update(ballDetails);
        }
    }

    private RunType getRunType() {
        double v = Math.random();
        if (v <= 0.2)                  return RunType.ONE;
        else if (v >= 0.3 && v <= 0.5) return RunType.TWO;
        else if (v >= 0.6 && v <= 0.8) return RunType.FOUR;
        else                           return RunType.SIX;
    }

    private boolean isWicketTaken() {
        return Math.random() < 0.2;
    }
}

class OverDetails {
    int               overNumber;
    List<BallDetails> balls;
    Player            bowledBy;

    OverDetails(int overNumber, Player bowledBy) {
        this.overNumber = overNumber;
        this.balls      = new ArrayList<>();
        this.bowledBy   = bowledBy;
    }

    public boolean startOver(Team battingTeam, Team bowlingTeam, int runsToWin) throws Exception {
        int ballCount = 1;
        while (ballCount <= 6) {
            BallDetails ball = new BallDetails(ballCount);
            ball.startBallDelivery(battingTeam, bowlingTeam, this);

            // Only NORMAL deliveries count toward the 6 legal balls of the over.
            // A wide / no-ball would just loop without incrementing ballCount (re-bowl).
            if (ball.ballType == BallType.NORMAL) {
                balls.add(ball);
                ballCount++;
                if (ball.wicketType != null) {
                    battingTeam.chooseNextBatsMan();
                }
                if (runsToWin != -1 && battingTeam.getTotalRuns() >= runsToWin) {
                    battingTeam.isWinner = true;
                    return true;
                }
            }
        }
        return false;
    }
}

class InningDetails {
    Team              battingTeam;
    Team              bowlingTeam;
    MatchType         matchType;
    List<OverDetails> overs;

    public InningDetails(Team battingTeam, Team bowlingTeam, MatchType matchType) {
        this.battingTeam = battingTeam;
        this.bowlingTeam = bowlingTeam;
        this.matchType   = matchType;
        this.overs       = new ArrayList<>();
    }

    public void start(int runsToWin) {
        try {
            battingTeam.chooseNextBatsMan();
        } catch (Exception e) {
            // no players left, nothing to do
        }

        int noOfOvers = matchType.noOfOvers();
        for (int overNumber = 1; overNumber <= noOfOvers; overNumber++) {
            bowlingTeam.chooseNextBowler(matchType.maxOverCountBowlers());

            OverDetails over = new OverDetails(overNumber, bowlingTeam.getCurrentBowler());
            overs.add(over);
            try {
                boolean won = over.startOver(battingTeam, bowlingTeam, runsToWin);
                if (won) break;
            } catch (Exception e) {
                break; // all batsmen out
            }

            // swap strike between overs
            Player temp = battingTeam.getStriker();
            battingTeam.setStriker(battingTeam.getNonStriker());
            battingTeam.setNonStriker(temp);
        }
    }

    public int getTotalRuns() {
        return battingTeam.getTotalRuns();
    }
}

/* ==========================================================================
 *                                 MATCH
 * ========================================================================== */

class Match {
    Team            teamA;
    Team            teamB;
    Date            matchDate;
    String          venue;
    Team            tossWinner;
    InningDetails[] innings;
    MatchType       matchType;

    public Match(Team teamA, Team teamB, Date matchDate, String venue, MatchType matchType) {
        this.teamA     = teamA;
        this.teamB     = teamB;
        this.matchDate = matchDate;
        this.venue     = venue;
        this.matchType = matchType;
        this.innings   = new InningDetails[2];
    }

    public void startMatch() {
        // 1. Toss
        tossWinner = toss(teamA, teamB);

        // 2. Two innings, toss-winner bats first.
        for (int inning = 1; inning <= 2; inning++) {
            InningDetails inningDetails;
            Team          bowlingTeam;
            Team          battingTeam;

            if (inning == 1) {
                battingTeam   = tossWinner;
                bowlingTeam   = tossWinner.getTeamName().equals(teamA.getTeamName()) ? teamB : teamA;
                inningDetails = new InningDetails(battingTeam, bowlingTeam, matchType);
                inningDetails.start(-1);
            } else {
                bowlingTeam   = tossWinner;
                battingTeam   = tossWinner.getTeamName().equals(teamA.getTeamName()) ? teamB : teamA;
                inningDetails = new InningDetails(battingTeam, bowlingTeam, matchType);
                inningDetails.start(innings[0].getTotalRuns());
                if (bowlingTeam.getTotalRuns() > battingTeam.getTotalRuns()) {
                    bowlingTeam.isWinner = true;
                }
            }
            innings[inning - 1] = inningDetails;

            System.out.println();
            System.out.println("INNING " + inning + " -- total Run: " + battingTeam.getTotalRuns());
            System.out.println("---Batting ScoreCard : " + battingTeam.teamName + "---");
            battingTeam.printBattingScoreCard();

            System.out.println();
            System.out.println("---Bowling ScoreCard : " + bowlingTeam.teamName + "---");
            bowlingTeam.printBowlingScoreCard();
        }

        System.out.println();
        if (teamA.isWinner)      System.out.println("---WINNER--- " + teamA.teamName);
        else if (teamB.isWinner) System.out.println("---WINNER--- " + teamB.teamName);
        else                     System.out.println("---MATCH TIED---");
    }

    private Team toss(Team a, Team b) {
        return Math.random() < 0.5 ? a : b;
    }
}

/* ==========================================================================
 *                              DEMO / DRIVER
 * ========================================================================== */

public class Main {

    public static void main(String[] args) {
        Main app = new Main();

        Team teamA = app.addTeam("India");
        Team teamB = app.addTeam("SriLanka");

        MatchType matchType = new T20MatchType();
        Match match = new Match(teamA, teamB, null, "SMS STADIUM", matchType);
        match.startMatch();
    }

    private Team addTeam(String name) {
        Queue<Player> playing11 = new LinkedList<>();
        List<Player>  bowlers   = new ArrayList<>();

        for (int i = 1; i <= 11; i++) {
            Player p = new Player(name + i, PlayerType.ALLROUNDER);
            playing11.add(p);
            if (i >= 8) bowlers.add(p); // last 4 also bowl
        }
        return new Team(name, playing11, new ArrayList<>(), bowlers);
    }
}
