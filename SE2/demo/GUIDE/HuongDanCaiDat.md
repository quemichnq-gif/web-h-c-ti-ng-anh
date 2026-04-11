# HƯỚNG DẪN CÀI ĐẶT CHƯƠNG TRÌNH

Dự án: **Academic Portal (Web Học Tiếng Anh)**
Nền tảng: Java Spring Boot 3

---

## 1. Yêu cầu hệ thống (Prerequisites)
Trước khi cài đặt, hãy đảm bảo máy tính của bạn đã cài đặt các công cụ sau:
*   **Java SDK 17** trở lên.
*   **MySQL Server** (phiên bản 8.0 trở lên).
*   **Maven** (đã tích hợp sẵn trong thư mục dự án qua `mvnw`).
*   Một IDE như **IntelliJ IDEA**, **Eclipse** hoặc **VS Code**.

## 2. Thiết lập Cơ sở dữ liệu (Database Setup)
1.  Mở công cụ quản lý MySQL (như MySQL Workbench, Navicat, hoặc Terminal).
2.  Tạo một database mới có tên là `student_management`.
    ```sql
    CREATE DATABASE student_management;
    ```
3.  Chạy các script SQL trong thư mục `SE2/demo/DATABASE/` theo thứ tự từ `01` đến `08` để khởi tạo cấu trúc bảng và dữ liệu mẫu:
    *   `01_users.sql`
    *   `02_authorities.sql`
    *   ... (cho đến `08_student_results.sql`)

## 3. Cấu hình ứng dụng
Mở file `src/main/resources/application.properties` và chỉnh sửa các thông số kết nối database nếu cần:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/student_management?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Ho_Chi_Minh
spring.datasource.username=root
spring.datasource.password=your_password_here
```
*Lưu ý: Thay `your_password_here` bằng mật khẩu MySQL của bạn.*

## 4. Chạy chương trình
### Cách 1: Sử dụng IntelliJ IDEA (Khuyên dùng)
1.  Mở thư mục `SE2/demo` bằng IntelliJ.
2.  Chờ Maven tải các dependencies.
3.  Tìm file `src/main/java/com/example/demo/Application.java`.
4.  Chuột phải và chọn **Run 'Application'**.

### Cách 2: Sử dụng dòng lệnh (Terminal/Command Prompt)
1.  Di chuyển vào thư mục dự án: `cd SE2/demo`
2.  Chạy lệnh:
    *   Windows: `mvnw.cmd spring-boot:run`
    *   macOS/Linux: `./mvnw spring-boot:run`

## 5. Truy cập ứng dụng
Sau khi chương trình khởi chạy thành công, mở trình duyệt và truy cập:
*   URL: `http://localhost:8080`
*   Tài khoản mặc định (nếu đã chạy file SQL):
    *   Admin: `admin` / `123`
    *   Teacher: `giangvien` / `123`
    *   Student: `sinhvien` / `123` (Kiểm tra trong file `01_users.sql` để biết chính xác tên đăng nhập).

---
*Ghi chú: File này được định dạng Markdown (.md). Bạn có thể sao chép nội dung này vào Microsoft Word hoặc PowerPoint để tạo báo cáo chính thức.*
