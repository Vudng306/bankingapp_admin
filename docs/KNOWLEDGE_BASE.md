# Banking App — Knowledge Base

> Tài liệu này dành cho AI agent / LLM kế tiếp để hiểu toàn bộ dự án và tiếp tục phát triển mà không cần hỏi lại.  
> Cập nhật lần cuối: 2026-06-28

---

## 1. Tổng quan dự án

Ứng dụng ngân hàng di động (backend). Cung cấp REST API cho app Android.

**Tech stack chính xác:**

| Thành phần | Phiên bản |
|-----------|-----------|
| Java | **21** (JDK tại `C:\Program Files\Java\jdk-21`) |
| Spring Boot | **4.1.0** |
| Spring Framework | 7.x |
| Spring Security | 7.x |
| Hibernate ORM | **7.4.1.Final** |
| Jackson | 3.x (`ObjectMapper`) + 2.21 (annotations) |
| Database | **MariaDB** tại `localhost:3306`, user `root`, không có password |
| Build tool | Gradle 9.5.1 |
| JDBC driver | `mysql-connector-j` (tương thích MariaDB) |

**Chạy server (Windows):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\gradlew.bat bootRun
# Nếu port 8080 đang bận:
Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess -Force
```

---

## 2. Những tương thích phá vỡ (QUAN TRỌNG — đừng lặp lại lỗi cũ)

Spring Boot 4.1.0 có nhiều breaking change so với 3.x. Tất cả đã được sửa trong codebase nhưng cần biết để không đưa code cũ vào.

### 2.1 Jackson 3.x — package `ObjectMapper` đã chuyển

```java
// SAI — không tồn tại trong Jackson 3.x
import com.fasterxml.jackson.databind.ObjectMapper;

// ĐÚNG — package mới
import tools.jackson.databind.ObjectMapper;
```

**Lưu ý:** Chỉ `ObjectMapper` bị đổi package. Các annotation (`@JsonInclude`, `@JsonProperty`, v.v.) vẫn ở `com.fasterxml.jackson.annotation` — **không đổi**.

### 2.2 Spring Security 7 — `DaoAuthenticationProvider` mất constructor mặc định

```java
// SAI — constructor rỗng và setUserDetailsService() bị xóa
DaoAuthenticationProvider p = new DaoAuthenticationProvider();
p.setUserDetailsService(uds); // NoSuchMethodException

// ĐÚNG — inject qua constructor
DaoAuthenticationProvider p = new DaoAuthenticationProvider(userDetailsService);
```

### 2.3 Hibernate 7.4 + MariaDB — `@Lock(PESSIMISTIC_WRITE)` sinh SQL sai

Hibernate 7.4 sinh `SELECT ... FOR UPDATE OF alias` — không hợp lệ trên MariaDB:

```java
// SAI — sinh "FOR UPDATE OF a1_0", MariaDB ném SQLSyntaxErrorException
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Account> findById(Long id);

// ĐÚNG — native query
@Query(value = "SELECT * FROM accounts WHERE id = :id FOR UPDATE", nativeQuery = true)
Optional<Account> findByIdForUpdate(@Param("id") Long id);
```

### 2.4 Flyway 12.4.0 — auto-configuration bị phá vỡ trong SB 4.1.0

Flyway auto-config không kích hoạt dù classpath và config đúng — **không có log entry nào**. Giải pháp: tắt Flyway, chạy migration thủ công qua CLI.

```yaml
# application.yml
spring:
  flyway:
    enabled: false
```

Các migration SQL nằm tại `src/main/resources/db/migration/` — cần chạy thủ công khi setup DB mới.

### 2.5 Java toolchain — dùng 21 không phải 17

```groovy
// build.gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21) // không phải 17
    }
}
```

---

## 3. Cấu trúc database

**Flyway đã bị tắt.** Khi setup DB mới, chạy tuần tự 4 file migration:

```
src/main/resources/db/migration/
├── V1__create_tables.sql         -- tạo toàn bộ 7 bảng
├── V2__add_account_version.sql   -- thêm cột version (optimistic lock) vào accounts
├── V3__add_transaction_external_account_name.sql  -- thêm to_external_account_name
└── V4__create_transfer_sessions.sql  -- bảng phiên giao dịch hai bước
```

### Schema tóm tắt

```
users
  id, full_name, email*, phone*, password_hash, pin_hash,
  avatar_url, status(ACTIVE|LOCKED), created_at, updated_at

accounts
  id, user_id→users, account_number*, balance, currency,
  account_type(PAYMENT|SAVINGS), status(ACTIVE|LOCKED),
  version (optimistic lock), created_at

transactions
  id, from_account_id→accounts, to_account_id→accounts,
  to_external_account, to_external_account_name, to_bank_code,
  amount, fee, type(INTERNAL|INTERBANK|TOPUP|SAVINGS_DEPOSIT|SAVINGS_WITHDRAW),
  status(PENDING|SUCCESS|FAILED), description, reference_code*, created_at

otp_codes
  id, user_id→users, code(6 ký tự), purpose(REGISTER|LOGIN|TRANSFER|RESET_PASSWORD),
  channel(SMS|EMAIL), expires_at, is_used, created_at

notifications
  id, user_id→users, title, content, type(TRANSACTION|BALANCE|SYSTEM),
  is_read, created_at

savings
  id, user_id→users, source_account_id→accounts, principal,
  interest_rate, term_months, start_date, maturity_date,
  accrued_interest, status(ACTIVE|MATURED|WITHDRAWN), created_at

devices
  id, user_id→users, device_name, device_id, push_token,
  biometric_enabled, last_login_at, is_active, created_at

transfer_sessions
  id, confirm_token*(UUID), user_id→users, transfer_type(INTERNAL|INTERBANK),
  from_account_id, to_account_number, to_account_name, to_bank_code,
  amount, description, expires_at, used(0|1), created_at
```

`*` = UNIQUE constraint

---

## 4. Cấu trúc package

```
org.nhom8.banking
├── BankingApplication.java
├── common/
│   ├── ApiResponse.java          -- wrapper mọi response { success, message, data }
│   └── BankCode.java             -- enum 12 ngân hàng nội địa (VCB, TCB, MB, ...)
├── config/
│   ├── AsyncConfig.java          -- ThreadPoolTaskExecutor "transferExecutor" cho interbank
│   ├── DataInitializer.java      -- seed data khi DB rỗng (chỉ chạy ngoài profile "test")
│   ├── SecurityConfig.java       -- JWT stateless, public URLs, 401/403 handlers
│   └── WebMvcConfig.java         -- CORS, static resources /uploads/**
├── controller/                   -- REST layer, @AuthenticationPrincipal CustomUserDetails
├── dto/
│   ├── request/                  -- @Valid annotated DTOs
│   └── response/                 -- Builder pattern
├── entity/                       -- JPA entities, enums bên trong entity
├── exception/
│   ├── ErrorCode.java            -- enum tập trung: mã lỗi → HttpStatus + message tiếng Việt
│   ├── AppException.java         -- RuntimeException mang ErrorCode
│   └── GlobalExceptionHandler.java -- @RestControllerAdvice
├── repository/                   -- JpaRepository + custom @Query
├── security/
│   ├── JwtTokenProvider.java     -- HS256, secret từ app.jwt.secret
│   ├── JwtAuthenticationFilter.java
│   ├── CustomUserDetails.java    -- implements UserDetails, giữ userId
│   └── UserDetailsServiceImpl.java
└── service/
    ├── [Interface].java
    └── impl/[Impl].java
```

---

## 5. Map tính năng: đã xong / chưa xong

| Tính năng | Controller | Service | Trạng thái |
|-----------|-----------|---------|-----------|
| Đăng ký (OTP) | `AuthController` | `AuthServiceImpl` | ✅ hoàn chỉnh |
| Đăng nhập (JWT) | `AuthController` | `AuthServiceImpl` | ✅ hoàn chỉnh |
| Quên / đặt lại mật khẩu | `AuthController` | `AuthServiceImpl` | ✅ hoàn chỉnh |
| Hồ sơ, PIN, avatar | `ProfileController` | `ProfileServiceImpl` | ✅ hoàn chỉnh |
| Chuyển tiền nội bộ (2 bước) | `TransferController` | `TransferServiceImpl` | ✅ hoàn chỉnh |
| Chuyển tiền liên ngân hàng (async) | `TransferController` | `TransferServiceImpl` + `InterbankGatewayServiceImpl` | ✅ hoàn chỉnh |
| Lịch sử giao dịch + biên lai | `TransactionController` | `TransactionServiceImpl` | ✅ hoàn chỉnh |
| Dashboard summary | `DashboardController` | `DashboardServiceImpl` | ✅ hoàn chỉnh |
| Thông báo (CRUD) | `NotificationController` | `NotificationServiceImpl` | ✅ hoàn chỉnh |
| Tiết kiệm (Savings) | **CHƯA CÓ** | **CHƯA CÓ** | ⚠️ entity + DB có, logic chưa viết |
| Quản lý thiết bị (Device) | **CHƯA CÓ** | **CHƯA CÓ** | ⚠️ entity + DB có, logic chưa viết |
| Đăng nhập bằng sinh trắc học | **CHƯA CÓ** | **CHƯA CÓ** | ⚠️ trường `biometric_enabled` trong devices |
| SMS OTP | — | `OtpServiceImpl` (stub) | ⚠️ channel SMS có trong enum, chưa tích hợp Twilio |
| Push notification | — | — | ⚠️ `push_token` trong devices, chưa tích hợp FCM |
| Token refresh | — | — | ❌ `refresh_expiration_ms` config có nhưng endpoint chưa viết |

---

## 6. Luồng nghiệp vụ quan trọng

### 6.1 Đăng ký và xác thực OTP

```
POST /auth/register   → tạo User (status=ACTIVE), gửi OTP purpose=REGISTER
POST /auth/verify-otp → verify OTP → trả JWT (user đã active ngay, không cần verify lần 2)
PUT  /profile/pin     → thiết lập PIN (pin_hash null → chỉ cần newPin)
```

Sau đăng ký, user **chưa có PIN** (`pin_hash = null`). Cần thiết lập PIN trước khi chuyển tiền.

### 6.2 Chuyển tiền hai bước (flow chính)

**Bước 1 — `POST /transfers/internal` hoặc `/transfers/interbank`:**
1. `verifyPin()` — tải User, kiểm tra `pin_hash != null`, `BCrypt.matches(pin, pinHash)`
2. Validate tài khoản (sở hữu, ACTIVE, không chuyển cho chính mình, đủ số dư)
3. Lưu `TransferSession` với `confirmToken = UUID`, `expiresAt = now + 5 phút`
4. `otpService.generate(user, TRANSFER, EMAIL)` → OTP mới, invalidate OTP cũ cùng purpose
5. Trả `{ confirmToken, otpResponse: { channel, target, expiresInSeconds, devOtpCode? } }`

**Bước 2 — `POST /transfers/confirm`:**
1. `otpService.verify(user, otpCode, TRANSFER)` — ném `INVALID_OTP` nếu sai/hết hạn
2. `sessionRepository.findByConfirmTokenAndUsedFalse(token)` — `used=true` = đã replay
3. Kiểm tra `session.user.id == userId` và `expiresAt > now`
4. `session.setUsed(true)` ← **đánh dấu ngay trước khi execute** (anti-replay)
5. Gọi `executeInternal()` hoặc `executeInterbank()` theo `session.transferType`

**Anti-replay:** `used=true` được set trước khi tiền được chuyển. Nếu execute thất bại, session đã consumed, không thể replay.

### 6.3 Deadlock prevention trong `executeInternal`

Hai giao dịch đồng thời A→B và B→A sẽ deadlock nếu lock tài khoản khác thứ tự. Fix:

```java
private Account[] lockOrdered(Long idA, Long idB) {
    Long first  = Math.min(idA, idB);  // luôn lock account có ID nhỏ hơn trước
    Long second = Math.max(idA, idB);
    // ...
}
```

`executeInternal` gọi `lockOrdered(fromId, toId)` → không bao giờ deadlock.

### 6.4 Interbank async flow

```
confirmTransfer()
  └─ executeInterbank()
        ├── lock fromAccount FOR UPDATE
        ├── trừ tiền, lưu Transaction(status=PENDING)
        ├── lưu Notification "đang xử lý"
        └── gatewayService.dispatch(txId, fromAccountId, amount)  ← @Async

dispatch() [thread khác — "transferExecutor"]
  ├── Thread.sleep(3000–8000ms)     ← giả lập mạng liên ngân hàng
  ├── random success/fail (85% success theo config)
  └── self.settle(txId, ..., success)  ← gọi qua self-proxy để @Transactional hoạt động

settle() [@Transactional riêng]
  ├── nếu SUCCESS → cập nhật tx.status = SUCCESS
  └── nếu FAILED  → cập nhật tx.status = FAILED + hoàn tiền (FOR UPDATE) + notification
```

**Self-injection pattern** (cần thiết vì `@Async` + `@Transactional` cần đi qua Spring proxy):
```java
@Autowired @Lazy
private InterbankGatewayService self;   // inject chính mình qua interface
// ...
self.settle(...)  // đi qua proxy → @Transactional được apply
```

Frontend polling: sau khi nhận `status=PENDING`, poll `GET /notifications` hoặc `GET /dashboard/summary` mỗi 2–3 giây.

---

## 7. Bảo mật

### JWT
- Algorithm: **HS256**
- Secret: `app.jwt.secret` trong `application.yml` (base64)
- Expiry: 86400000ms = 24 giờ
- Filter: `JwtAuthenticationFilter` → set `SecurityContextHolder` → `@AuthenticationPrincipal CustomUserDetails`

### Public endpoints (không cần JWT)
```
/auth/register, /auth/verify-otp, /auth/login,
/auth/forgot-password, /auth/reset-password, /auth/resend-otp, /error
```

### PIN
- Lưu dưới dạng BCrypt hash trong `users.pin_hash`
- `pin_hash = null` khi chưa thiết lập → `verifyPin()` ném `PIN_NOT_SET`
- Bắt buộc trước khi chuyển tiền

### OTP
- 6 chữ số ngẫu nhiên, TTL 5 phút, lưu trong `otp_codes`
- `otpService.generate()` tự invalidate OTP cũ cùng purpose (tránh nhiều OTP active)
- `app.otp.dev-mode=true` → không gửi email thật, trả `devOtpCode` trong response

---

## 8. Pattern lỗi (ErrorCode)

Tất cả lỗi đi qua `ErrorCode` enum → `AppException` → `GlobalExceptionHandler`:

```java
// Ném lỗi
throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);

// GlobalExceptionHandler trả về:
{ "success": false, "message": "Số dư tài khoản không đủ" }  // HTTP 400
```

**Thêm lỗi mới:** chỉ cần thêm entry vào `ErrorCode.java` với `HttpStatus` và message tiếng Việt.

Lỗi validation (`@Valid`) được handle riêng trong `GlobalExceptionHandler` → trả map `{ field: message }`.

---

## 9. Seed data (test)

| Field | User 1 | User 2 |
|-------|--------|--------|
| Email | `an@banking.com` | `bich@banking.com` |
| Password | `Password123!` | `Password123!` |
| PIN | `123456` | `123456` |
| Account number | `9704001000000001` | `9704001000000002` |
| Balance ban đầu | 50,000,000 VND | 30,000,000 VND |
| Balance hiện tại* | ~49,900,000 VND | ~30,100,000 VND |

*Sau các giao dịch test trong session phát triển. Nếu reset DB sẽ về giá trị ban đầu.

**Seed chỉ chạy khi `users` table rỗng** (`DataInitializer.java`).

---

## 10. Thêm tính năng mới (pattern chuẩn)

### Controller

```java
@RestController
@RequestMapping("/feature")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureService featureService;

    @GetMapping
    public ResponseEntity<ApiResponse<FeatureResponse>> get(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok("message", featureService.get(user.getId())));
    }
}
```

### Service

```java
@Service
@RequiredArgsConstructor
public class FeatureServiceImpl implements FeatureService {

    @Override
    @Transactional(readOnly = true)
    public FeatureResponse get(Long userId) {
        // ...
        throw new AppException(ErrorCode.RESOURCE_NOT_FOUND); // lỗi chuẩn
    }
}
```

### Transaction rules

| Tình huống | Annotation |
|-----------|-----------|
| Chỉ đọc | `@Transactional(readOnly = true)` |
| Ghi thông thường | `@Transactional` |
| Ghi có pessimistic lock | `@Transactional(isolation = Isolation.READ_COMMITTED)` |
| Async (interbank) | `@Async` — **không** có `@Transactional` trên method dispatch, tách ra `settle()` riêng |

### Pessimistic lock (khi nào cần)

Dùng `accountRepository.findByIdForUpdate(id)` khi cần đảm bảo số dư không thay đổi trong transaction. Nếu lock hai account cùng lúc, **luôn lock theo thứ tự ID tăng dần** (xem `lockOrdered`).

---

## 11. Tính năng còn thiếu (ưu tiên phát triển tiếp)

### Savings (tiết kiệm) — cao nhất

Entity `Savings`, bảng `savings`, repository `SavingsRepository` đều tồn tại. Cần viết:
- `SavingsService` + `SavingsServiceImpl`: mở sổ, xem danh sách, tất toán khi đáo hạn
- `SavingsController`: CRUD endpoints
- Logic rút sớm (trước `maturity_date`): mất lãi hoặc phạt tùy chính sách
- Tính lãi tự động (`accrued_interest`): có thể dùng `@Scheduled` cron hàng ngày

### Token Refresh

`app.jwt.refresh-expiration-ms: 604800000` (7 ngày) đã config nhưng chưa có endpoint. Cần:
- Lưu `refresh_token` (DB hoặc Redis)
- `POST /auth/refresh` → trả `accessToken` mới

### Device Management

Bảng `devices` và entity `Device` đã có. Cần:
- `POST /devices` — đăng ký thiết bị (khi đăng nhập)
- `GET /devices` — danh sách thiết bị
- `DELETE /devices/{id}` — thu hồi thiết bị

### Biometric login

Field `biometric_enabled` trong `devices`. Flow: thiết bị generate key pair → backend lưu public key → xác thực bằng challenge-response (không qua mật khẩu).

### Chuyển tiền nhanh (QR / số điện thoại)

Hiện tại chuyển tiền nội bộ yêu cầu `accountNumber`. Cần thêm lookup:
- `GET /accounts/lookup?phone=...` → trả `{ accountNumber, ownerName }` để frontend điền sẵn

---

## 12. Config environment

| Key | Default | Mô tả |
|-----|---------|-------|
| `app.otp.dev-mode` | `true` | `true` = không gửi email, trả `devOtpCode` |
| `app.otp.expiry-minutes` | `5` | TTL OTP (phút) |
| `app.interbank.success-rate` | `85` | % thành công giả lập interbank |
| `app.interbank.delay-min-ms` | `3000` | Độ trễ tối thiểu interbank (ms) |
| `app.interbank.delay-max-ms` | `8000` | Độ trễ tối đa interbank (ms) |
| `app.upload.dir` | `uploads` | Thư mục lưu avatar |
| `MAIL_USERNAME` | `leanhtuan7126@gmail.com` | SMTP username (env var) |
| `MAIL_PASSWORD` | `your-app-password` | Gmail App Password (env var) |
| `UPLOAD_DIR` | `uploads` | Override thư mục upload (env var) |

Khi `dev-mode=false`, phải set `MAIL_USERNAME` và `MAIL_PASSWORD` đúng để OTP gửi được qua email.

---

## 13. Endpoint index nhanh

| Method | Path | Auth | Tác dụng |
|--------|------|------|---------|
| POST | `/auth/register` | — | Đăng ký, nhận OTP |
| POST | `/auth/verify-otp` | — | Xác thực OTP, nhận JWT |
| POST | `/auth/login` | — | Đăng nhập (email hoặc phone) |
| POST | `/auth/forgot-password` | — | Gửi OTP reset password |
| POST | `/auth/reset-password` | — | Đặt lại password bằng OTP |
| POST | `/auth/resend-otp` | — | Gửi lại OTP (REGISTER\|RESET_PASSWORD) |
| PUT | `/auth/password` | JWT | Đổi password ← dùng `/profile/password` thay thế |
| PUT | `/auth/pin` | JWT | Set PIN lần đầu ← dùng `/profile/pin` thay thế |
| PUT | `/auth/pin/change` | JWT | Đổi PIN ← dùng `/profile/pin` thay thế |
| GET | `/profile` | JWT | Lấy thông tin user + danh sách account |
| PUT | `/profile/password` | JWT | Đổi password |
| PUT | `/profile/pin` | JWT | Set/đổi PIN (tự phân biệt lần đầu / thay đổi) |
| POST | `/profile/avatar` | JWT | Upload ảnh đại diện (multipart) |
| GET | `/dashboard/summary` | JWT | Accounts + số dư + 5 giao dịch gần nhất + unread count |
| POST | `/transfers/internal` | JWT | Bước 1: chuyển nội bộ (PIN → OTP) |
| POST | `/transfers/interbank` | JWT | Bước 1: chuyển liên NH (PIN → OTP) |
| POST | `/transfers/confirm` | JWT | Bước 2: xác nhận OTP → thực thi |
| GET | `/transactions/history` | JWT | Lịch sử (`accountId`, `page`, `size`, `type`) |
| GET | `/transactions/{id}/receipt` | JWT | Biên lai chi tiết |
| GET | `/notifications` | JWT | Danh sách thông báo (`page`, `size`) |
| GET | `/notifications/unread-count` | JWT | Số thông báo chưa đọc |
| PATCH | `/notifications/{id}/read` | JWT | Đánh dấu đã đọc |
| PATCH | `/notifications/read-all` | JWT | Đánh dấu tất cả đã đọc |
