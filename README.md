# StudyHub — tài khoản và quản trị

Ứng dụng Android native, Kotlin + XML + Room. Dữ liệu nằm trong `study_manager.db` trên thiết bị; không có API hoặc backend.

## Chạy ứng dụng

1. Mở dự án bằng Android Studio và Sync Gradle.
2. Chạy cấu hình `app` trên máy ảo hoặc điện thoại (Android 7.0 / API 24 trở lên).
3. Lần đầu đăng nhập bằng **admin / admin123**.
4. Admin vào **Quản lý sinh viên** để tạo hồ sơ và tài khoản Student. Đăng xuất rồi dùng tài khoản đó để kiểm tra phân quyền.

Admin mặc định chỉ được khởi tạo nếu chưa tồn tại. Đổi mật khẩu trong Dashboard; mở lại ứng dụng không đặt lại mật khẩu cũ. Không có chức năng xóa Admin mặc định.

## Phạm vi chức năng

- Login / logout, điều hướng theo vai trò Admin hoặc Student.
- SharedPreferences lưu `accountId`, `username`, `role`, `rememberLogin` khi chọn ghi nhớ. Không lưu mật khẩu. Vai trò luôn được đọc lại từ Room, không lấy quyền từ preferences.
- Dashboard Admin hiển thị số sinh viên, số môn học và các thao tác quản trị.
- Sinh viên: thêm, xem chi tiết, sửa, xóa và tìm theo mã, họ tên, email, số điện thoại, tài khoản.
- Môn học: thêm, xem chi tiết, sửa, xóa và tìm theo mã, tên, mô tả, số tín chỉ.
- Tạo Student đồng thời tạo Account có vai trò Student. Sửa có thể đổi username hoặc đặt mật khẩu mới; để trống mật khẩu khi sửa thì giữ mật khẩu cũ. Xóa Student đồng thời xóa Account liên kết.
- Student chỉ xem thông tin cá nhân, đổi mật khẩu và đăng xuất trong phạm vi phần việc này.
- Xác nhận trước khi xóa hoặc đăng xuất. Có kiểm tra bắt buộc, email, số điện thoại, mật khẩu, tín chỉ và dữ liệu trùng.

## Cấu trúc code

Nguồn nằm tại `app/src/main/java/com/thuchanh2/th_mobile/`:

| Thư mục / file | Trách nhiệm |
| --- | --- |
| `data/entity/Account.kt` | Entity Account(id, username, password, role) |
| `data/entity/Student.kt` | Entity Student(id, studentCode, fullname, email, phone, accountId), khóa ngoại và DTO |
| `data/entity/Subject.kt` | Entity Subject(id, subjectCode, subjectName, description, credits) |
| `data/dao/` | SQL `@Query` cho SELECT / INSERT / UPDATE / DELETE; transaction và kiểm tra quyền quản trị |
| `data/AppDatabase.kt` | Database Room, phiên bản 1, singleton; export schema vào `app/schemas/` |
| `auth/` | Băm mật khẩu PBKDF2 với salt ngẫu nhiên và quản lý phiên SharedPreferences |
| `ui/` | Dashboard, danh sách RecyclerView, form, màn hình Student và ViewModel |
| `MainActivity.kt` | Giao diện đăng nhập và chuyển đến màn hình theo vai trò |

Giao diện XML nằm trong `app/src/main/res/layout/`; hỗ trợ cuộn, bàn phím, edge-to-edge và light/dark theme. Các màn hình quản trị không exported và có kiểm tra quyền khi mở. DAO cũng kiểm tra quyền trước khi đọc/ghi dữ liệu quản trị.

## Kiểm tra

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Robolectric kiểm tra truy vấn SQLite qua Room, CRUD, dữ liệu trùng, tài khoản liên kết, phân quyền, đổi mật khẩu, phiên ghi nhớ/đăng xuất và inflate toàn bộ layout. Robolectric chỉ là dependency kiểm thử, không đóng gói trong ứng dụng.

APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

## Ghép phần lịch học sau này

Hiện database chỉ có ba Entity thuộc phần việc: Account, Student, Subject. Khi thêm StudySchedule, thêm Entity/DAO vào cùng AppDatabase, nâng version và viết Migration giữ dữ liệu. Không dùng destructive migration. Các quy tắc xóa sinh viên/môn học khi đã có lịch phải được thống nhất lúc ghép phần này.
