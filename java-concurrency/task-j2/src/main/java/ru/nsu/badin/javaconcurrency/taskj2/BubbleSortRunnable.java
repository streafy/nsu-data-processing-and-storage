package ru.nsu.badin.javaconcurrency.taskj2;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.currentThread;

public class BubbleSortRunnable implements Runnable {
    private final SortableList list;
    private final AtomicInteger stepCounter;

    public BubbleSortRunnable(SortableList list, AtomicInteger stepCounter) {
        this.list = list;
        this.stepCounter = stepCounter;
    }

    @Override
    public void run() {
        while (!currentThread().isInterrupted()) {
            list.bubbleSortStep(stepCounter);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                currentThread().interrupt();
                break;
            }
        }
    }
}
