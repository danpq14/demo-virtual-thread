package vn.vnpay.efin;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class VirtualThreadDemo {

    private static final int NUMBER_OF_TASKS = 10_000;

    public static void main(String[] args) throws Exception {
        System.out.println("Demo Virtual Thread - Xử lý non-blocking");
        System.out.println("===================================");
        
        System.out.println("\n1. So sánh hiệu suất Platform Thread vs Virtual Thread:");
        comparePlatformVsVirtualThreads();
        
        System.out.println("\n2. Demo Non-blocking IO với Virtual Thread:");
        demoNonBlockingIO();
        
        System.out.println("\n3. Demo HTTP Server với Virtual Thread:");
        HttpServerDemo.main(args);
        
        System.out.println("\n4. Demo Database với Virtual Thread:");
        DatabaseVirtualThreadDemo.main(args);
    }
    
    private static void comparePlatformVsVirtualThreads() throws Exception {
        // Chạy với Platform Thread truyền thống
        Instant start = Instant.now();
        try (ExecutorService executorService = Executors.newFixedThreadPool(100)) {
            submitTasks(executorService);
        }
        Duration platformThreadDuration = Duration.between(start, Instant.now());
        System.out.println("Platform Threads: " + platformThreadDuration.toMillis() + " ms");

        // Chạy với Virtual Thread
        start = Instant.now();
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            submitTasks(executorService);
        }
        Duration virtualThreadDuration = Duration.between(start, Instant.now());
        System.out.println("Virtual Threads: " + virtualThreadDuration.toMillis() + " ms");
        System.out.println("Cải thiện: " + (platformThreadDuration.toMillis() - virtualThreadDuration.toMillis()) + " ms");
    }
    
    private static void submitTasks(ExecutorService executorService) throws Exception {
        CountDownLatch latch = new CountDownLatch(NUMBER_OF_TASKS);
        IntStream.range(0, NUMBER_OF_TASKS).forEach(i -> {
            executorService.submit(() -> {
                try {
                    // Giả lập tác vụ CPU nhẹ với thời gian chờ IO
                    Thread.sleep(10); // Giả lập I/O blocking
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        });
        latch.await();
    }
    
    private static void demoNonBlockingIO() throws Exception {
        System.out.println("Bắt đầu mô phỏng các tác vụ IO non-blocking với 1000 virtual threads...");
        
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch latch = new CountDownLatch(1000);
            
            for (int i = 0; i < 1000; i++) {
                int taskId = i;
                executor.submit(() -> {
                    try {
                        // Mô phỏng tác vụ IO như gọi API, truy vấn DB...
                        simulateIOOperation(taskId);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            System.out.println("Đã tạo 1000 virtual threads để xử lý các tác vụ IO non-blocking");
            System.out.println("Đang chờ hoàn thành...");
            latch.await();
            System.out.println("Tất cả tác vụ IO đã hoàn thành!");
        }
    }
    
    private static void simulateIOOperation(int taskId) {
        try {
            // Mô phỏng thời gian chờ IO ngẫu nhiên từ 100-300ms
            int delay = ThreadLocalRandom.current().nextInt(100, 300);
            Thread.sleep(delay);
            
            if (taskId % 100 == 0) {
                System.out.println("Task " + taskId + " hoàn thành sau " + delay + "ms, sử dụng " + Thread.currentThread());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
} 