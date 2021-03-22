package timingtest;

import deque.LinkedListDeque;

public class TimeLinkedListDeque {
    public static void main(String[] args) {
        timeLinkedListDequeAddFirst();
    }

    public static void timeLinkedListDequeAddFirst() {
        LinkedListDeque<Integer> linkedListDeque = new LinkedListDeque<>();
        Timing.addFirst(linkedListDeque);
    }
}
