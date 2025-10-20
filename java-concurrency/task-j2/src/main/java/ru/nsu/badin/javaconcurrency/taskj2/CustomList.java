package ru.nsu.badin.javaconcurrency.taskj2;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.currentThread;

public class CustomList implements Iterable<String>, SortableList {
    private Node head;
    private final ReentrantLock headLock = new ReentrantLock();
    private final AtomicInteger swapCounter = new AtomicInteger(0);

    @Override
    public synchronized void addAsHead(String data) {
        Node node = new Node(data);
        if (head == null) {
            head = node;
            return;
        }
        node.next = head;
        head.prev = node;
        head = node;
    }

    @Override
    public void print() {
        headLock.lock();
        try {
            if (head == null) {
                return;
            }
            for (String data : this) {
                System.out.print(data + " -> ");
            }
            System.out.println();
        } finally {
            headLock.unlock();
        }

    }

    @Override
    public void bubbleSortStep(AtomicInteger stepCounter) {
        if (head == null) {
            return;
        }
        System.out.println("[" + currentThread().getName() + "] [steps = " + stepCounter.get() + "] [swaps = " + swapCounter.get() + "] сортирует");

        Node curr = head;
        Node next = curr.next;
        while (next != null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                currentThread().interrupt();
            }

            if (curr.data.compareTo(next.data) > 0) {
                swap(curr, next);
                swapCounter.incrementAndGet();
            }
            stepCounter.incrementAndGet();

            curr = curr.next;
            next = (curr == null) ? null : curr.next;
        }
    }

    public void swap(Node first, Node second) {
        if (first == null || second == null || first == second) return;

        if (second.next == first) {
            Node temp = first;
            first = second;
            second = temp;
        }

        if (first.next != second || second.prev != first) {
            return;
        }

        Node firstPrev = first.prev;
        Node secondNext = second.next;

        if (firstPrev != null) firstPrev.lock.lock();
        first.lock.lock();
        second.lock.lock();
        if (secondNext != null) secondNext.lock.lock();

        try {
            if (first.prev != firstPrev || first.next != second || second.prev != first || second.next != secondNext) {
                return;
            }

            if (firstPrev != null) {
                firstPrev.next = second;
            } else {
                head = second;
            }
            if (secondNext != null) {
                secondNext.prev = first;
            }

            second.prev = firstPrev;
            second.next = first;

            first.prev = second;
            first.next = secondNext;
        } finally {
            if (secondNext != null) secondNext.lock.unlock();
            second.lock.unlock();
            first.lock.unlock();
            if (firstPrev != null) firstPrev.lock.unlock();
        }
    }

    @Override
    public Iterator<String> iterator() {
        return new CustomIterator();
    }

    private class CustomIterator implements Iterator<String> {
        private Node current = head;

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public String next() {
            String data = current.data;
            current = current.next;
            return data;
        }
    }
}
