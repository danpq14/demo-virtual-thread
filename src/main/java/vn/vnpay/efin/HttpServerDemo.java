package vn.vnpay.efin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class HttpServerDemo {

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Đăng ký handlers cho các endpoints
        server.createContext("/api/fast", new FastHandler());
        server.createContext("/api/slow", new SlowHandler());
        server.createContext("/api/parallel", new ParallelTaskHandler());
        
        // Sử dụng Virtual Thread Executor để xử lý các requests
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        
        server.start();
        System.out.println("HTTP Server đang chạy tại http://localhost:" + port);
        System.out.println("Các endpoints có sẵn:");
        System.out.println("  - GET /api/fast - Phản hồi nhanh");
        System.out.println("  - GET /api/slow - Phản hồi chậm (mô phỏng blocking IO)");
        System.out.println("  - GET /api/parallel - Phản hồi sau khi thực hiện nhiều tác vụ song song");
        System.out.println("\nĐể test hiệu suất, sử dụng công cụ như Apache Bench:");
        System.out.println("  ab -n 1000 -c 100 http://localhost:8080/api/slow");
        
        // Không dừng server trong demo này, chỉ chạy 30 giây
        try {
            Thread.sleep(30_000);
            server.stop(0);
            System.out.println("HTTP Server đã dừng sau 30 giây.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Handler xử lý nhanh, không có blocking
    static class FastHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Phản hồi nhanh từ " + Thread.currentThread();
            sendResponse(exchange, response);
        }
    }
    
    // Handler mô phỏng blocking IO operation
    static class SlowHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Mô phỏng blocking IO từ 200-500ms (như gọi database, external API...)
                Thread.sleep(ThreadLocalRandom.current().nextInt(200, 500));
                String response = "Phản hồi chậm sau khi chờ IO từ " + Thread.currentThread();
                sendResponse(exchange, response);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                String response = "Operation bị gián đoạn";
                sendResponse(exchange, response, 500);
            }
        }
    }
    
    // Handler thực hiện nhiều tác vụ song song sử dụng virtual threads
    static class ParallelTaskHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                // Tạo 5 tác vụ song song và chờ tất cả hoàn thành
                var future1 = executor.submit(() -> simulateTask("Database query", 300));
                var future2 = executor.submit(() -> simulateTask("Payment API call", 200));
                var future3 = executor.submit(() -> simulateTask("Authentication", 150));
                var future4 = executor.submit(() -> simulateTask("Logging", 100));
                var future5 = executor.submit(() -> simulateTask("Notification", 250));
                
                try {
                    String result1 = future1.get();
                    String result2 = future2.get();
                    String result3 = future3.get();
                    String result4 = future4.get();
                    String result5 = future5.get();
                    
                    String response = "Các tác vụ song song đã hoàn thành:\n" +
                            "1. " + result1 + "\n" +
                            "2. " + result2 + "\n" +
                            "3. " + result3 + "\n" +
                            "4. " + result4 + "\n" +
                            "5. " + result5 + "\n" +
                            "Xử lý bởi: " + Thread.currentThread();
                    
                    sendResponse(exchange, response);
                } catch (Exception e) {
                    String response = "Lỗi khi thực hiện tác vụ song song: " + e.getMessage();
                    sendResponse(exchange, response, 500);
                }
            }
        }
        
        private String simulateTask(String taskName, int delayMs) {
            try {
                Thread.sleep(delayMs);
                return taskName + " hoàn thành sau " + delayMs + "ms trên " + Thread.currentThread();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return taskName + " bị gián đoạn";
            }
        }
    }
    
    // Helper method để gửi HTTP response
    private static void sendResponse(HttpExchange exchange, String response) throws IOException {
        sendResponse(exchange, response, 200);
    }
    
    private static void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
} 