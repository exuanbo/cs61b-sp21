package game2048;

import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/** The state of a game of 2048.
 *  @author Exuanbo
 */
public class Model extends Observable {
    /** Current contents of the board. */
    private Board board;
    /** Current score. */
    private int score;
    /** Maximum score so far.  Updated when game ends. */
    private int maxScore;
    /** True iff game is ended. */
    private boolean gameOver;

    /* Coordinate System: column C, row R of the board (where row 0,
     * column 0 is the lower-left corner of the board) will correspond
     * to board.tile(c, r).  Be careful! It works like (x, y) coordinates.
     */

    /** Largest piece value. */
    public static final int MAX_PIECE = 2048;

    /** A new 2048 game on a board of size SIZE with no pieces
     *  and score 0. */
    public Model(int size) {
        board = new Board(size);
        score = maxScore = 0;
        gameOver = false;
    }

    /** A new 2048 game where RAWVALUES contain the values of the tiles
     * (0 if null). VALUES is indexed by (row, col) with (0, 0) corresponding
     * to the bottom-left corner. Used for testing purposes. */
    public Model(int[][] rawValues, int score, int maxScore, boolean gameOver) {
        int size = rawValues.length;
        board = new Board(rawValues, score);
        this.score = score;
        this.maxScore = maxScore;
        this.gameOver = gameOver;
    }

    /** Return the current Tile at (COL, ROW), where 0 <= ROW < size(),
     *  0 <= COL < size(). Returns null if there is no tile there.
     *  Used for testing. Should be deprecated and removed.
     *  */
    public Tile tile(int col, int row) {
        return board.tile(col, row);
    }

    /** Return the number of squares on one side of the board.
     *  Used for testing. Should be deprecated and removed. */
    public int size() {
        return board.size();
    }

    /** Return true iff the game is over (there are no moves, or
     *  there is a tile with value 2048 on the board). */
    public boolean gameOver() {
        checkGameOver();
        if (gameOver) {
            maxScore = Math.max(score, maxScore);
        }
        return gameOver;
    }

    /** Return the current score. */
    public int score() {
        return score;
    }

    /** Return the current maximum game score (updated at end of game). */
    public int maxScore() {
        return maxScore;
    }

    /** Clear the board to empty and reset the score. */
    public void clear() {
        score = 0;
        gameOver = false;
        board.clear();
        setChanged();
    }

    /** Add TILE to the board. There must be no Tile currently at the
     *  same position. */
    public void addTile(Tile tile) {
        board.addTile(tile);
        checkGameOver();
        setChanged();
    }

    /** Tilt the board toward SIDE. Return true iff this changes the board.
     *
     * 1. If two Tile objects are adjacent in the direction of motion and have
     *    the same value, they are merged into one Tile of twice the original
     *    value and that new value is added to the score instance variable
     * 2. A tile that is the result of a merge will not merge again on that
     *    tilt. So each move, every tile will only ever be part of at most one
     *    merge (perhaps zero).
     * 3. When three adjacent tiles in the direction of motion have the same
     *    value, then the leading two tiles in the direction of motion merge,
     *    and the trailing tile does not.
     * */
    public boolean tilt(Side side) {
        boolean changed;
        changed = false;

        boolean isLineVertical = side == Side.NORTH || side == Side.SOUTH;
        boolean isLineIncreasing = side == Side.NORTH || side == Side.EAST;
        HashSet<Tile> changedTiles = new HashSet<>();
        int initialOuter = isLineIncreasing ? board.size() - 1 : 0;
        int outerBorder = isLineIncreasing ? -1 : board.size();
        IntFunction<Integer> nextOuter = (int outer) -> isLineIncreasing ? outer - 1 : outer + 1;
        for (int outer = initialOuter; outer != outerBorder; outer = nextOuter.apply(outer)) {
            for (int inner = 0; inner < board.size(); inner++) {
                int col = isLineVertical ? inner : outer;
                int row = isLineVertical ? outer : inner;
                Tile tile = board.tile(col, row);
                if (tile == null) {
                    continue;
                }
                boolean isChanged = moveTile(tile, isLineVertical, isLineIncreasing, changedTiles);
                if (isChanged) {
                    changed = true;
                }
            }
        }

        // for the tilt to the Side SIDE. If the board changed, set the
        // changed local variable to true.

        checkGameOver();
        if (changed) {
            setChanged();
        }
        return changed;
    }

    private boolean moveTile(Tile tile, boolean isLineVertical, boolean isLineIncreasing, Set<Tile> changedTiles) {
        int currentLine = isLineVertical ? tile.row() : tile.col();
        int farthermostLine = isLineIncreasing ? board.size() - 1 : 0;
        if (currentLine == farthermostLine) {
            return false;
        }
        int targetCol;
        int targetRow;
        Tile nearestTile = findNearestTile(tile, isLineVertical, isLineIncreasing);
        if (nearestTile == null) {
            targetCol = isLineVertical ? tile.col() : farthermostLine;
            targetRow = isLineVertical ? farthermostLine : tile.row();
        } else if (tile.value() == nearestTile.value() && !changedTiles.contains(nearestTile)) {
            targetCol = isLineVertical ? tile.col() : nearestTile.col();
            targetRow = isLineVertical ? nearestTile.row() : tile.row();
        } else if ((isLineVertical ? nearestTile.row() : nearestTile.col()) == (isLineVertical ? tile.row() : tile.col()) + (isLineIncreasing ? 1 : -1)) {
            return false;
        } else {
            targetCol = isLineVertical ? tile.col() : nearestTile.col() - (isLineIncreasing ? 1 : -1);
            targetRow = isLineVertical ? nearestTile.row() - (isLineIncreasing ? 1 : -1) : tile.row();
        }
        boolean isMerged = board.move(targetCol, targetRow, tile);
        if (isMerged) {
            score += tile.next().value();
            changedTiles.add(board.tile(targetCol, targetRow));
        }
        return true;
    }

    private Tile findNearestTile(Tile tile, boolean isLineVertical, boolean isLineIncreasing) {
        int initialLine = (isLineVertical ? tile.row() : tile.col()) + (isLineIncreasing ? 1 : -1);
        int lineBorder = isLineIncreasing ? board.size() : -1;
        IntFunction<Integer> nextLine = (int line) -> isLineIncreasing ? line + 1 : line - 1;
        for (int line = initialLine; line != lineBorder; line = nextLine.apply(line)) {
            int currentCol = isLineVertical ? tile.col() : line;
            int currentRow = isLineVertical ? line : tile.row();
            Tile currentTile = board.tile(currentCol, currentRow);
            if (currentTile != null) {
                return currentTile;
            }
        }
        return null;
    }

    /** Checks if the game is over and sets the gameOver variable
     *  appropriately.
     */
    private void checkGameOver() {
        gameOver = checkGameOver(board);
    }

    /** Determine whether game is over. */
    private static boolean checkGameOver(Board b) {
        return maxTileExists(b) || !atLeastOneMoveExists(b);
    }

    /** Returns true if at least one space on the Board is empty.
     *  Empty spaces are stored as null.
     * */
    public static boolean emptySpaceExists(Board b) {
        return TileUtils.some(Objects::isNull, b);
    }

    /**
     * Returns true if any tile is equal to the maximum valid value.
     * Maximum valid value is given by MAX_PIECE. Note that
     * given a Tile object t, we get its value with t.value().
     */
    public static boolean maxTileExists(Board b) {
        return TileUtils.some(tile -> tile != null && tile.value() == MAX_PIECE, b);
    }

    /**
     * Returns true if there are any valid moves on the board.
     * There are two ways that there can be valid moves:
     * 1. There is at least one empty space on the board.
     * 2. There are two adjacent tiles with the same value.
     */
    public static boolean atLeastOneMoveExists(Board b) {
        if (emptySpaceExists(b)) {
            return true;
        }
        Predicate<Tile> hasAdjacentTileWithSameValue = (Tile tile) -> {
            for (int[] adjacentCoordinate : getAdjacentCoordinates(tile.col(), tile.row(), b)) {
                int adjacentCol = adjacentCoordinate[0];
                int adjacentRow = adjacentCoordinate[1];
                Tile adjacentTile = b.tile(adjacentCol, adjacentRow);
                if (tile.value() == adjacentTile.value()) {
                    return true;
                }
            }
            return false;
        };
        return TileUtils.some(hasAdjacentTileWithSameValue, b);
    }

    private static int[][] getAdjacentCoordinates(int col, int row, Board b) {
        int[][] coordinates = new int[][] {
            {col, row + 1}, {col + 1, row},
            {col, row - 1}, {col - 1, row}
        };
        return Arrays.stream(coordinates).filter(coordinate -> isValidCoordinate(coordinate, b)).toArray(int[][]::new);
    }

    private static boolean isValidCoordinate(int[] coordinate, Board b) {
        int col = coordinate[0];
        int row = coordinate[1];
        return col >= 0 && col < b.size() && row >= 0 && row < b.size();
    }

    private static class TileUtils {
        public static boolean some(Predicate<Tile> tilePredicate, Board b) {
            for (int col = 0; col < b.size(); col++) {
                for (int row = 0; row < b.size(); row++) {
                    if (tilePredicate.test(b.tile(col, row))) {
                        return true;
                    }
                }
            }
            return false;
        }
    }


    @Override
     /** Returns the model as a string, used for debugging. */
    public String toString() {
        Formatter out = new Formatter();
        out.format("%n[%n");
        for (int row = size() - 1; row >= 0; row -= 1) {
            for (int col = 0; col < size(); col += 1) {
                if (tile(col, row) == null) {
                    out.format("|    ");
                } else {
                    out.format("|%4d", tile(col, row).value());
                }
            }
            out.format("|%n");
        }
        String over = gameOver() ? "over" : "not over";
        out.format("] %d (max: %d) (game is %s) %n", score(), maxScore(), over);
        return out.toString();
    }

    @Override
    /** Returns whether two models are equal. */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (getClass() != o.getClass()) {
            return false;
        } else {
            return toString().equals(o.toString());
        }
    }

    @Override
    /** Returns hash code of Model’s string. */
    public int hashCode() {
        return toString().hashCode();
    }
}
