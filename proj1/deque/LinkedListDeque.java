package deque;

import java.util.Iterator;

public class LinkedListDeque<T> implements Deque<T> {
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
                return "null";
            }
            return item.toString();
        }
    }

    private final Node<T> head;
    private int size;

    public LinkedListDeque() {
        head = new Node<>(null, null, null);
        head.next = head;
        head.prev = head;
        size = 0;
    }

    public LinkedListDeque(T item) {
        head = new Node<>(null, null, null);
        head.next = new Node<>(item, head, head);
        head.prev = head.next;
        size = 1;
    }

    public void addFirst(T item) {
        head.next = new Node<>(item, head, head.next);
        head.prev = head.next;
        size += 1;
    }

    public void addLast(T item) {
        head.prev = new Node<>(item, head.prev, head);
        head.prev.prev.next = head.prev;
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
        if (p == head) {
            return;
        }
        for (int i = 0; i < size; i++) {
            items[i] = p.item.toString();
            p = p.next;
        }
        System.out.println(String.join(" ", items));
    }

    public T removeFirst() {
        if (isEmpty()) {
            return null;
        }
        T item = head.next.item;
        head.next = head.next.next;
        size -= 1;
        return item;
    }

    public T removeLast() {
        if (isEmpty()) {
            return null;
        }
        T item = head.prev.item;
        head.prev.prev.next = head;
        head.prev = head.prev.prev;
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
        if (p == head) {
            return null;
        }
        if (index == 0) {
            return p.item;
        }
        return getRecursiveHelper(index - 1, p.next);
    }

    @Override
    public Iterator<T> iterator() {
        return new LinkedListDequeIterator();
    }

    private class LinkedListDequeIterator implements Iterator<T> {
        private Node<T> p;

        public LinkedListDequeIterator() {
            p = head.next;
        }

        @Override
        public boolean hasNext() {
            return p == head;
        }

        @Override
        public T next() {
            T item = p.item;
            p = p.next;
            return item;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LinkedListDeque)) {
            return false;
        }
        LinkedListDeque<?> lld = (LinkedListDeque<?>) o;
        if (lld.size() != size) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (lld.get(i) != get(i)) {
                return false;
            }
        }
        return true;
    }
}
