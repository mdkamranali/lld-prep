import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Java doesn't have a built-in Pair like C++, so we create our own
class Pair {
    int first;
    int second;

    Pair(int first, int second) {
        this.first = first;
        this.second = second;
    }
}

class Cell {
    int start;
    int end;

    Cell(int position) {
        this.start = position;
        this.end = position;
    }
}

class Dice {
    int diceCount;
    int min = 1;
    int max = 6;

    Dice(int diceCount) {
        this.diceCount = diceCount;
    }

    int rollDice() {
        int totalSum = 0;
        int diceUsed = 0;
        while (diceUsed < diceCount) {
            // nextInt(min, max+1) generates a random number between min (inclusive) and max (inclusive)
            // e.g., nextInt(1, 7) gives a value from 1 to 6, simulating one dice roll
            totalSum += ThreadLocalRandom.current().nextInt(min, max + 1);
            diceUsed++;
        }
        return totalSum;
    }
}

class Player {
    String id;
    int currentPosition;

    Player(String id, int currentPosition) {
        this.id = id;
        this.currentPosition = currentPosition;
    }
}

class Game {

    Cell[] cells;
    Dice dice;
    Deque<Player> playersList = new LinkedList<>();
    Player winner;

    List<Pair> snakes;
    List<Pair> ladders;

    int boardSize;

    Game() {
        initializeGame();
    }

    private void initializeGame() {
        boardSize = 100;
        dice = new Dice(1);
        winner = null;

        snakes = new ArrayList<>();
        snakes.add(new Pair(17, 7));
        snakes.add(new Pair(54, 34));
        snakes.add(new Pair(62, 19));
        snakes.add(new Pair(98, 79));
        snakes.add(new Pair(64, 60));

        ladders = new ArrayList<>();
        ladders.add(new Pair(3, 22));
        ladders.add(new Pair(5, 8));
        ladders.add(new Pair(11, 26));
        ladders.add(new Pair(20, 29));

        initializeCells();
        addPlayers();
    }

    private void initializeCells() {
        cells = new Cell[boardSize];

        for (int i = 0; i < boardSize; i++) {
            cells[i] = new Cell(i);
        }

        for (Pair snake : snakes) {
            cells[snake.first].end = snake.second;
        }

        for (Pair ladder : ladders) {
            cells[ladder.first].end = ladder.second;
        }
    }

    private void addPlayers() {
        Player player1 = new Player("p1", 0);
        Player player2 = new Player("p2", 0);
        playersList.add(player1);
        playersList.add(player2);
    }

    void startGame() {
        while (winner == null) {
            Player playerTurn = findPlayerTurn();
            System.out.println("Player turn: " + playerTurn.id + " | Current position: " + playerTurn.currentPosition);

            int diceValue = dice.rollDice();
            int newPosition = playerTurn.currentPosition + diceValue;

            if (newPosition >= boardSize) {
                System.out.println("Player " + playerTurn.id + " rolled " + diceValue + " but can't move (exceeds board)");
                continue;
            }

            int finalPosition = cells[newPosition].end;
            if (finalPosition < newPosition) {
                System.out.println("Bitten by snake at " + newPosition + " -> goes to " + finalPosition);
            } else if (finalPosition > newPosition) {
                System.out.println("Climbed ladder at " + newPosition + " -> goes to " + finalPosition);
            }

            playerTurn.currentPosition = finalPosition;
            System.out.println("Player " + playerTurn.id + " | New position: " + playerTurn.currentPosition);

            if (playerTurn.currentPosition == boardSize - 1) {
                winner = playerTurn;
            }
        }
        System.out.println("WINNER IS: " + winner.id);
    }

    private Player findPlayerTurn() {
        Player playerTurn = playersList.removeFirst();
        playersList.addLast(playerTurn);
        return playerTurn;
    }
}

public class Main {
    public static void main(String[] args) {
        Game game = new Game();
        game.startGame();
    }
}
