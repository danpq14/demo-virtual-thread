# Demo Virtual Thread trong Java

Dự án này minh họa việc sử dụng Virtual Thread trong Java 21 (Project Loom) để xử lý các tác vụ non-blocking hiệu quả.

## Yêu cầu

- Java Development Kit (JDK) 21 trở lên
- Gradle 7.0 trở lên

## Cấu trúc dự án

Dự án bao gồm các thành phần chính sau:

1. **VirtualThreadDemo.java**: Lớp chính so sánh hiệu suất giữa Platform Thread và Virtual Thread, và demo tác vụ IO non-blocking.

2. **HttpServerDemo.java**: Demo một HTTP server đơn giản sử dụng Virtual Thread để xử lý các yêu cầu đồng thời.

3. **DatabaseVirtualThreadDemo.java**: Mô phỏng các tác vụ truy vấn cơ sở dữ liệu đồng thời bằng Virtual Thread.

## Cách chạy demo

Để chạy demo, sử dụng Gradle:

```bash
./gradlew run
```

Hoặc chạy từng lớp riêng biệt:

```bash
# Chạy demo chính
./gradlew runMain --args="vn.vnpay.efin.VirtualThreadDemo"

# Chạy demo HTTP Server
./gradlew runMain --args="vn.vnpay.efin.HttpServerDemo"

# Chạy demo Database
./gradlew runMain --args="vn.vnpay.efin.DatabaseVirtualThreadDemo"
```

## Giải thích về Virtual Thread

### Virtual Thread là gì?

Virtual Thread (Thread ảo) là một tính năng mới trong Java 21, là một phần của [Project Loom](https://openjdk.org/projects/loom/). Đây là một loại thread nhẹ (lightweight thread) được quản lý bởi JVM thay vì hệ điều hành.

### Lợi ích của Virtual Thread

1. **Hiệu suất cao**: Có thể tạo hàng triệu virtual thread mà không gặp vấn đề về tài nguyên hệ thống.
2. **Mô hình lập trình đơn giản**: Sử dụng mô hình lập trình tuần tự quen thuộc thay vì callbacks phức tạp.
3. **Hiệu quả cao với các tác vụ IO-bound**: Tự động giải phóng thread khi gặp hoạt động blocking IO.
4. **Không thay đổi API**: Tích hợp liền mạch với các API thread hiện có trong Java.

### Cách Virtual Thread hoạt động

- **Mounted/Unmounted**: Virtual threads được gắn (mount) và tháo gắn (unmount) từ carrier threads (platform threads) khi cần thiết.
- **Hiệu quả IO**: Khi một virtual thread thực hiện một hoạt động blocking IO, nó tự động nhường carrier thread cho các virtual threads khác.
- **Scheduling**: Virtual threads được lên lịch bởi JVM, không phải hệ điều hành, giúp giảm chi phí context switching.

### Khi nào nên dùng Virtual Thread

- Ứng dụng có nhiều tác vụ đồng thời IO-bound.
- Ứng dụng web và microservices cần xử lý nhiều request đồng thời.
- Tác vụ với thời gian chờ IO dài (database queries, network calls, etc).

### Virtual Thread vs Reactive Programming

- **Virtual Thread**: Mã nguồn tuyến tính và dễ đọc, dễ debug, thích hợp với các nhóm phát triển có kinh nghiệm với mô hình lập trình đồng bộ.
- **Reactive Programming**: Hiệu quả cao về sử dụng tài nguyên, phù hợp với back-pressure, nhưng có đường cong học tập dốc hơn.

## Tài liệu tham khảo

- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [Project Loom](https://openjdk.org/projects/loom/)
- [Virtual Threads in Java](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html) 