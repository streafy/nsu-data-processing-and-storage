package ru.nsu.badin.javaconcurrency.taskj2;

import java.util.concurrent.locks.ReentrantLock;

public class Node {
    public String data;
    public Node prev;
    public Node next;
    public final ReentrantLock lock = new ReentrantLock();

    public Node(String data) {
        this.data = data;
    }
}