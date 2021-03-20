package deque;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Performs some basic linked list tests.
 */
public class LinkedListDequeTest {

    /**
     * Adds a few things to the list, checking isEmpty() and size() are correct,
     * finally printing the results.
     */
    @Test
    public void addIsEmptySizeTest() {
        LinkedListDeque<String> lld1 = new LinkedListDeque<>();

        assertTrue("A newly initialized LLDeque should be empty", lld1.isEmpty());

        lld1.addFirst("front");
        assertEquals(1, lld1.size());
        assertFalse("lld1 should now contain 1 item", lld1.isEmpty());

        lld1.addLast("middle");
        assertEquals(2, lld1.size());

        lld1.addLast("back");
        assertEquals(3, lld1.size());

        System.out.println("Printing out deque: ");
        lld1.printDeque();
    }

    /**
     * Adds an item, then removes an item, and ensures that dll is empty afterwards.
     */
    @Test
    public void addRemoveTest() {
        LinkedListDeque<Integer> lld1 = new LinkedListDeque<>();

        assertTrue("lld1 should be empty upon initialization", lld1.isEmpty());

        lld1.addFirst(10);
        assertFalse("lld1 should contain 1 item", lld1.isEmpty());

        lld1.removeFirst();
        assertTrue("lld1 should be empty after removal", lld1.isEmpty());
    }

    @Test
    /* Tests removing from an empty deque */
    public void removeEmptyTest() {
        LinkedListDeque<Integer> lld1 = new LinkedListDeque<>();

        lld1.addFirst(3);

        lld1.removeLast();
        lld1.removeFirst();
        lld1.removeLast();
        lld1.removeFirst();

        int size = lld1.size();
        String errorMsg = "  Bad size returned when removing from empty deque.\n";
        errorMsg += "  student size() returned " + size + "\n";
        errorMsg += "  actual size() returned 0\n";

        assertEquals(errorMsg, 0, size);
    }

    @Test
    /* Check if you can create LinkedListDeques with different parameterized types */
    public void multipleParamTest() {
        LinkedListDeque<String> lld1 = new LinkedListDeque<>();
        LinkedListDeque<Double> lld2 = new LinkedListDeque<>();
        LinkedListDeque<Boolean> lld3 = new LinkedListDeque<>();

        lld1.addFirst("string");
        lld2.addFirst(3.14159);
        lld3.addFirst(true);

        String s = lld1.removeFirst();
        double d = lld2.removeFirst();
        boolean b = lld3.removeFirst();
    }

    @Test
    /* check if null is return when removing from an empty LinkedListDeque. */
    public void emptyNullReturnTest() {
        LinkedListDeque<Integer> lld1 = new LinkedListDeque<>();

        assertNull("Should return null when removeFirst is called on an empty Deque,", lld1.removeFirst());
        assertNull("Should return null when removeLast is called on an empty Deque,", lld1.removeLast());
    }

    @Test
    /* Add large number of elements to deque; check if order is correct. */
    public void bigLLDequeTest() {
        LinkedListDeque<Integer> lld1 = new LinkedListDeque<>();

        for (int i = 0; i < 1000000; i++) {
            lld1.addLast(i);
        }

        for (double i = 0; i < 500000; i++) {
            assertEquals("Should have the same value", (double) lld1.removeFirst(), i, 0.0);
        }

        for (double i = 999999; i > 500000; i--) {
            assertEquals("Should have the same value", (double) lld1.removeLast(), i, 0.0);
        }
    }

    @Test
    public void nonEmptyInstantiationTest() {
        LinkedListDeque<Integer> lld1 = new LinkedListDeque<>(1);

        assertFalse("Should not be empty", lld1.isEmpty());
        assertEquals("Should have size 1", 1, lld1.size());
    }

    @Test
    public void getTest() {
        LinkedListDeque<Integer> lld1 = new LinkedListDeque<>();

        for (int i = 0; i < 1000; i++) {
            lld1.addLast(i);
        }

        for (int i = 0; i < 1000; i++) {
            assertEquals("Should have same value", i, (int) lld1.get(i));
        }
    }

    @Test
    public void getRecursiveTest() {
        LinkedListDeque<Integer> lld1 = new LinkedListDeque<>();

        for (int i = 0; i < 1000; i++) {
            lld1.addLast(i);
        }

        for (int i = 0; i < 1000; i++) {
            assertEquals("Should have same value", i, (int) lld1.getRecursive(i));
        }
    }

    @Test
    public void iteratorTest() {
        LinkedListDeque<Integer> lld1 = new LinkedListDeque<>();

        for (int i = 0; i < 1000; i++) {
            lld1.addLast(i);
        }

        int i = 0;
        for (int item : lld1) {
            assertEquals("Should have same value", i, item);
            i += 1;
        }
    }

    @Test
    public void equalsTest() {
        LinkedListDeque<Integer> lld1 = new LinkedListDeque<>();
        LinkedListDeque<Integer> lld2 = new LinkedListDeque<>();

        lld1.addLast(0);
        lld2.addLast(0);
        assertEquals(lld1, lld2);

        lld1.addLast(1);
        assertNotEquals(lld1, lld2);

        lld2.addLast(2);
        assertNotEquals(lld1, lld2);
    }
}
