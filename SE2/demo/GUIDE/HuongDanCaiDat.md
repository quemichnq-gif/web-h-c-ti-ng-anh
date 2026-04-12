# HƯỚNG DẪN CÀI ĐẶT & CHẠY DỰ ÁN
**Dự án:** LMS (Web Học Tiếng Anh Academic Portal)
**Phiên bản:** 1.2 (Cập nhật 12/04/2026)

---

## 1. TỔNG QUAN HỆ THỐNG
Đây là một hệ thống quản lý học tập chuyên sâu cho việc học Tiếng Anh, bao gồm:
- **Hành chính:** Quản lý khóa học, bài học, người dùng và nhật ký hệ thống (Audit Logs).
- **Học thuật:** Hệ thống bài kiểm tra (Assessment), quản trị lỗi học tập (Error Management) và báo cáo.
- **Tương tác:** Ghi danh (Enrollment), Thông báo (Notifications) và Quản lý hồ sơ cá nhân.

## 2. YÊU CẦU HỆ THỐNG
- **Java:** JDK 17 hoặc cao hơn.
- **Cơ sở dữ liệu:** MySQL 8.0+.
- **Công cụ build:** Maven (đã đính kèm script `./mvnw`).
- **RAM:** Cấu hình đề nghị tối thiểu 4GB.

## 3. CÀI ĐẶT CƠ SỞ DỮ LIỆU
1. Tạo Database trong MySQL *(tùy chọn — ứng dụng sẽ tự tạo nếu chưa có nhờ tham số `createDatabaseIfNotExist=true`)*:
   ```sql
   CREATE DATABASE student_management CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   ```
2. Import cấu trúc bảng *(tùy chọn — ứng dụng dùng `ddl-auto=update` sẽ tự tạo bảng từ Java Entity)*:
   Trong thư mục `DATABASE/`, hãy chạy 13 file SQL theo thứ tự đánh số từ `01` đến `13`.
   - *Lưu ý:* Các file chỉ chứa cấu trúc bảng (`CREATE TABLE`), **không** có dữ liệu mẫu. Dữ liệu mẫu (Seed Data) sẽ được ứng dụng tự động nạp khi khởi chạy lần đầu thông qua `BootstrapConfig`.

## 4. CẤU HÌNH DỰ ÁN
Mở file `src/main/resources/application.properties` để cấu hình các thông số sau:

### KẾT NỐI DATABASE
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/student_management?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
spring.datasource.username=root (hoặc username MySQL của bạn)
spring.datasource.password= (điền mật khẩu MySQL của bạn, mặc định để trống)
```

### CẤU HÌNH UPLOAD FILE (Bài học)
Mặc định bài học sẽ lưu tài liệu và ảnh vào thư mục `uploads/lessons`. Bạn có thể thay đổi đường dẫn tại:
```properties
file.upload-dir=uploads/lessons
```

### CẤU HÌNH EMAIL (Quên mật khẩu)
Nếu muốn sử dụng chức năng reset mật khẩu qua Email, hãy điền thông tin SMTP (Gmail):
```properties
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
```
> **Lưu ý:** Cần sử dụng **App Password** (mật khẩu ứng dụng) của Google, không phải mật khẩu Gmail thông thường. Truy cập [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords) để tạo.

## 5. CHẠY CHƯƠNG TRÌNH
1. Mở Terminal tại thư mục `SE2/demo/`.
2. Chạy lệnh:
   - Windows: `mvnw.cmd spring-boot:run`
   - macOS/Linux: `./mvnw spring-boot:run`
3. Truy cập địa chỉ: `http://localhost:8080`

## 6. THÔNG TIN TÀI KHOẢN MẶC ĐỊNH
Các tài khoản mặc định sẽ được tự động tạo khi ứng dụng khởi chạy lần đầu (thông qua `BootstrapConfig`):
- **Quản trị viên (Admin):** `maitrang` / mật khẩu: `maitrang123`
- **Nhân viên Học vụ (Academic Staff):** `staff` / mật khẩu: `staff123`
- **Sinh viên (Student):** `student1` / mật khẩu: `student123` (có các user từ student1 đến student6)

---
*Ghi chú: Các tài khoản mặc định sẽ được tự động tạo khi ứng dụng khởi chạy lần đầu (thông qua BootstrapConfig). Không cần import dữ liệu thủ công.*
