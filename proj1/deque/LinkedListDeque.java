package deque;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Iterator;

public class LinkedListDeque<T> implements Iterable<T> {
    private static class Node<N> {
        public N item;
        public Node<N> prev;
        public Node<N> next;

        public Node(N i, Node<N> p, Node<N> n) {
            item = i;
            prev = p;
            next = n;
        }

        @Override
        public String toString() {
            if (item == null) {
                return prev == null ? "HEAD" : next == null ? "TAIL" : "null";
            }
            return item.toString();
        }
    }

    private final Node<T> head;
    private final Node<T> tail;
    private int size;

    public LinkedListDeque() {
        head = new Node<>(null, null, null);
        tail = new Node<>(null, head, null);
        head.next = tail;
        size = 0;
    }

    public LinkedListDeque(T item) {
        head = new Node<>(null, null, null);
        head.next = new Node<>(item, head, null);
        tail = new Node<>(null, head.next, null);
        head.next.next = tail;
        size = 1;
    }

    public void addFirst(T item) {
        head.next = new Node<>(item, head, head.next);
        head.next.next.prev = head.next;
        if (tail.prev == head) {
            tail.prev = head.next;
        }
        size += 1;
    }

    public void addLast(T item) {
        tail.prev.next = new Node<>(item, tail.prev, tail);
        tail.prev = tail.prev.next;
        size += 1;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void printDeque() {
        String[] items = new String[size];
        Node<T> p = head.next;
        if (p == tail) {
            return;
        }
        for (int i = 0; i < size; i++) {
            items[i] = p.item.toString();
            p = p.next;
        }
        System.out.println(String.join(" ", items));
    }

    public T removeFirst() {
        if (head.next == tail) {
            return null;
        }
        T item = head.next.item;
        head.next = head.next.next;
        size -= 1;
        return item;
    }

    public T removeLast() {
        if (tail.prev == head) {
            return null;
        }
        T item = tail.prev.item;
        tail.prev.prev.next = tail;
        tail.prev = tail.prev.prev;
        size -= 1;
        return item;
    }

    public T get(int index) {
        Node<T> p = head.next;
        for (int i = 0; i < size; i++) {
            if (i == index) {
                return p.item;
            }
            p = p.next;
        }
        return null;
    }

    public T getRecursive(int index) {
        return getRecursiveHelper(index, head.next);
    }

    private T getRecursiveHelper(int index, Node<T> p) {
        if (p == tail) {
            return null;
        }
        if (index == 0) {
            return p.item;
        }
        return getRecursiveHelper(index - 1, p.next);
    }

    @Override
    @NonNull
    public Iterator<T> iterator() {
        return new LinkedListDequeIterator();
    }

    private class LinkedListDequeIterator implements Iterator<T> {
        private Node<T> p;

        public LinkedListDequeIterator() {
            p = head.next;
        }

        public boolean hasNext() {
            return p == tail;
        }

        public T next() {
            T item = p.item;
            p = p.next;
            return item;
        }
    }
}
