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
3. [`auth/infrastructure/SecurityConfig`](../src/main/java/com/codesentinel/auth/infrastructure/SecurityConfig.java) khởi tạo `SecurityFilterChain` (đặt trong module `auth` chứ không phải `shared` vì nó phụ thuộc trực tiếp `JwtService`/`UserRepository`):
   - Bật CORS (chi tiết ở [`CorsConfig`](../src/main/java/com/codesentinel/shared/config/CorsConfig.java): chỉ cho phép `http://localhost:5173` và domain Vercel FE).
   - Tắt CSRF.
   - `permitAll()` cho `/api/auth/**` và `/api/scans/**`.
   - `GET`/`POST /api/users` yêu cầu role `ADMIN`.
   - Mọi request khác yêu cầu đã đăng nhập (`authenticated()`).
   - Gắn [`JwtAuthenticationFilter`](../src/main/java/com/codesentinel/auth/infrastructure/JwtAuthenticationFilter.java) trước `UsernamePasswordAuthenticationFilter` để parse access token trên mọi request.
   - Bật `oauth2Login()` (chưa có `successHandler` nối vào JWT nội bộ — vẫn là TODO, xem mục 7).
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
2. Gọi `issueTokens(user)` (helper dùng chung với `refresh()`): sinh **access token** + **refresh token** qua [`JwtService`](../src/main/java/com/codesentinel/auth/application/JwtServiceImpl.java), lưu `refreshToken` vào bảng `refresh_token`.
3. Trả `AuthResponse` (`accessToken`, `refreshToken`, `tokenType = "Bearer"`, `expiresIn`).

### 3.3 Làm mới token — `POST /api/auth/refresh`

```
Client → AuthController.refresh(RefreshTokenRequest{refreshToken})
       → AuthService.refresh()
```

1. Tra `refreshToken` trong bảng `refresh_token`; không tồn tại → `BadRequestException("Invalid refresh token")`.
2. Kiểm tra chưa bị revoke (`stored.isRevoked()`) và chưa hết hạn (`expiryDate`).
3. Verify chữ ký/username qua `jwtService.validateToken()`.
4. **Rotation**: revoke ngay refresh token vừa dùng (`stored.setRevoked(true)`), rồi phát hành cặp access/refresh token **mới** qua `issueTokens()`. Refresh token chỉ dùng được đúng 1 lần — nếu bị đánh cắp và dùng lại sau khi chủ sở hữu đã refresh, request đó sẽ bị từ chối vì token cũ đã revoke.

### 3.4 Đăng xuất — `POST /api/auth/logout`

`AuthService.logout()` tra `refreshToken` trong DB và set `revoked = true`. Từ thời điểm này refresh token không thể dùng để lấy access token mới nữa (access token cũ đã phát hành trước đó vẫn còn hiệu lực cho tới khi hết hạn tự nhiên — hệ thống chưa có cơ chế thu hồi access token giữa chừng, vì access token không được lưu DB, chỉ refresh token mới lưu).

### 3.5 Xác thực access token trên mỗi request

[`JwtAuthenticationFilter`](../src/main/java/com/codesentinel/auth/infrastructure/JwtAuthenticationFilter.java) (`OncePerRequestFilter`, gắn thủ công vào `SecurityFilterChain`, **không** đánh dấu `@Component` để tránh bị Spring Boot tự đăng ký thêm 1 lần nữa ở tầng servlet):

1. Đọc header `Authorization: Bearer <token>`. Không có/không đúng định dạng → bỏ qua, coi như request ẩn danh.
2. `jwtService.isAccessToken(token)` — chặn việc dùng **refresh token** gọi thẳng API như access token (2 loại token phân biệt bằng claim `token_type`).
3. Lấy `username` từ token, load `User` qua `UserRepository`, verify bằng `jwtService.validateToken()`.
4. Nếu hợp lệ: nạp `UsernamePasswordAuthenticationToken(user, null, [ROLE_<user.role>])` vào `SecurityContextHolder` — từ đây `hasRole("ADMIN")` trong `SecurityConfig` mới có dữ liệu để so khớp.
5. Mọi lỗi (token hết hạn, sai chữ ký, user không tồn tại) đều bị nuốt (log debug) chứ không throw — để Spring Security tự quyết định 401/403 theo rule của từng route, thay vì làm sập filter chain.

> Trước refactor: `validateToken()`/`isTokenExpired()` đã có sẵn trong `JwtService` nhưng **không có filter nào gọi** — access token phát hành ra không hề được xác thực khi quay lại, và `/api/users` (yêu cầu `authenticated()`) trên thực tế **không thể gọi được bằng bất kỳ token nào** vì thiếu bước nạp `SecurityContextHolder`. Đã lấp lỗ hổng này.

### 3.6 Phân quyền theo Role

`User` có thêm field `role` (enum [`Role`](../src/main/java/com/codesentinel/auth/domain/Role.java): `USER`, `ADMIN`; mặc định `USER` khi tạo mới qua `@Builder.Default`). `GET`/`POST /api/users` yêu cầu `ROLE_ADMIN`. Vì chưa có API/flow nào tạo tài khoản ADMIN, muốn thử endpoint này cần tự update thủ công trong DB: `UPDATE users SET role = 'ADMIN' WHERE id = ...;`.

> Cột `role` **không** đặt `NOT NULL` ở entity — nếu đặt, `spring.jpa.hibernate.ddl-auto=update` có thể fail khi `ALTER TABLE` thêm cột NOT NULL vào bảng `users` đã có dữ liệu cũ (không có giá trị backfill). User cũ có `role = null` được `JwtAuthenticationFilter` mặc định coi như `USER`.

### 3.7 OAuth2 (Facebook)

`SecurityConfig` bật `oauth2Login()`, cấu hình Facebook nằm ở `application.yml`. Vẫn **chưa có `successHandler`** nối vào `JwtService` để phát hành token nội bộ sau khi login Facebook — đây là việc cần làm thêm khi triển khai social login thật sự.

### 3.8 Quản lý user khác (`/api/users`)

[`auth/api/UserController`](../src/main/java/com/codesentinel/auth/api/UserController.java) — giờ yêu cầu `ROLE_ADMIN` (xem 3.6):
- `POST /api/users` → `UserService.createUser()` — **đã sửa 3 bug**: (1) trước đây không set `username` (cột NOT NULL) nên sẽ lỗi ở DB, (2) không hash password, (3) response map sai field (`username` lấy nhầm từ `fullName`). Giờ dùng chung logic đúng như `AuthServiceImpl`.
- `GET /api/users` → trả `List<UserResponse>` thay vì trả thẳng entity `User` như trước (entity có field `password` đã hash — lộ qua JSON API là lỗi bảo mật đã được sửa).

`UserService` giờ là **interface**, `UserServiceImpl` là implementation — controller phụ thuộc vào abstraction (Dependency Inversion Principle) thay vì class cụ thể như trước.

### 3.9 reCAPTCHA

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
| 15 | `JwtService.validateToken()` tồn tại nhưng không filter nào gọi — access token phát hành ra không hề được xác thực khi quay lại; `/api/users` (`authenticated()`) trên thực tế không gọi được bằng bất kỳ token nào | Thêm `JwtAuthenticationFilter`, gắn vào `SecurityFilterChain` trước `UsernamePasswordAuthenticationFilter` |
| 16 | Refresh token được sinh ra + lưu DB nhưng không có API nào dùng nó — chết ngay từ khi tạo ra | Thêm `POST /api/auth/refresh`, có rotation (refresh token dùng 1 lần) |
| 17 | `RefreshToken.revoked` khai báo sẵn nhưng không nơi nào set `true` — không có cách thu hồi phiên đăng nhập | Thêm `POST /api/auth/logout` |
| 18 | Không có role/authorization model — mọi user (nếu có xác thực) đều ngang quyền nhau | Thêm enum `Role` (`USER`/`ADMIN`) vào `User`; `GET`/`POST /api/users` yêu cầu `ROLE_ADMIN` |
| 19 | Lỗi 401/403 từ Spring Security (filter chain) trả về không cùng format `ApiResponse` với phần còn lại của API | Thêm `RestAuthenticationEntryPoint` (401) + `RestAccessDeniedHandler` (403), gắn qua `.exceptionHandling()` trong `SecurityConfig` |
| 20 | `POST /api/auth/login`, `/register`, `/refresh` không có giới hạn số lần gọi — có thể brute-force password hoặc spam tạo tài khoản | Thêm `RateLimitFilter` (Bucket4j in-memory): tối đa 5 request/phút theo (IP + đường dẫn), vượt quá trả 429 |
| 21 | `Provider`, `UserProvider`, `LoginProvider` (entity + repository cho multi-provider OAuth) và `ApplicationConstant` là code chết — không service/controller nào import, sống sót qua các đợt refactor trước vì bị coi là "thuộc domain hiện có" mà không kiểm tra lại | Xoá toàn bộ 6 file (2 entity + 1 enum + 2 repository + 1 constant class) sau khi xác nhận không nơi nào tham chiếu |

## 7. Việc còn lại — chưa xử lý trong đợt này (cần quyết định thêm)

Đây là những điểm **cố tình chưa động vào** vì cần thông tin/quyết định ngoài phạm vi refactor code (rủi ro cao nếu tự ý đổi):

1. **Secret plaintext trong `application.yml`** (JWT secret, Facebook client-secret, reCAPTCHA secret) — nên chuyển sang biến môi trường như đã làm với `DB_URL/DB_USERNAME/DB_PASSWORD`, nhưng cần bạn tự cấu hình biến môi trường tương ứng trên môi trường deploy (Docker/hosting) trước, nếu không app sẽ không khởi động được.
2. **`oauth2Login()` chưa có `successHandler`** phát hành JWT nội bộ sau khi login Facebook thành công.
3. **reCAPTCHA chưa được enforce ở login** — cần frontend gửi kèm `recaptchaToken` trước khi bật lại kiểm tra này.
4. **Không có cách tạo tài khoản ADMIN đầu tiên** — phải tự `UPDATE users SET role = 'ADMIN'` thủ công trong DB; chưa có quy trình bootstrap admin an toàn (out of scope, cần bạn quyết định cách làm: seed script, CLI riêng, hay endpoint chỉ gọi được 1 lần).
5. **Access token không thể thu hồi giữa chừng** — logout chỉ revoke refresh token; access token cũ (tối đa 15 phút) vẫn dùng được tới khi tự hết hạn, vì access token không lưu DB (thiết kế stateless có đánh đổi này, chấp nhận được với thời hạn ngắn).
6. **Kiến trúc vẫn là 1 monolith duy nhất** (1 jar, 1 database, 1 Dockerfile) — nếu muốn tách microservice thật sự (auth-service / scan-service độc lập, DB riêng, deploy riêng), đây là bước tiếp theo, không nằm trong phạm vi đợt refactor này.
7. **Rate limit hiện là in-memory theo từng instance** (`RateLimitFilter` dùng `ConcurrentHashMap` nội bộ) — nếu sau này scale ngang nhiều instance, mỗi instance đếm giới hạn riêng, không dùng chung. Muốn chính xác tuyệt đối giữa nhiều instance cần chuyển sang backend dùng chung (Redis...).

---

*Tài liệu được cập nhật sau đợt refactor sang modular monolith (SOLID + layered architecture). Khi code thay đổi tiếp, cần cập nhật lại tài liệu này cho khớp.*
