package tuwien.taskinn.services.scheduling.utility;

/**
 * Space efficient representation of NxM matrices of booleans.
 */
public class BitMatrix {

    /** Number of columns */
    public final int columns;
    /** Number of rows */
    public final int rows;
    private final long[] bitLongs;

    /**
     * Constructs a BitMatrix with the given number of columns and rows. All elements are false initially.
     *
     * @param pColumns Number of columns
     * @param pRows Number of rows
     */
    public BitMatrix(int pColumns, int pRows) {
        if (pColumns <= 0 || pRows <= 0)
            throw new IllegalArgumentException("Number of rows and columns must be positive");

        columns = pColumns;
        rows = pRows;
        bitLongs = new long[((pColumns * pRows) + 63) / 64];
    }

    /**
     * Retrieves the boolean at the given column and row.
     *
     * @param column The given column
     * @param row The given row
     * @return The boolean at the given column and row
     */
    public boolean get(int column, int row) {
        if (column < 0 || row < 0)
            throw new IndexOutOfBoundsException("Column and row must be non-negative");
        if (column >= columns || row >= rows)
            throw new IndexOutOfBoundsException("Column and row must be within dimensions");

        final int bitPosition = (column) * rows + row;
        return (bitLongs[bitPosition / 64] & (1L << (bitPosition % 64))) != 0;
    }

    /**
     * Sets the boolean at the given column and row to the given value.
     *
     * @param column The given column
     * @param row The given row
     * @param value The given value
     */
    public void set(int column, int row, boolean value) {
        if (column < 0 || row < 0) {
            throw new IndexOutOfBoundsException("Column and row must be non-negative");
        }
        if (column >= columns || row >= rows) {
            throw new IndexOutOfBoundsException("Column and row must be within dimensions");
        }

        final int bitPosition = (column) * rows + row;

        if (value) {
            bitLongs[bitPosition / 64] |= (1L << (bitPosition % 64));
        } else {
            bitLongs[bitPosition / 64] = (bitLongs[bitPosition / 64] & (~(1L << (bitPosition % 64))));
        }
    }

    /**
     * Creates a fresh copy.
     *
     * @return A fresh copy
     */
    public BitMatrix copy() {
        final BitMatrix result = new BitMatrix(columns, rows);
        System.arraycopy(bitLongs, 0, result.bitLongs, 0, bitLongs.length);
        return result;
    }

}
