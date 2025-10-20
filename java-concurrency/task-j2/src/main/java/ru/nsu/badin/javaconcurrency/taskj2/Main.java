package ru.nsu.badin.javaconcurrency.taskj2;

import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.currentThread;

public class Main {
    private static final int THREAD_COUNT = 3;

    public static void main(String[] args) {
        CustomList customList = new CustomList();
        ArrayListSynchronizedWrapper wrapperList = new ArrayListSynchronizedWrapper();

        AtomicInteger customListSteps = new AtomicInteger(0);
        AtomicInteger wrapperListSteps = new AtomicInteger(0);

        System.out.println("Custom List");
        start(customList, customListSteps);

        System.out.println("ArrayList Synchronized Wrapper");
        start(wrapperList, wrapperListSteps);
    }

    private static void start(SortableList list, AtomicInteger steps) {
        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(new BubbleSortRunnable(list, steps));
            threads[i].start();
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("Введите строки. Пустая строка - вывод списка, :q для выхода");
        while (true) {
            String input = scanner.nextLine();
            System.out.println("[" + currentThread().getName() + "] " + "принял строку " + input);
            if (input.isEmpty()) {
                System.out.print("[Шаги - " + steps + "] ");
                list.print();
                continue;
            }
            if (input.equalsIgnoreCase(":q")) {
                Arrays.stream(threads).forEach(Thread::interrupt);
                break;
            }
            list.addAsHead(input);
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                currentThread().interrupt();
            }
        }
    }
}
