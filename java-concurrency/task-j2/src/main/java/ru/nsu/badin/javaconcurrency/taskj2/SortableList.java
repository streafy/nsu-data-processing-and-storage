package ru.nsu.badin.javaconcurrency.taskj2;

import java.util.concurrent.atomic.AtomicInteger;

public interface SortableList {
    void addAsHead(String data);

    void bubbleSortStep(AtomicInteger stepCounter);

    void print();
}
