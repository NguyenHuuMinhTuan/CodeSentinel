# CodeSentinel — Tài liệu luồng xử lý (User Guide)

> Tài liệu này mô tả flow hiện tại của source code trong `src/main/java/com/codesentinel`, sau đợt refactor sang **modular monolith** theo layered architecture (`api → application → domain → infrastructure`) áp dụng các nguyên tắc SOLID. Đây vẫn là một backend duy nhất (không phải microservice thật sự) nhưng ranh giới giữa các module (`auth`, `scan`, `shared`) đã rõ ràng, thuận lợi nếu sau này muốn tách thành service độc lập.

## 1. Tổng quan kiến trúc

Ứng dụng **Spring Boot** (Java 21, Gradle), gồm 3 module domain:

| Module | Package | Vai trò |
|---|---|---|
| `auth` | `com.codesentinel.auth.*` | Đăng ký / đăng nhập / quản lý user, phát hành & xác thực JWT, OAuth2 |
| `scan` | `com.codesentinel.scan.*` | Nhận file/zip/rar upload, giải nén an toàn, quét mã độc bằng nhiều detector |
| `shared` | `com.codesentinel.shared.*` | Thành phần dùng chung: cấu hình Security/CORS/PasswordEncoder, response chuẩn hoá, exception handler, logging |

Mỗi module domain (`auth`, `scan`) được tổ chức theo 4 lớp:

```
<module>/
├── api/             Controller (REST endpoint)
├── application/     Business logic — khai báo interface + impl (DIP)
├── domain/          Entity / model / enum / thuật toán lõi (không phụ thuộc framework I/O)
└── infrastructure/  Repository JPA, cấu hình đọc từ properties, I/O (file, archive...)
```

Entry point: [CodeSentinelApplication.java](../src/main/java/com/codesentinel/CodeSentinelApplication.java).

Mọi request đi qua `RequestLoggingFilter` trước khi tới controller; mọi exception được `GlobalExceptionHandler` bọc lại thành `ApiResponse` chuẩn — **tất cả 3 controller** (`AuthController`, `UserController`, `ScanController`) giờ trả về cùng một định dạng `ApiResponse<T>`.

---

## 2. Luồng khởi động ứng dụng

1. `CodeSentinelApplication.main()` chạy `SpringApplication.run(...)`.
2. Spring nạp cấu hình từ [application.yml](../src/main/resources/application.yml) và [application.properties](../src/main/resources/application.properties): JWT secret/expiration, OAuth2 Facebook, datasource, Google reCAPTCHA.
3. [`shared/config/SecurityConfig`](../src/main/java/com/codesentinel/shared/config/SecurityConfig.java) khởi tạo `SecurityFilterChain`:
   - Bật CORS (chi tiết ở [`CorsConfig`](../src/main/java/com/codesentinel/shared/config/CorsConfig.java): chỉ cho phép `http://localhost:5173` và domain Vercel FE).
   - Tắt CSRF.
   - `permitAll()` cho `/api/auth/**` và `/api/scans/**`; mọi request khác yêu cầu authenticated.
   - Bật `oauth2Login()` (chưa có `successHandler` nối vào JWT nội bộ — vẫn là TODO, xem mục 6).
4. `ScanEngine` (`@Component` trong `scan/domain/engine`) được Spring khởi tạo bằng cách inject **toàn bộ danh sách bean implements `ScanDetector`** — log số lượng & tên detector khi start.

> Đã xoá: `ApplicationInit` (in JWT secret ra console log — rò rỉ thông tin nhạy cảm) và cơ chế `DataBuffer` (một map tĩnh in-memory chỉ được ghi, không nơi nào đọc lại — chết hoàn toàn). `JwtConfig`/`OAuthConfig` vẫn là Spring bean, inject trực tiếp nơi cần dùng.

---

## 3. Luồng xác thực người dùng (Auth flow) — module `auth`

Controller: [`auth/api/AuthController`](../src/main/java/com/codesentinel/auth/api/AuthController.java) — base path `/api/auth`, permitAll.

### 3.1 Đăng ký — `POST /api/auth/register`

```
Client → AuthController.register(RegisterRequest)
       → AuthService.register()   (interface, impl: AuthServiceImpl)
```

Xử lý trong [`AuthServiceImpl.register()`](../src/main/java/com/codesentinel/auth/application/AuthServiceImpl.java):
1. Kiểm tra `username`/`email` đã tồn tại → ném `BadRequestException` (400) nếu trùng.
2. Build entity `User`, hash password bằng `PasswordEncoder` (BCrypt, cấu hình ở [`PasswordConfig`](../src/main/java/com/codesentinel/shared/config/PasswordConfig.java)), set `active = true`.
3. Lưu qua `UserRepository` (JPA, bảng `users`).
4. Trả `UserResponse` (không có password) bọc trong `ApiResponse.success(...)`.

### 3.2 Đăng nhập — `POST /api/auth/login`

```
Client → AuthController.login(LoginRequest)
       → AuthService.login()
```

1. Tìm user theo `email`; sai email hoặc password → `BadRequestException("Email hoặc password không đúng")`.
2. Sinh **access token** + **refresh token** qua [`JwtService`](../src/main/java/com/codesentinel/auth/application/JwtServiceImpl.java).
3. Lưu `refreshToken` vào bảng `refresh_token`.
4. Trả `AuthResponse` (`accessToken`, `refreshToken`, `tokenType = "Bearer"`, `expiresIn`).

### 3.3 Sinh & xác thực JWT

[`JwtServiceImpl`](../src/main/java/com/codesentinel/auth/application/JwtServiceImpl.java) dùng `jjwt`, ký HMAC-SHA với secret từ `JwtConfig`. Access token hết hạn sau 15 phút, refresh token sau 30 ngày (cấu hình trong `application.yml`). Tên các claim (`user_id`, `username`, `token_type`) giờ chỉ khai báo **một nơi duy nhất**: [`auth/infrastructure/JwtClaim`](../src/main/java/com/codesentinel/auth/infrastructure/JwtClaim.java) — trước refactor có 2 class trùng tên ở 2 package khác nhau, đã gộp lại.

> `validateToken()`/`isTokenExpired()` đã có sẵn trong `JwtService` nhưng **chưa có filter nào gọi để xác thực access token** trên request — vì hiện tại mọi route đều `permitAll()` nên chưa phát sinh vấn đề, nhưng cần bổ sung một `OncePerRequestFilter` xác thực JWT nếu sau này có route yêu cầu đăng nhập.

### 3.4 OAuth2 (Facebook)

`SecurityConfig` bật `oauth2Login()`, cấu hình Facebook nằm ở `application.yml`. Vẫn **chưa có `successHandler`** nối vào `JwtService` để phát hành token nội bộ sau khi login Facebook — đây là việc cần làm thêm khi triển khai social login thật sự.

### 3.5 Quản lý user khác (`/api/users`)

[`auth/api/UserController`](../src/main/java/com/codesentinel/auth/api/UserController.java):
- `POST /api/users` → `UserService.createUser()` — **đã sửa 3 bug**: (1) trước đây không set `username` (cột NOT NULL) nên sẽ lỗi ở DB, (2) không hash password, (3) response map sai field (`username` lấy nhầm từ `fullName`). Giờ dùng chung logic đúng như `AuthServiceImpl`.
- `GET /api/users` → trả `List<UserResponse>` thay vì trả thẳng entity `User` như trước (entity có field `password` đã hash — lộ qua JSON API là lỗi bảo mật đã được sửa).

`UserService` giờ là **interface**, `UserServiceImpl` là implementation — controller phụ thuộc vào abstraction (Dependency Inversion Principle) thay vì class cụ thể như trước.

### 3.6 reCAPTCHA

[`RecaptchaService`](../src/main/java/com/codesentinel/auth/application/RecaptchaService.java) vẫn tồn tại và verify được token qua Google API, nhưng **vẫn chưa được gọi từ `login()`** (field `recaptchaToken` trong `LoginRequest` bị bỏ ở bản gốc). Không tự động bật lại trong đợt refactor này vì cần phối hợp với frontend (frontend hiện không gửi token) — cân nhắc bật khi frontend sẵn sàng.

---

## 4. Luồng quét mã độc (Scan flow) — module `scan`

Controller: [`scan/api/ScanController`](../src/main/java/com/codesentinel/scan/api/ScanController.java) — `POST /api/scans/upload` (multipart, field `file`), permitAll. Response giờ bọc trong `ApiResponse<ScanSummary>` (trước đây trả thẳng `ScanSummary`, không nhất quán với 2 controller kia).

```
Client uploads file
   → ScanController.uploadZip(MultipartFile)
   → ScanServiceImpl.scanZip(file)      [orchestrator — chỉ điều phối, không tự làm hết việc]
        1. UploadValidator.validate()          // kiểm tra size/extension
        2. storeToTempFile()                   // lưu tạm với tên UUID_random
        3. ArchiveExtractor.extract()           // giải nén ĐÚNG 1 LẦN, tự chọn zip/rar
        4. FileRiskScanner.scanDangerousFiles() // liệt kê file đuôi nguy hiểm
        5. ScanEngine.scan()                    // chạy tất cả detector
        6. ScanReportBuilder.build()            // liệt kê file + thống kê extension
        7. build ScanSummary                    // response cuối cùng
        finally: TempWorkspaceCleaner.cleanup() // luôn dọn file tạm dù có lỗi hay không
```

`ScanServiceImpl` trước refactor làm tất cả các việc trên trong một class (~400 dòng, vi phạm Single Responsibility Principle). Giờ mỗi bước là một collaborator riêng, được inject qua constructor — `ScanServiceImpl` chỉ còn vai trò điều phối.

### 4.1 Validate upload

[`UploadValidator`](../src/main/java/com/codesentinel/scan/application/UploadValidator.java): không cho file rỗng, giới hạn **100MB**, extension phải nằm trong whitelist (`zip, rar, java, js, py, php, html, xml, json, yml, yaml, txt`). Sai → `BadRequestException` → 400.

### 4.2 Giải nén (đã sửa bug giải nén 2 lần)

**Trước refactor**: code gọi `ArchiveUtil.extract()` rồi gọi tiếp `ZipUtil.extractZip()` lên cùng một file — dư thừa, và không nhất quán với `.rar` (`ZipUtil` chỉ hiểu `ZipInputStream`).

**Sau refactor**: một class duy nhất [`ArchiveExtractor`](../src/main/java/com/codesentinel/scan/infrastructure/ArchiveExtractor.java) tự chọn đúng 1 nhánh xử lý theo đuôi file:
- `.zip` → giải nén bằng `ZipInputStream`, có chống **Zip Slip** (`targetPath.startsWith(destPath)` sau `normalize()`) và **Zip Bomb** (tối đa 10.000 entry, tối đa 50MB/file).
- `.rar` → giải nén bằng thư viện `junrar`, cũng được bổ sung kiểm tra Zip Slip (bản gốc chưa có).
- Đuôi khác → ném lỗi `Unsupported archive format`.

### 4.3 Quét file nguy hiểm theo phần mở rộng

[`FileRiskScanner`](../src/main/java/com/codesentinel/scan/infrastructure/FileRiskScanner.java) (đổi tên từ `FileRiskUtil`, giờ là Spring bean thay vì static util để dễ test/mock) đệ quy thư mục đã giải nén, gắn cờ file đuôi `.exe .bat .sh .ps1 .dll`.

### 4.4 Engine quét nội dung (phần lõi phát hiện)

[`ScanEngine`](../src/main/java/com/codesentinel/scan/domain/engine/ScanEngine.java) duyệt đệ quy cây thư mục, với mỗi file có đuôi "đọc được" (`.java .js .ts .py .php .txt .html .xml .json .yml .yaml .properties .sh .bat`) chạy **tuần tự tất cả bean `ScanDetector`** đã được Spring inject (Open/Closed Principle: thêm detector mới chỉ cần thêm 1 class `@Component implements ScanDetector`, không sửa `ScanEngine`).

Danh sách detector (`scan/domain/detector/`):

| Detector | Cơ chế phát hiện | `FindingType` |
|---|---|---|
| `KeywordDetector` | Chuẩn hoá "aggressive" rồi tìm keyword nguy hiểm (`processbuilder`, `cmd.exe`, `powershell`, `eval(`...) | `DANGEROUS_KEYWORD` (HIGH) |
| `Base64Detector` | Tìm chuỗi base64 trong dấu nháy, decode thử, flag nếu nội dung giải mã chứa keyword nguy hiểm | `BASE64_OBFUSCATION` (HIGH) |
| `NetworkDetector` | Chuẩn hoá + decode base64 toàn file, so khớp pattern mạng (URL, webhook, ngrok, socket...) | `NETWORK_ACCESS` (LOW/MEDIUM/HIGH) |
| `ObfuscationDetector` | Unicode vô hình, hex-escape liên tiếp, chuỗi base64-like dài, nối chuỗi quá nhiều | `INVISIBLE_UNICODE`, `HEX_OBFUSCATION`, `SUSPICIOUS_RANDOM_STRING`, `STRING_OBFUSCATION` |
| `ReflectionDetector` | Pattern reflection Java + chain scoring khi nhiều pattern xuất hiện cùng lúc | `REFLECTION_ABUSE`, `REFLECTION_CHAIN` |
| `RegexDetector` | URL nghi vấn, Discord webhook, địa chỉ IP | `SUSPICIOUS_URL` (MEDIUM), `DISCORD_WEBHOOK` (HIGH), `IP_ADDRESS` (LOW) |

**Thay đổi quan trọng**: `Finding.type` và `Finding.severity` trước đây là `String` tự do (magic string, dễ typo) — giờ là enum [`FindingType`](../src/main/java/com/codesentinel/scan/domain/enums/FindingType.java) và [`SeverityLevel`](../src/main/java/com/codesentinel/scan/domain/enums/SeverityLevel.java), Jackson vẫn serialize ra JSON dạng string như cũ nên **không đổi hợp đồng API** cho FE. Các field `riskScore`, `category`, `recommendation`, `cwe` trong `Finding` đã bị xoá vì được khai báo nhưng chưa từng có detector nào gán giá trị (luôn `null` trong response cũ).

Đồng thời đã xoá `RuleRegistry`/`DetectionRule` (package `scan.rule` cũ) và `FileWalker`/`KeywordDetectUtil` (package `scan.util` cũ) — toàn bộ là code chết, không được `ScanEngine` hay bất kỳ nơi nào khác gọi tới, trùng lặp chức năng với `KeywordDetector`/`RegexDetector` đang chạy thật.

### 4.5 Tổng hợp kết quả & dọn dẹp

[`ScanReportBuilder`](../src/main/java/com/codesentinel/scan/application/ScanReportBuilder.java) duyệt lại cây thư mục để build `FileInfo` + thống kê theo extension (tách khỏi `ScanServiceImpl` theo SRP).

Response cuối [`ScanSummary`](../src/main/java/com/codesentinel/scan/domain/model/ScanSummary.java) — cấu trúc JSON giữ nguyên như trước, chỉ khác là giờ nằm trong `ApiResponse.data`:
```json
{
  "success": true,
  "message": "Scan completed successfully",
  "data": {
    "totalFiles": 12,
    "extensions": { "java": 8, "xml": 4 },
    "files": [ { "fileName": "...", "extension": "...", "path": "..." } ],
    "suspiciousFiles": [ "/tmp/.../malware.exe" ],
    "findings": [ { "file": "...", "line": 10, "type": "DANGEROUS_KEYWORD", "severity": "HIGH", "...": "..." } ]
  },
  "timestamp": "..."
}
```

[`TempWorkspaceCleaner`](../src/main/java/com/codesentinel/scan/infrastructure/TempWorkspaceCleaner.java) luôn dọn file zip/rar tạm + thư mục giải nén trong khối `finally`, kể cả khi có exception.

---

## 5. Logging & xử lý lỗi chung (`shared`)

- [`RequestLoggingFilter`](../src/main/java/com/codesentinel/shared/logging/RequestLoggingFilter.java): log mọi request (method, URI, IP, content-type, params, thông tin file multipart, body, status code).
- [`GlobalExceptionHandler`](../src/main/java/com/codesentinel/shared/exception/GlobalExceptionHandler.java) (`@RestControllerAdvice`) bắt: `MethodArgumentNotValidException` (400 + danh sách lỗi field), `RuntimeException` nghiệp vụ (400, bao gồm `BadRequestException`), `Exception` hệ thống còn lại (500).
- [`ApiResponse<T>`](../src/main/java/com/codesentinel/shared/exception/ApiResponse.java): **toàn bộ 3 controller** giờ dùng chung định dạng này (trước refactor `ScanController` trả thẳng object, không đồng nhất).

---

## 6. Các vấn đề đã sửa trong đợt refactor này

| # | Vấn đề (bản cũ) | Đã xử lý |
|---|---|---|
| 1 | 2 class `JwtClaim` trùng tên ở 2 package khác nhau | Gộp thành 1 nguồn duy nhất: `auth/infrastructure/JwtClaim` |
| 2 | `ApplicationInit` in JWT secret ra console/log khi start | Xoá hẳn class này + cơ chế `DataBuffer` (chết, write-only) |
| 3 | `UserService.createUser()`: thiếu set `username`, không hash password, map response sai field | Viết lại đúng, tách interface/impl, dùng chung `PasswordEncoder` |
| 4 | `GET /api/users` trả thẳng entity `User` (lộ password hash) | Map sang `List<UserResponse>` |
| 5 | `UserServiceImpl.java` cũ là class rỗng, không dùng | Xoá; interface `UserService` + impl thật được đặt đúng vị trí |
| 6 | `ScanServiceImpl` giải nén 2 lần (`ArchiveUtil` + `ZipUtil`), không nhất quán với `.rar` | Gộp thành 1 class `ArchiveExtractor`, tự chọn đúng nhánh xử lý |
| 7 | `ScanController` trả response không bọc `ApiResponse` như 2 controller còn lại | Đồng nhất bằng `ApiResponse<ScanSummary>` |
| 8 | `RuleRegistry`/`DetectionRule`, `FileWalker`, `KeywordDetectUtil`, `GlobalLogger`, `LogUtil`, `ErrorResponse` là code chết, không nơi nào gọi | Xoá toàn bộ |
| 9 | Magic string cho `type`/`severity` trong `Finding`, dễ typo, 2 enum khai báo nhưng không dùng | Chuyển sang dùng `FindingType`/`SeverityLevel` enum thật sự trong mọi detector |
| 10 | `ScanServiceImpl` vi phạm SRP (validate + extract + scan + build report + cleanup trong 1 class) | Tách thành `UploadValidator`, `ArchiveExtractor`, `ScanReportBuilder`, `TempWorkspaceCleaner`; `ScanServiceImpl` chỉ điều phối |
| 11 | `UserController`/controller khác phụ thuộc class cụ thể thay vì interface (vi phạm DIP) | Toàn bộ service đều có interface riêng, controller chỉ biết interface |
| 12 | Cấu trúc thư mục lộn xộn: `config/`, `common/`, `security/`, `user/`, `scan/{controller,service,util,rule,...}` rải rác không theo layer nào | Tổ chức lại thành `auth/`, `scan/`, `shared/`, mỗi module theo `api → application → domain → infrastructure` |
| 13 | `application.properties` khai báo trùng Facebook client-id/secret với `application.yml`, và có 3 key chết (`jwt.secret`, `google.client-id`, `google.client-secret`) không được đọc bởi bất kỳ `@ConfigurationProperties` nào | Xoá các key trùng/chết |
| 14 | Package test `com.example.CodeSentinel` không khớp package chính `com.codesentinel` | Sửa về đúng `com.codesentinel` |

## 7. Việc còn lại — chưa xử lý trong đợt này (cần quyết định thêm)

Đây là những điểm **cố tình chưa động vào** vì cần thông tin/quyết định ngoài phạm vi refactor code (rủi ro cao nếu tự ý đổi):

1. **Secret plaintext trong `application.yml`** (JWT secret, Facebook client-secret, reCAPTCHA secret) — nên chuyển sang biến môi trường như đã làm với `DB_URL/DB_USERNAME/DB_PASSWORD`, nhưng cần bạn tự cấu hình biến môi trường tương ứng trên môi trường deploy (Docker/hosting) trước, nếu không app sẽ không khởi động được.
2. **`oauth2Login()` chưa có `successHandler`** phát hành JWT nội bộ sau khi login Facebook thành công.
3. **Chưa có filter xác thực JWT** trên request (hiện chưa cần vì mọi route đều `permitAll()`, nhưng sẽ cần khi có route yêu cầu đăng nhập thật).
4. **reCAPTCHA chưa được enforce ở login** — cần frontend gửi kèm `recaptchaToken` trước khi bật lại kiểm tra này.
5. **Kiến trúc vẫn là 1 monolith duy nhất** (1 jar, 1 database, 1 Dockerfile) — nếu muốn tách microservice thật sự (auth-service / scan-service độc lập, DB riêng, deploy riêng), đây là bước tiếp theo, không nằm trong phạm vi đợt refactor này.

---

*Tài liệu được cập nhật sau đợt refactor sang modular monolith (SOLID + layered architecture). Khi code thay đổi tiếp, cần cập nhật lại tài liệu này cho khớp.*
