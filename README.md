## Hướng dẫn chạy project

Project backend sử dụng Spring Boot và MySQL. Trước khi chạy backend, cần đảm bảo MySQL đã chạy và database `banking_db` đã được tạo/import.

---

### 1. Yêu cầu trước khi chạy

Cần cài sẵn:

* Java JDK 21
* MySQL Server hoặc XAMPP MySQL
* Git hoặc công cụ giải nén file zip
* Trình duyệt web

Kiểm tra Java:

```bat
java -version
```

Nếu Java đã cài đúng, terminal sẽ hiển thị phiên bản Java.

---

### 2. Mở project

Giải nén file project, sau đó mở CMD hoặc PowerShell tại thư mục backend.

Ví dụ:

```bat
cd C:\Users\aceto\Downloads\bankingapp_backend_with_admin\bankingapp_backend-main
```

Kiểm tra trong thư mục phải có các file như:

```text
gradlew.bat
build.gradle
settings.gradle
banking_db.sql
src
```

---

### 3. Bật MySQL

Nếu dùng XAMPP:

```text
Mở XAMPP Control Panel
→ Start MySQL
```

Kiểm tra MySQL đã chạy chưa:

```bat
netstat -ano | findstr :3306
```

Nếu thấy dòng có chữ `LISTENING` nghĩa là MySQL đang chạy.

Nếu không thấy gì, nghĩa là MySQL chưa chạy hoặc đang chạy ở port khác.

---

### 4. Tạo database

Chạy lệnh:

```bat
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS banking_db;"
```

Nếu MySQL root không có mật khẩu, khi được hỏi password thì bấm Enter.

Nếu dùng XAMPP mặc định, thường username là:

```text
root
```

và password để trống.

---

### 5. Import dữ liệu database

Trong thư mục project, chạy:

```bat
mysql -u root -p banking_db < banking_db.sql
```

Nếu MySQL root không có mật khẩu, khi được hỏi password thì bấm Enter.

Sau khi import xong, database `banking_db` sẽ có các bảng cần thiết cho backend.

---

### 6. Kiểm tra cấu hình database

Mở file:

```text
src/main/resources/application.yml
```

Kiểm tra đoạn cấu hình MySQL:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/banking_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true
    username: root
    password:
```

Nếu MySQL của bạn có mật khẩu, sửa dòng:

```yaml
password:
```

thành:

```yaml
password: mật_khẩu_mysql_của_bạn
```

Ví dụ:

```yaml
password: 123456
```

Nếu MySQL chạy port khác, ví dụ `3307`, sửa URL thành:

```yaml
url: jdbc:mysql://localhost:3307/banking_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true
```

---

### 7. Chạy backend trên Windows CMD

Chạy lần lượt:

```bat
gradlew.bat compileJava
gradlew.bat bootRun
```

Nếu chạy thành công, terminal sẽ hiện log Spring Boot và không báo lỗi `BUILD FAILED`.

---

### 8. Chạy backend trên PowerShell

Nếu dùng PowerShell, chạy:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat bootRun
```

Lưu ý: Trên Windows CMD dùng `gradlew.bat`, còn trên PowerShell thường dùng `.\gradlew.bat`.

---

### 9. Truy cập trang web

Sau khi backend chạy thành công, mở trình duyệt:

```text
http://localhost:8080
```

Trang Admin:

```text
http://localhost:8080/admin
```

Tài khoản admin mặc định:

```text
Username: admin
Password: admin123
```

---

### 10. Chạy bằng port khác nếu cần

Nếu port `8080` bị chiếm, có thể chạy bằng port khác, ví dụ `8081`:

```bat
gradlew.bat bootRun --args="--server.port=8081"
```

Sau đó mở:

```text
http://localhost:8081/admin
```

---

### 11. Lỗi thường gặp

#### Lỗi 1: `'.' is not recognized`

Nguyên nhân: chạy lệnh Linux trên Windows CMD.

Sai:

```bash
./gradlew bootRun
```

Đúng trên Windows CMD:

```bat
gradlew.bat bootRun
```

Đúng trên PowerShell:

```powershell
.\gradlew.bat bootRun
```

---

#### Lỗi 2: `Communications link failure`

Nguyên nhân: backend không kết nối được MySQL.

Cách xử lý:

1. Bật MySQL.
2. Kiểm tra port 3306:

```bat
netstat -ano | findstr :3306
```

3. Kiểm tra username/password trong `application.yml`.
4. Tạo database:

```bat
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS banking_db;"
```

5. Import database:

```bat
mysql -u root -p banking_db < banking_db.sql
```

6. Chạy lại:

```bat
gradlew.bat bootRun
```

---

#### Lỗi 3: `Unable to start embedded Tomcat`

Nguyên nhân có thể do port bị chiếm hoặc lỗi cấu hình khác.

Kiểm tra port 8080:

```bat
netstat -ano | findstr :8080
```

Nếu có tiến trình chiếm port, lấy PID ở cột cuối rồi tắt:

```bat
taskkill /PID <PID> /F
```

Ví dụ:

```bat
taskkill /PID 12345 /F
```

Sau đó chạy lại:

```bat
gradlew.bat bootRun
```

---

#### Lỗi 4: Không tìm thấy lệnh `mysql`

Nếu chạy lệnh `mysql` bị lỗi:

```text
'mysql' is not recognized as an internal or external command
```

nghĩa là MySQL chưa được thêm vào PATH.

Nếu dùng XAMPP, có thể chạy bằng đường dẫn đầy đủ:

```bat
C:\xampp\mysql\bin\mysql.exe -u root -p -e "CREATE DATABASE IF NOT EXISTS banking_db;"
```

Import database:

```bat
C:\xampp\mysql\bin\mysql.exe -u root -p banking_db < banking_db.sql
```

---

### 12. Quy trình chạy nhanh

Nếu đã cài đủ Java và MySQL, chạy nhanh theo thứ tự:

```bat
cd C:\Users\aceto\Downloads\bankingapp_backend_with_admin\bankingapp_backend-main

netstat -ano | findstr :3306

mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS banking_db;"

mysql -u root -p banking_db < banking_db.sql

gradlew.bat bootRun
```

Mở Admin:

```text
http://localhost:8080/admin
```

Đăng nhập:

```text
admin / admin123
```
