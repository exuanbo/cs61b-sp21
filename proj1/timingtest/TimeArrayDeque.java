package timingtest;

import deque.ArrayDeque;

public class TimeArrayDeque {
    public static void main(String[] args) {
        timeArrayDequeAddFirst();
    }

    public static void timeArrayDequeAddFirst() {
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<>();
        Timing.addFirst(arrayDeque);
    }
}

