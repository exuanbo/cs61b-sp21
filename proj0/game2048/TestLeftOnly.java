package game2048;

import org.junit.Test;

/** Tests the tilt() method in the left (Side.WEST) direction only.
 *
 * @author Exuanbo
 */
public class TestLeftOnly extends TestUtils {


    @Test
    /** Move tiles left (no merging). */
    public void testLeftNoMerge() {
        int[][] before = new int[][] {
                {0, 0, 2, 0},
                {0, 0, 0, 4},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
        };
        int[][] after = new int[][] {
                {2, 0, 0, 0},
                {4, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
        };

        model = new Model(before, 0, 0, false);
        String prevBoard = model.toString();
        boolean changed = model.tilt(Side.WEST);
        checkChanged(Side.WEST, true, changed);
        checkModel(after, 0, 0, prevBoard, Side.WEST);
    }

    @Test
    /** A basic merge. */
    public void testLeftBasicMerge() {
        int[][] before = new int[][] {
                {0, 0, 0, 0},
                {0, 2, 2, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
        };
        int[][] after = new int[][] {
                {0, 0, 0, 0},
                {4, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
        };

        updateModel(before, 0, 0, false);
        String prevBoard = model.toString();
        boolean changed = model.tilt(Side.WEST);
        checkChanged(Side.WEST, true, changed);
        checkModel(after, 4, 0, prevBoard, Side.WEST);
    }

    @Test
    /** A triple merge. Only the leading 2 tiles should merge. */
    public void testLeftTripleMerge() {
        int[][] before = new int[][] {
                {0, 0, 0, 0},
                {2, 0, 2, 2},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
        };
        int[][] after = new int[][] {
                {0, 0, 0, 0},
                {4, 2, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
        };

        updateModel(before, 0, 0, false);
        String prevBoard = model.toString();
        boolean changed = model.tilt(Side.WEST);
        checkChanged(Side.WEST, true, changed);
        checkModel(after, 4, 0, prevBoard, Side.WEST);
    }

    @Test
    /** A tricky merge.
     *
     * The tricky part here is that the 4 tile on the rightmost row shouldn't
     * merge with the newly created 4 tile on the leftmost row.*/
    public void testLeftTrickyMerge() {
        int[][] before = new int[][] {
                {0, 0, 0, 0},
                {2, 2, 0, 4},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
        };
        int[][] after = new int[][] {
                {0, 0, 0, 0},
                {4, 4, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
        };

        updateModel(before, 0, 0, false);
        String prevBoard = model.toString();
        boolean changed = model.tilt(Side.WEST);
        checkChanged(Side.WEST, true, changed);
        checkModel(after, 4, 0, prevBoard, Side.WEST);
    }
}
