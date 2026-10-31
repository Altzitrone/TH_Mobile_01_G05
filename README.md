# StudyHub — quản lý và đặt lịch học

Ứng dụng Android native dùng Kotlin, giao diện XML và Room. Các phần quản trị, lịch học, calendar và thông báo cùng sử dụng **một database**: `study_manager.db`.

## Chạy và kiểm tra chức năng

1. Mở dự án trong Android Studio, Sync Gradle và Run `app` trên Android API 24 trở lên.
2. Lần đầu đăng nhập **admin / admin123**. Có thể đổi mật khẩu tại Dashboard; mở lại app không đặt lại mật khẩu.
3. Admin thêm môn học rồi thêm sinh viên kèm tên đăng nhập và mật khẩu Student.
4. Đăng xuất, đăng nhập bằng tài khoản Student vừa tạo.
5. Chọn **Đặt lịch học**, chọn môn trong danh sách, ngày, giờ bắt đầu/kết thúc và địa điểm.
6. Kiểm tra tìm kiếm, sửa/xóa, xác nhận/hủy lịch, các bộ lọc trạng thái.
7. Xem cùng các lịch đó tại Calendar theo ngày, tuần hoặc tháng. Tuần bắt đầu từ thứ Hai.
8. Xem thông báo thay đổi lịch tại tab Thông báo; có bộ lọc và đánh dấu đã đọc.
9. Trên Android 13 trở lên, cho phép thông báo khi được hỏi để nhận thông báo hệ thống.

Dữ liệu khởi tạo chỉ có Admin. Không tự tạo tài khoản Student, môn học hoặc lịch mẫu mang ID cố định.

## Đối chiếu yêu cầu đề bài

| Yêu cầu | Thực hiện |
| --- | --- |
| Login / Logout, Admin / Student | Kiểm tra tài khoản trong Room; bảo vệ màn hình và DAO theo vai trò |
| Ghi nhớ đăng nhập | SharedPreferences lưu ID, username, role khi chọn ghi nhớ; quyền thực tế đọc lại từ Room |
| Student Management | Admin thêm/xem/sửa/xóa/tìm; xử lý Account liên kết trong transaction |
| Subject Management | Admin thêm/xem/sửa/xóa/tìm |
| Dashboard Admin | Số lượng sinh viên, môn học, điều hướng quản trị và đổi mật khẩu |
| Đặt và quản lý lịch cá nhân | Chọn môn, ngày, giờ, địa điểm; thêm/xem/sửa/xóa/tìm |
| Kiểm tra trùng lịch | Transaction trong DAO; cùng sinh viên, cùng ngày, các khoảng giờ giao nhau; bỏ qua lịch Cancelled |
| Trạng thái | Pending, Confirmed, Cancelled; xác nhận lại cũng kiểm tra trùng lịch |
| Calendar | Ngày / tuần / tháng, dữ liệu thật của tài khoản đang đăng nhập |
| Thông báo | Lưu thay đổi lịch và lời nhắc; thông báo hệ thống khi có quyền; khôi phục lịch nhắc sau khởi động máy / cập nhật app |

Hai buổi nối tiếp như 08:00–10:00 và 10:00–11:00 được phép. Lịch đã hủy không giữ chỗ. Sinh viên không đọc hoặc sửa lịch của sinh viên khác.

## Dữ liệu và cấu trúc

- `data/entity/`: bốn Entity chính `Account`, `Student`, `Subject`, `StudySchedule`; Entity phụ `NotificationItem` lưu lịch sử/thông báo đã đọc.
- `data/model/StudySchedule.kt`: DTO kết quả JOIN môn học với lịch, không phải một bộ Entity thứ hai.
- `data/dao/`: truy vấn SQL qua `@Query`, transaction và kiểm tra quyền.
- `data/AppDatabase.kt`: Room duy nhất, version 3; migration 1 → 2 → 3 giữ dữ liệu quản trị/lịch học và xử lý cả hai cấu trúc version 2 sau merge.
- `auth/`: PBKDF2 với salt và SharedPreferences. Không lưu mật khẩu thô vào preferences.
- `ui/`, `StudyScheduleActivity.kt`: giao diện quản trị, Student, calendar và thông báo.
- `notifications/`: lịch nhắc bằng AlarmManager và BroadcastReceiver.
- `app/schemas/`: schema Room đã export để quản lý migration.

ID Account và ID Student là hai khóa khác nhau. Mọi chức năng cá nhân đều xác định Student bằng `Student.accountId` của tài khoản đăng nhập.

Xóa sinh viên sẽ xóa tài khoản, lịch và thông báo liên quan. Môn đang có lịch học được bảo vệ bằng khóa ngoại RESTRICT; cần xử lý các lịch liên quan trước khi xóa môn.

## Nhắc lịch

Ứng dụng lên lịch nhắc khoảng 15 phút trước buổi học. AlarmManager dùng alarm không yêu cầu quyền exact alarm; Android có thể trì hoãn theo cơ chế tiết kiệm pin. Quyền thông báo bị từ chối không ngăn đặt lịch hoặc xem hộp thông báo trong ứng dụng.

Thông báo hệ thống gắn với tài khoản đang sử dụng ứng dụng. Đóng tiến trình không tự khôi phục một phiên đăng nhập không chọn ghi nhớ, nhưng vẫn giữ danh tính nhận lời nhắc; đăng xuất sẽ xóa cả hai thông tin đó. Thao tác đọc dữ liệu vẫn luôn kiểm tra quyền từ database.

## Kiểm thử

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Bộ test Robolectric kiểm tra CRUD, đăng nhập/phân quyền, SharedPreferences, các dạng trùng lịch, transaction đặt đồng thời, quyền sở hữu, thông báo, khóa ngoại, migration giữ dữ liệu và điều hướng/khôi phục màn hình Student.

APK: `app/build/outputs/apk/debug/app-debug.apk`.
