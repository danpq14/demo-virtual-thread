package vn.vnpay.efin;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class NonBlockingDemo {

    private record Person(String name, Integer age) {}

    public static void main(String[] args) throws Exception {
        //Blocking - Thực hiện tuần tự
        System.out.println("\n=== Demo Virtual Thread ===\n");
        System.out.println("\n===== 1. Thực hiện task một cách tuần tự =====\n");

        Instant start = Instant.now();
        String stringVal = someActionReturnString("A String");
        Integer intVal = someActionReturnInteger(0);
        Person person = someActionReturnPerson();

        doSomeActionAfterOtherActionsDone(stringVal, intVal, person);
        Duration duration = Duration.between(start, Instant.now());
        System.out.println("Thời gian thực hiện tuần tự: " + duration.toMillis() + "ms");

        //Non-Blocking - Thực hiện song song
        System.out.println("\n===== 2. Thực hiện song song với Virtual Thread =====\n");
        start = Instant.now();
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Tạo 3 CompletableFuture để thực hiện 3 tác vụ song song
            CompletableFuture<String> stringFuture = CompletableFuture.supplyAsync(
                    () -> someActionReturnString("A String"), executor);
            
            CompletableFuture<Integer> integerFuture = CompletableFuture.supplyAsync(
                    () -> someActionReturnInteger(0), executor);
            
            CompletableFuture<Person> personFuture = CompletableFuture.supplyAsync(
                    () -> someActionReturnPerson(), executor);
            
            // Chờ tất cả các tác vụ hoàn thành và kết hợp kết quả
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    stringFuture, integerFuture, personFuture);
            
            // Đợi tất cả hoàn thành
            allFutures.join();
            
            // Lấy kết quả và thực hiện tác vụ cuối cùng
            String resultString = stringFuture.get();
            Integer resultInteger = integerFuture.get();
            Person resultPerson = personFuture.get();
            
            doSomeActionAfterOtherActionsDone(resultString, resultInteger, resultPerson);
        }
        
        Duration nonBlockingDuration = Duration.between(start, Instant.now());
        System.out.println("Time taken with Virtual Threads: " + nonBlockingDuration.toMillis() + "ms");
        
        // So sánh cải thiện
        long improvement = duration.toMillis() - nonBlockingDuration.toMillis();
        double percentImprovement = (improvement * 100.0) / duration.toMillis();
        System.out.println("\n===== So sánh hiệu suất =====");
        System.out.println("Thời gian xử lý tuần tự: " + duration.toMillis() + "ms");
        System.out.println("Thời gian xử lý song song: " + nonBlockingDuration.toMillis() + "ms");
        System.out.println("Cải thiện: " + improvement + "ms (" + String.format("%.2f", percentImprovement) + "%)");
    }

    public static String someActionReturnString(String input) {
        System.out.println("Doing some action returning string: " + input);
        try {
            // Mô phỏng thời gian truy vấn database từ 50-150ms
//            int delay = ThreadLocalRandom.current().nextInt(50, 150);
            Thread.sleep(100);
            return input + " was modified";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "error";
        }
    }

    public static Integer someActionReturnInteger(Integer input) {
        System.out.println("Doing some action returning integer: " + input);
        try {
            // Mô phỏng thời gian truy vấn database từ 50-150ms
//            int delay = ThreadLocalRandom.current().nextInt(50, 150);
            Thread.sleep(100);
            return input + 100;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return input;
        }
    }

    public static Person someActionReturnPerson() {
        System.out.println("Doing some action returning person");
        try {
            // Mô phỏng thời gian truy vấn database từ 50-150ms
//            int delay = ThreadLocalRandom.current().nextInt(50, 150);
            Thread.sleep(100);
            return new Person("John", 25);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public static void doSomeActionAfterOtherActionsDone(String string, Integer integer, Person person) {
        System.out.println("Doing some action after other actions done: ");
        try {
            Thread.sleep(100);
            System.out.println(person.name + ", " + person.age + ", " + string + ", " + integer);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
