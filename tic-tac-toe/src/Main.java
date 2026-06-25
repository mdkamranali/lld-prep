import java.util.*;

public class Main {

    enum PieceType { X, O }

    static class Player {
        String name;
        PieceType pieceType;

        Player(String name, PieceType pieceType) {
            this.name = name;
            this.pieceType = pieceType;
        }
    }

    static class Board {
        int size;
        PieceType[][] grid;

        Board(int size) {
            this.size = size;
            this.grid = new PieceType[size][size];
        }

        boolean addPiece(int row, int col, PieceType piece) {
            if (grid[row][col] != null) return false;
            grid[row][col] = piece;
            return true;
        }

        boolean hasFreeCells() {
            for (int i = 0; i < size; i++)
                for (int j = 0; j < size; j++)
                    if (grid[i][j] == null) return true;
            return false;
        }

        void print() {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    System.out.print(grid[i][j] != null ? " " + grid[i][j] + " " : "   ");
                    if (j < size - 1) System.out.print("|");
                }
                System.out.println();
                if (i < size - 1) System.out.println("-----------");
            }
        }

        boolean checkWin(int row, int col, PieceType piece) {
            boolean rowWin = true, colWin = true, diagWin = true, antiDiagWin = true;

            for (int i = 0; i < size; i++) {
                if (grid[row][i] != piece) rowWin = false;
                if (grid[i][col] != piece) colWin = false;
                if (grid[i][i] != piece) diagWin = false;
                if (grid[i][size - 1 - i] != piece) antiDiagWin = false;
            }
            return rowWin || colWin || diagWin || antiDiagWin;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Board board = new Board(3);

        Deque<Player> players = new LinkedList<>();
        players.add(new Player("Player1", PieceType.X));
        players.add(new Player("Player2", PieceType.O));

        while (true) {
            Player current = players.removeFirst();
            board.print();

            if (!board.hasFreeCells()) {
                System.out.println("Game result: Tie");
                return;
            }

            System.out.print(current.name + " (" + current.pieceType + ") - Enter row,col: ");
            String[] parts = scanner.nextLine().split(",");
            int row = Integer.parseInt(parts[0].trim());
            int col = Integer.parseInt(parts[1].trim());

            if (!board.addPiece(row, col, current.pieceType)) {
                System.out.println("Cell occupied, try again.");
                players.addFirst(current);
                continue;
            }

            if (board.checkWin(row, col, current.pieceType)) {
                board.print();
                System.out.println("Game result: " + current.name + " wins!");
                return;
            }

            players.addLast(current);
        }
    }
}
