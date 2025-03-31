package vn.vnpay.efin;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Demo ứng dụng Virtual Thread trong các tác vụ truy vấn database đồng thời
 */
public class DatabaseVirtualThreadDemo {

    // Số lượng truy vấn đồng thời
    private static final int CONCURRENT_QUERIES = 1000;
    
    // Record để lưu kết quả truy vấn
    private record QueryResult(String tableName, String data) {}
    
    public static void main(String[] args) throws Exception {
        System.out.println("\n=== Demo Virtual Thread với Truy vấn Database ===\n");
        
        // So sánh hiệu suất giữa xử lý tuần tự và đồng thời
        System.out.println("1. So sánh hiệu suất xử lý tuần tự vs đồng thời với Virtual Thread:");
        compareSequentialVsConcurrent();
        
        // Demo truy vấn nhiều bảng đồng thời
        System.out.println("\n2. Demo truy vấn nhiều bảng đồng thời:");
        demoMultiTableQuery();
        
        // Xử lý batch với Virtual Thread
        System.out.println("\n3. Xử lý batch với Virtual Thread:");
        demoBatchProcessing();
    }
    
    private static void compareSequentialVsConcurrent() throws Exception {
        // Thực hiện tuần tự
        System.out.println("Đang thực hiện " + CONCURRENT_QUERIES + " truy vấn database theo cách tuần tự...");
        Instant start = Instant.now();
        
        for (int i = 0; i < CONCURRENT_QUERIES; i++) {
            simulateDatabaseQuery("Query " + i);
        }
        
        Duration sequentialDuration = Duration.between(start, Instant.now());
        System.out.println("Thời gian xử lý tuần tự: " + sequentialDuration.toMillis() + " ms");
        
        // Thực hiện đồng thời với Virtual Thread
        System.out.println("Đang thực hiện " + CONCURRENT_QUERIES + " truy vấn database đồng thời với Virtual Thread...");
        start = Instant.now();
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < CONCURRENT_QUERIES; i++) {
                final int queryId = i;
                futures.add(executor.submit(() -> {
                    simulateDatabaseQuery("Query " + queryId);
                    return null;
                }));
            }
            
            // Chờ tất cả queries hoàn thành
            for (Future<?> future : futures) {
                future.get();
            }
        }
        
        Duration concurrentDuration = Duration.between(start, Instant.now());
        System.out.println("Thời gian xử lý đồng thời: " + concurrentDuration.toMillis() + " ms");
        System.out.println("Cải thiện: " + 
                (sequentialDuration.toMillis() - concurrentDuration.toMillis()) + " ms (" + 
                (sequentialDuration.toMillis() * 100 / concurrentDuration.toMillis() - 100) + "%)");
    }
    
    private static void demoMultiTableQuery() throws Exception {
        System.out.println("Mô phỏng truy vấn đồng thời từ nhiều bảng trong database...");
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Instant start = Instant.now();
            
            // Tạo các truy vấn đồng thời cho 5 bảng khác nhau
            CompletableFuture<QueryResult> usersQuery = CompletableFuture.supplyAsync(
                    () -> simulateDatabaseTableQuery("users", 300), executor);
            
            CompletableFuture<QueryResult> ordersQuery = CompletableFuture.supplyAsync(
                    () -> simulateDatabaseTableQuery("orders", 250), executor);
            
            CompletableFuture<QueryResult> productsQuery = CompletableFuture.supplyAsync(
                    () -> simulateDatabaseTableQuery("products", 200), executor);
            
            CompletableFuture<QueryResult> paymentsQuery = CompletableFuture.supplyAsync(
                    () -> simulateDatabaseTableQuery("payments", 350), executor);
            
            CompletableFuture<QueryResult> logsQuery = CompletableFuture.supplyAsync(
                    () -> simulateDatabaseTableQuery("logs", 150), executor);
            
            // Kết hợp tất cả các kết quả
            CompletableFuture<Void> allQueries = CompletableFuture.allOf(
                    usersQuery, ordersQuery, productsQuery, paymentsQuery, logsQuery);
            
            // Chờ tất cả hoàn thành và lấy kết quả
            allQueries.join();
            
            // In các kết quả
            List<QueryResult> results = List.of(
                    usersQuery.join(),
                    ordersQuery.join(),
                    productsQuery.join(),
                    paymentsQuery.join(),
                    logsQuery.join()
            );
            
            Duration duration = Duration.between(start, Instant.now());
            
            System.out.println("Tất cả các truy vấn hoàn thành trong: " + duration.toMillis() + " ms");
            for (QueryResult result : results) {
                System.out.println("- " + result.tableName() + ": " + result.data());
            }
            
            // Hiển thị độ cải thiện
            int maxDelay = results.stream()
                    .mapToInt(r -> r.data().contains("sau ") ? 
                            Integer.parseInt(r.data().split("sau ")[1].split("ms")[0]) : 0)
                    .max()
                    .orElse(0);
            
            System.out.println("Truy vấn chậm nhất mất: " + maxDelay + " ms");
            System.out.println("Tổng thời gian nếu tuần tự: " + 
                    results.stream()
                            .mapToInt(r -> r.data().contains("sau ") ? 
                                    Integer.parseInt(r.data().split("sau ")[1].split("ms")[0]) : 0)
                            .sum() + " ms");
            System.out.println("Cải thiện so với xử lý tuần tự: " + 
                    (results.stream()
                            .mapToInt(r -> r.data().contains("sau ") ? 
                                    Integer.parseInt(r.data().split("sau ")[1].split("ms")[0]) : 0)
                            .sum() - duration.toMillis()) + " ms");
        }
    }
    
    private static void demoBatchProcessing() throws Exception {
        System.out.println("Mô phỏng xử lý batch với 10,000 records...");
        
        // Tạo bộ dữ liệu mẫu 10,000 records
        List<String> records = IntStream.range(0, 10_000)
                .mapToObj(i -> "Record " + i)
                .collect(Collectors.toList());
        
        int batchSize = 1000; // Mỗi batch 1000 records
        int numberOfBatches = (records.size() + batchSize - 1) / batchSize;
        
        System.out.println("Tổng số records: " + records.size());
        System.out.println("Kích thước batch: " + batchSize);
        System.out.println("Số lượng batches: " + numberOfBatches);
        
        Instant start = Instant.now();
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch latch = new CountDownLatch(numberOfBatches);
            
            // Xử lý mỗi batch trong một virtual thread riêng
            for (int i = 0; i < numberOfBatches; i++) {
                int fromIndex = i * batchSize;
                int toIndex = Math.min(fromIndex + batchSize, records.size());
                List<String> batch = records.subList(fromIndex, toIndex);
                
                // Tạo bản sao final của biến i để sử dụng trong lambda
                final int batchId = i;
                
                executor.submit(() -> {
                    try {
                        processBatch(batchId, batch);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            System.out.println("Đã gửi tất cả batches tới xử lý đồng thời sử dụng Virtual Thread.");
            System.out.println("Đang chờ tất cả batches hoàn thành...");
            latch.await();
        }
        
        Duration duration = Duration.between(start, Instant.now());
        System.out.println("Tất cả batches đã được xử lý trong: " + duration.toMillis() + " ms");
        System.out.println("Thời gian trung bình cho mỗi batch: " + (duration.toMillis() / numberOfBatches) + " ms");
        System.out.println("Tốc độ xử lý: " + (records.size() * 1000L / duration.toMillis()) + " records/giây");
    }
    
    // Các phương thức helper để mô phỏng database operations
    
    private static void simulateDatabaseQuery(String queryName) {
        try {
            // Mô phỏng thời gian truy vấn database từ 50-150ms
            int delay = ThreadLocalRandom.current().nextInt(50, 150);
            Thread.sleep(delay);
            
            if (queryName.contains("0") && queryName.length() <= 8) {
                System.out.println(queryName + " hoàn thành sau " + delay + 
                        "ms trên " + Thread.currentThread());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static QueryResult simulateDatabaseTableQuery(String tableName, int baseDelay) {
        try {
            // Thêm độ trễ ngẫu nhiên ±50ms vào base delay
            int delay = baseDelay + ThreadLocalRandom.current().nextInt(-50, 50);
            if (delay < 10) delay = 10; // Đảm bảo độ trễ tối thiểu
            
            Thread.sleep(delay);
            return new QueryResult(tableName, 
                    "Dữ liệu từ bảng " + tableName + " sau " + delay + "ms");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new QueryResult(tableName, "Error: Interrupted");
        }
    }
    
    private static void processBatch(int batchId, List<String> batch) {
        try {
            // Mô phỏng thời gian xử lý batch từ 200-400ms
            int processingTime = ThreadLocalRandom.current().nextInt(200, 400);
            Thread.sleep(processingTime);
            
            System.out.println("Batch " + batchId + " (" + batch.size() + 
                    " records) đã xử lý sau " + processingTime + 
                    "ms trên " + Thread.currentThread());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Batch " + batchId + " bị gián đoạn");
        }
    }
} 