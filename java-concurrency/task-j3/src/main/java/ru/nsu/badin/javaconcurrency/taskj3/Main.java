package ru.nsu.badin.javaconcurrency.taskj3;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<String> result = new WebSpider("http://localhost:8080").start();

        for (String message : result) {
            System.out.println(message);
        }
        System.out.println(result.size());
    }
}
