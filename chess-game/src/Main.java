/* CHESS ENGINE - minimal single file.   javac Main.java && java Main
 *
 * PIECE NAMES - friendly name used in this code, and the real chess name:
 *
 *   King     (Raja)      K   moves 1 square in any direction
 *   Queen    (Rani)      Q   any distance, straight or diagonal
 *   Elephant (= Rook)    E   any distance, straight only
 *   Camel    (= Bishop)  C   any distance, diagonal only
 *   Horse    (= Knight)  H   "L" shape, and it JUMPS over pieces
 *   Soldier  (= Pawn)    S   1 forward (2 on its first move), kills diagonally
 *
 * Board: row 0 = rank 8 (Black), row 7 = rank 1 (White). So "e2" = row 6, col 4,
 * and White soldiers move from a high row to a low row.
 *
 * Covers: all 6 pieces, blocked paths, turn order, captures, self-check/pins,
 * check, checkmate, stalemate, soldier promotion, undo.
 * Not covered: castling, en passant, threefold repetition.
 */

import java.util.ArrayDeque;
import java.util.Deque;

enum Color {
    WHITE, BLACK
}

/* ---------- PIECES: each subclass owns only its own movement rule ---------- */

abstract class Piece {

    protected Color color;
    protected char displayLetter;                // K Q E C H S

    public Piece(Color color, char displayLetter) {
        this.color = color;
        this.displayLetter = displayLetter;
    }

    /** Shape + path rules only. Check and pins are the game's job, not the piece's. */
    public abstract boolean canMove(int fromRow, int fromCol, int toRow, int toCol,
                                    Piece[][] squares);

    /**
     * Instead of type-checking a piece from the outside, we ask the piece itself.
     * Only the King class overrides this to return true, so no class anywhere in
     * this file has to know the list of piece types.
     */
    public boolean isKing() {
        return false;
    }

    /** Same idea: only the Soldier can be promoted on reaching the far end. */
    public boolean canBePromoted() {
        return false;
    }

    /** The target square must be empty, or hold an enemy piece we can kill. */
    public boolean canLandOn(Piece[][] squares, int toRow, int toCol) {
        Piece target = squares[toRow][toCol];
        return target == null || target.getColor() != this.color;
    }

    /**
     * WHAT THIS DOES: the Elephant, Camel and Queen slide across the board, so
     * they cannot pass THROUGH another piece. This walks the squares in between
     * and returns false if any of them is occupied.
     *
     * It does NOT look at the start square (our own piece is there) or at the
     * target square (canLandOn handles that one).
     *
     * EXAMPLE - Elephant on a1 wants to go to a5:
     *   from = row 7, col 0     to = row 3, col 0
     *   rowStep = -1 (go up), colStep = 0 (stay in the same column)
     *   so it checks a2, a3, a4 and stops before reaching a5.
     */
    public static boolean isPathClear(Piece[][] squares, int fromRow, int fromCol,
                                      int toRow, int toCol) {
        // Which direction do we walk in? One step at a time: -1, 0 or +1.
        int rowStep = 0;
        if (toRow > fromRow) {
            rowStep = 1;             // walking down the board
        } else if (toRow < fromRow) {
            rowStep = -1;            // walking up the board
        }

        int colStep = 0;
        if (toCol > fromCol) {
            colStep = 1;             // walking right
        } else if (toCol < fromCol) {
            colStep = -1;            // walking left
        }

        // Take the first step, so we skip the square we are standing on.
        int row = fromRow + rowStep;
        int col = fromCol + colStep;

        // Keep walking until we arrive at the target square.
        while (row != toRow || col != toCol) {
            if (squares[row][col] != null) {
                return false;        // a piece is standing in the way
            }
            row = row + rowStep;
            col = col + colStep;
        }

        return true;                 // nothing in between
    }

    public Color getColor() {
        return color;
    }

    /** "WHITE Horse" - used in the move log and in error messages. */
    public String getFullName() {
        return color + " " + getClass().getSimpleName();
    }

    /** Uppercase letter for White, lowercase for Black. */
    public char getBoardLetter() {
        return color == Color.WHITE ? displayLetter : Character.toLowerCase(displayLetter);
    }
}

/** Rook. Straight lines only, any distance, cannot pass through anything. */
class Elephant extends Piece {

    public Elephant(Color color) {
        super(color, 'E');
    }

    @Override
    public boolean canMove(int fromRow, int fromCol, int toRow, int toCol, Piece[][] squares) {
        boolean movesStraight = (fromRow == toRow || fromCol == toCol);
        return movesStraight
                && isPathClear(squares, fromRow, fromCol, toRow, toCol)
                && canLandOn(squares, toRow, toCol);
    }
}

/** Bishop. Diagonals only - the row distance must equal the column distance. */
class Camel extends Piece {

    public Camel(Color color) {
        super(color, 'C');
    }

    @Override
    public boolean canMove(int fromRow, int fromCol, int toRow, int toCol, Piece[][] squares) {
        int rowDistance = Math.abs(fromRow - toRow);
        int colDistance = Math.abs(fromCol - toCol);
        boolean movesDiagonally = (rowDistance == colDistance);
        return movesDiagonally
                && isPathClear(squares, fromRow, fromCol, toRow, toCol)
                && canLandOn(squares, toRow, toCol);
    }
}

/** Elephant + Camel combined: the strongest piece on the board. */
class Queen extends Piece {

    public Queen(Color color) {
        super(color, 'Q');
    }

    @Override
    public boolean canMove(int fromRow, int fromCol, int toRow, int toCol, Piece[][] squares) {
        boolean movesStraight = (fromRow == toRow || fromCol == toCol);
        boolean movesDiagonally = Math.abs(fromRow - toRow) == Math.abs(fromCol - toCol);
        return (movesStraight || movesDiagonally)
                && isPathClear(squares, fromRow, fromCol, toRow, toCol)
                && canLandOn(squares, toRow, toCol);
    }
}

/** Knight. 2 squares one way plus 1 the other. The only piece that jumps. */
class Horse extends Piece {

    public Horse(Color color) {
        super(color, 'H');
    }

    @Override
    public boolean canMove(int fromRow, int fromCol, int toRow, int toCol, Piece[][] squares) {
        int rowDistance = Math.abs(fromRow - toRow);
        int colDistance = Math.abs(fromCol - toCol);
        boolean movesInLShape = (rowDistance == 2 && colDistance == 1)
                || (rowDistance == 1 && colDistance == 2);
        // No isPathClear call here on purpose: the Horse jumps over everything.
        return movesInLShape && canLandOn(squares, toRow, toCol);
    }
}

/** One square in any direction. Trapping him ends the game. */
class King extends Piece {

    public King(Color color) {
        super(color, 'K');
    }

    @Override
    public boolean canMove(int fromRow, int fromCol, int toRow, int toCol, Piece[][] squares) {
        int rowDistance = Math.abs(fromRow - toRow);
        int colDistance = Math.abs(fromCol - toCol);
        boolean movesOneSquare = (rowDistance <= 1 && colDistance <= 1);
        return movesOneSquare && canLandOn(squares, toRow, toCol);
    }

    @Override
    public boolean isKing() {
        return true;
    }
}

/** Pawn. The only piece that moves one way but kills another way. */
class Soldier extends Piece {

    public Soldier(Color color) {
        super(color, 'S');
    }

    @Override
    public boolean canMove(int fromRow, int fromCol, int toRow, int toCol, Piece[][] squares) {
        int forwardDirection = (color == Color.WHITE) ? -1 : 1;   // White walks toward row 0
        int startingRow = (color == Color.WHITE) ? 6 : 1;
        int rowChange = toRow - fromRow;
        int colChange = toCol - fromCol;
        Piece target = squares[toRow][toCol];

        boolean oneStepForward = (colChange == 0)
                && (rowChange == forwardDirection)
                && (target == null);

        boolean twoStepsForward = (colChange == 0)
                && (rowChange == 2 * forwardDirection)
                && (fromRow == startingRow)
                && (target == null)
                && (squares[fromRow + forwardDirection][fromCol] == null);

        boolean diagonalKill = (Math.abs(colChange) == 1)
                && (rowChange == forwardDirection)
                && (target != null)
                && (target.getColor() != color);

        return oneStepForward || twoStepsForward || diagonalKill;
    }

    @Override
    public boolean canBePromoted() {
        return true;
    }
}

/* ---------- BOARD: the grid, plus the "is my King attacked?" question ---------- */

class Board {

    public static int SIZE = 8;                  // 8 x 8 grid

    private Piece[][] squares = new Piece[SIZE][SIZE];

    public Board() {
        placeBackRow(0, Color.BLACK);
        placeBackRow(7, Color.WHITE);
        for (int col = 0; col < SIZE; col++) {
            squares[1][col] = new Soldier(Color.BLACK);
            squares[6][col] = new Soldier(Color.WHITE);
        }
    }

    /** Elephant, Horse, Camel, Queen, King, Camel, Horse, Elephant. */
    private void placeBackRow(int row, Color color) {
        squares[row][0] = new Elephant(color);
        squares[row][1] = new Horse(color);
        squares[row][2] = new Camel(color);
        squares[row][3] = new Queen(color);
        squares[row][4] = new King(color);
        squares[row][5] = new Camel(color);
        squares[row][6] = new Horse(color);
        squares[row][7] = new Elephant(color);
    }

    public Piece[][] getSquares() {
        return squares;
    }

    public Piece getPieceAt(int row, int col) {
        return squares[row][col];
    }

    public void setPieceAt(int row, int col, Piece piece) {
        squares[row][col] = piece;
    }

    /**
     * Is this colour's King attacked right now? We reuse each piece's own
     * canMove() rule, so there is no separate "attack table" to keep in sync.
     */
    public boolean isKingInCheck(Color color) {
        int kingRow = -1;
        int kingCol = -1;
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                Piece piece = squares[row][col];
                if (piece != null && piece.isKing() && piece.getColor() == color) {
                    kingRow = row;
                    kingCol = col;
                }
            }
        }
        if (kingRow == -1) {
            return false;
        }

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                Piece enemy = squares[row][col];
                if (enemy != null && enemy.getColor() != color
                        && enemy.canMove(row, col, kingRow, kingCol, squares)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** row 6, col 4 -> "e2" */
    public static String getSquareName(int row, int col) {
        return "" + (char) ('a' + col) + (SIZE - row);
    }

    public static boolean isInsideBoard(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    /** Builds one text line per row, then prints it. */
    public void print() {
        String columnHeader = "     a  b  c  d  e  f  g  h";

        System.out.println();
        System.out.println(columnHeader);

        for (int row = 0; row < SIZE; row++) {
            // We print (8 - row), not row, because array row 0 is the top of the
            // board and chess calls that rank 8. Without it, "e2" in the demo
            // would not line up with anything you can see on this printout.
            String line = "  " + (SIZE - row) + " ";

            for (int col = 0; col < SIZE; col++) {
                Piece piece = squares[row][col];
                if (piece == null) {
                    line = line + " . ";
                } else {
                    line = line + " " + piece.getBoardLetter() + " ";
                }
            }

            System.out.println(line);
        }

        System.out.println(columnHeader);
        System.out.println("     CAPS = White.  K ing  Q ueen  E lephant  C amel  H orse  S oldier");
        System.out.println();
    }
}

/* ---------- MOVE: remembers the killed piece, so it can undo itself ---------- */

class Move {

    private int fromRow;
    private int fromCol;
    private int toRow;
    private int toCol;
    private Piece movedPiece;
    private Piece killedPiece;

    public Move(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.movedPiece = board.getPieceAt(fromRow, fromCol);
        this.killedPiece = board.getPieceAt(toRow, toCol);
    }

    public void execute(Board board) {
        board.setPieceAt(toRow, toCol, movedPiece);
        board.setPieceAt(fromRow, fromCol, null);
    }

    public void undo(Board board) {
        board.setPieceAt(fromRow, fromCol, movedPiece);
        board.setPieceAt(toRow, toCol, killedPiece);
    }

    public Piece getMovedPiece() {
        return movedPiece;
    }

    public int getToRow() {
        return toRow;
    }

    public int getToCol() {
        return toCol;
    }

    @Override
    public String toString() {
        String killText = (killedPiece == null) ? "" : " kills " + killedPiece.getFullName();
        return movedPiece.getFullName() + " "
                + Board.getSquareName(fromRow, fromCol) + "-"
                + Board.getSquareName(toRow, toCol) + killText;
    }
}

/* ---------- GAME: turns, validation, check / checkmate, undo ---------- */

class ChessGame {

    private Board board = new Board();
    private Deque<Move> moveHistory = new ArrayDeque<>();
    private Color currentTurn = Color.WHITE;
    private boolean isGameOver = false;

    public Board getBoard() {
        return board;
    }

    public boolean makeMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (isGameOver) {
            System.out.println("X    the game is already over");
            return false;
        }
        if (!board.isInsideBoard(fromRow, fromCol) || !board.isInsideBoard(toRow, toCol)) {
            System.out.println("X    those row/col numbers are off the board (use 0 to 7)");
            return false;
        }
        if (fromRow == toRow && fromCol == toCol) {
            System.out.println("X    source and target are the same square");
            return false;
        }

        Piece movingPiece = board.getPieceAt(fromRow, fromCol);
        if (movingPiece == null) {
            System.out.println("X    no piece on row " + fromRow + ", col " + fromCol);
            return false;
        }
        if (movingPiece.getColor() != currentTurn) {
            System.out.println("X    it is " + currentTurn + "'s turn");
            return false;
        }
        if (!movingPiece.canMove(fromRow, fromCol, toRow, toCol, board.getSquares())) {
            System.out.println("X    " + movingPiece.getFullName()
                    + " cannot move there (bad pattern, blocked path, or own piece)");
            return false;
        }

        // Play the move for real, then ask the board. Roll it back if it was suicide.
        Move move = new Move(board, fromRow, fromCol, toRow, toCol);
        move.execute(board);
        if (board.isKingInCheck(currentTurn)) {
            move.undo(board);
            System.out.println("X    that move leaves your own King in check (piece is pinned)");
            return false;
        }

        promoteIfNeeded(move);
        moveHistory.push(move);
        System.out.println("OK   " + move);

        announceResult();
        switchTurn();
        return true;
    }

    /** Hands the turn over to the other player. */
    private void switchTurn() {
        if (currentTurn == Color.WHITE) {
            currentTurn = Color.BLACK;
        } else {
            currentTurn = Color.WHITE;
        }
    }

    /** A Soldier reaching the far end becomes a Queen. */
    private void promoteIfNeeded(Move move) {
        Piece piece = move.getMovedPiece();
        boolean reachedFarEnd = (move.getToRow() == 0) || (move.getToRow() == Board.SIZE - 1);
        if (piece.canBePromoted() && reachedFarEnd) {
            board.setPieceAt(move.getToRow(), move.getToCol(), new Queen(piece.getColor()));
            System.out.println("     " + piece.getFullName() + " is promoted to a Queen");
        }
    }

    /**
     * Called while currentTurn is STILL the player who just moved, so the person
     * who has to answer that move is the other player - called "defender" here.
     *
     *   defender has no legal reply + his King is attacked      = CHECKMATE
     *   defender has no legal reply + his King is not attacked  = STALEMATE (draw)
     */
    private void announceResult() {
        Color defender;
        if (currentTurn == Color.WHITE) {
            defender = Color.BLACK;
        } else {
            defender = Color.WHITE;
        }

        boolean inCheck = board.isKingInCheck(defender);
        if (!hasAnyLegalMove(defender)) {
            isGameOver = true;
            if (inCheck) {
                System.out.println(">>>  CHECKMATE - " + currentTurn + " wins");
            } else {
                System.out.println(">>>  STALEMATE - the game is a draw");
            }
        } else if (inCheck) {
            System.out.println("!    CHECK - the " + defender + " King is under attack");
        }
    }

    /**
     * QUESTION THIS ANSWERS: "does this player have even ONE move he is allowed
     * to play?" We need it after every move to know if the game has ended:
     *
     *      no move possible  +  King is attacked      ->  CHECKMATE
     *      no move possible  +  King is NOT attacked   ->  STALEMATE (a draw)
     *      at least one move possible                  ->  the game continues
     *
     * HOW WE FIND OUT: brute force. We try moving every piece of this colour to
     * every square on the board, and for each attempt we:
     *
     *      1. skip it if that piece's own rule says it cannot move like that
     *      2. actually play it on the board
     *      3. ask "is my own King attacked now?"
     *      4. undo it, leaving the board exactly as it was
     *
     * Step 3 is the whole point. A move can look perfectly legal and still be
     * forbidden, because you may never leave your own King attacked. That is how
     * a pinned piece gets frozen, and how we know a King has truly run out of
     * escape squares.
     *
     * As soon as we find one move that keeps the King safe we stop and return
     * true. We do not care WHICH move it was, only that one exists.
     */
    private boolean hasAnyLegalMove(Color color) {

        // Loops 1 and 2: pick up each of this player's pieces, one at a time.
        for (int fromRow = 0; fromRow < Board.SIZE; fromRow++) {
            for (int fromCol = 0; fromCol < Board.SIZE; fromCol++) {

                Piece piece = board.getPieceAt(fromRow, fromCol);
                if (piece == null || piece.getColor() != color) {
                    continue;                 // empty square, or the enemy's piece
                }

                // Loops 3 and 4: try to send that piece to every square.
                for (int toRow = 0; toRow < Board.SIZE; toRow++) {
                    for (int toCol = 0; toCol < Board.SIZE; toCol++) {

                        if (!piece.canMove(fromRow, fromCol, toRow, toCol, board.getSquares())) {
                            continue;         // the piece does not move like that
                        }

                        // Play it, look at the King, then put everything back.
                        Move testMove = new Move(board, fromRow, fromCol, toRow, toCol);
                        testMove.execute(board);
                        boolean kingIsSafe = !board.isKingInCheck(color);
                        testMove.undo(board);

                        if (kingIsSafe) {
                            return true;      // one legal move is enough
                        }
                    }
                }
            }
        }

        return false;                         // nothing at all is playable
    }

    public void undoLastMove() {
        if (moveHistory.isEmpty()) {
            System.out.println("X    nothing to undo");
            return;
        }
        Move lastMove = moveHistory.pop();
        lastMove.undo(board);
        switchTurn();
        isGameOver = false;
        System.out.println("UNDO " + lastMove + ", " + currentTurn + " to move again");
    }

}

public class Main {

    /* makeMove(fromRow, fromCol, toRow, toCol) - all numbers are 0 to 7.
     *
     *   col  0  1  2  3  4  5  6  7
     *        a  b  c  d  e  f  g  h
     *   row 0  =  rank 8  (Black back row)
     *   row 1  =  rank 7  (Black soldiers)
     *   row 6  =  rank 2  (White soldiers)
     *   row 7  =  rank 1  (White back row)
     *
     * So the square "e2" is row 6, col 4.
     */
    public static void main(String[] args) {
        ChessGame game = new ChessGame();
        game.getBoard().print();

        System.out.println("-- moves the engine must refuse --");
        game.makeMove(6, 4, 3, 4);   // e2 to e5: a Soldier cannot jump three squares
        game.makeMove(7, 0, 3, 0);   // a1 to a5: Elephant blocked by its own Soldier on a2
        game.makeMove(7, 4, 5, 4);   // e1 to e3: the King moves only one square
        game.makeMove(7, 1, 5, 1);   // b1 to b3: the Horse moves in an L, never straight
        game.makeMove(0, 3, 4, 7);   // d8 to h4: a Black piece on White's turn
        game.makeMove(9, 9, 0, 0);   // off the board entirely

        System.out.println("\n-- Fool's mate: the fastest checkmate in chess --");
        game.makeMove(6, 5, 5, 5);   // f2 to f3: White opens a diagonal to its own King
        game.makeMove(1, 4, 3, 4);   // e7 to e5: Black frees its Queen
        game.makeMove(6, 6, 4, 6);   // g2 to g4: White blunders again
        game.makeMove(0, 3, 4, 7);   // d8 to h4: no escape, no block, no kill
        game.getBoard().print();

        System.out.println("-- undo the mate, then play it again --");
        game.undoLastMove();
        game.makeMove(0, 3, 4, 7);   // d8 to h4 once more
    }
}
