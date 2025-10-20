package ru.nsu.badin.javaconcurrency.taskj2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.currentThread;

public class ArrayListSynchronizedWrapper implements SortableList {
    private final List<String> list = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger swapCounter = new AtomicInteger(0);

    @Override
    public void addAsHead(String data) {
        list.add(0, data);
    }

    @Override
    public void print() {
        synchronized (list) {
            for (String data : list) {
                System.out.print(data + " -> ");
            }
            System.out.println();
        }
    }

    @Override
    public void bubbleSortStep(AtomicInteger stepCounter) {
        if (list.isEmpty()) {
            return;
        }
        System.out.println("[" + currentThread().getName() + "] [steps = " + stepCounter.get() + "] [swaps = " + swapCounter.get() + "] сортирует");
        synchronized (list) {
            for (int i = 0; i < list.size() - 1; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    currentThread().interrupt();
                }

                String curr = list.get(i);
                String next = list.get(i + 1);

                if (curr.compareTo(next) > 0) {
                    Collections.swap(list, i, i + 1);

                    swapCounter.incrementAndGet();
                }

                stepCounter.incrementAndGet();
            }
        }
    }
}
