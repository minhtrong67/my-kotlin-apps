# Moody for Android

Bản **Android native** (Kotlin + Jetpack Compose + Material Design 3) của web app Moody — nhật ký tâm trạng hằng ngày.

- Kotlin 2.0, Jetpack Compose (BOM 2024.12), Material 3
- Room + DataStore (chế độ khách, lưu trên máy)
- Firebase Auth (Google, qua Credential Manager) + Firestore — **cùng cấu trúc dữ liệu với web**, nên cùng một tài khoản Google sẽ thấy cùng dữ liệu ở web và Android
- Nhắc nhở bằng AlarmManager (chạy cả khi app đã đóng, đặt lại sau khi khởi động máy) — web không làm được
- Song ngữ Tiếng Việt / English (đổi ngôn ngữ từng app, theo thiết bị hoặc chọn thủ công)

## Chạy thử

1. Mở thư mục này bằng **Android Studio** (Ladybug trở lên), JDK 17.
2. Đợi Gradle Sync, chọn thiết bị/emulator (API 26+), bấm Run.

App chạy được ngay ở **chế độ khách** (dữ liệu lưu trên máy) mà không cần Firebase.

## Bật đăng nhập Google + đồng bộ (dùng chung project Firebase với web)

Cấu hình Firebase nằm trong file **`.env`** ở thư mục gốc (đã được `.gitignore`, không commit, không cần `google-services.json`):

1. Sao chép `.env.example` thành `.env` và điền 6 giá trị (lấy trong Firebase Console → Project settings → Your apps → Android, hoặc từ file `google-services.json` cũ):
   `FIREBASE_PROJECT_ID`, `FIREBASE_PROJECT_NUMBER`, `FIREBASE_STORAGE_BUCKET`, `FIREBASE_APP_ID`, `FIREBASE_API_KEY`, `GOOGLE_WEB_CLIENT_ID` (OAuth client loại *Web*).
2. Package name đăng ký trên Firebase phải là `com.minhtrong67.moody`, và đã thêm **SHA-1** (`./gradlew signingReport`). Bật Google ở Authentication.
3. Build lại. Không có `.env` thì app vẫn chạy ở chế độ khách.
4. Trên CI có thể đặt cùng tên đó làm biến môi trường thay cho file `.env`.

Lưu ý bảo mật: API key của Firebase không phải bí mật tuyệt đối (nó nằm trong APK), hãy **giới hạn key theo package + SHA-1** trong Google Cloud Console → APIs & Services → Credentials, và dùng Firestore Rules (chỉ chủ tài khoản đọc/ghi `users/{uid}/...`).

## Đối chiếu với web

| Web | Android |
|---|---|
| Hôm nay (lời chào, thẻ check-in, chuỗi ngày, TB tuần, 7 ngày, gần đây) | `TodayScreen` |
| Check-in full-screen (ngày giờ, 5 mood, hoạt động, ghi chú, xác nhận khi sửa) | `CheckInScreen` |
| Lịch sử: tìm kiếm, lọc, sắp xếp, vuốt để sửa/xóa, lịch tháng + tóm tắt | `HistoryScreen` |
| Thống kê ngày/tuần/tháng/năm, 2 biểu đồ tròn, chú thích %, xuất PDF/PNG | `StatsScreen` (chụp đúng vùng đang hiển thị, chia sẻ qua share sheet) |
| Quản lý hoạt động / tâm trạng | `ManageScreens` |
| 19 màu chủ đề Material 3, sáng/tối/hệ thống, cỡ chữ, ngôn ngữ, trang mặc định | `SettingsScreen`, bảng màu sinh từ `palettes.js` |
| Xuất/nhập CSV, JSON, "toàn bộ" (.zip 3 CSV) | `transfer/DataTransfer.kt` (file tương thích hai chiều với web) |
| Nhắc nhở (chỉ khi mở app) | Nhắc hằng ngày / báo cáo tuần / tháng bằng AlarmManager (xem mục Nhắc nhở) |

## Chưa chuyển (có chủ đích)

- Landing page marketing và nút "Cài đặt ứng dụng" (PWA) — không còn ý nghĩa trong app native.
- Xuất/nhập **.xlsx** (CSV/JSON/ZIP đã đủ để trao đổi dữ liệu với web). Có thể thêm bằng thư viện `fastexcel`.

## Ghi chú bản mới

- Màu chủ đạo mặc định **Teal**; thêm tuỳ chọn **Tự động (Material You)** (Android 12+) trong Cài đặt → Màu chủ đạo.
- Vuốt ngang để chuyển giữa 4 tab (Hôm nay / Lịch sử / Thống kê / Cài đặt). Trong danh sách Lịch sử, vuốt trên một thẻ là sửa/xoá (cần vuốt xa), vuốt ở vùng trống để đổi tab.
- File APK được đặt tên `Moody-v1.0.0-release.apk` / `Moody-v1.0.0-debug.apk` (đổi `appVersionName` trong `app/build.gradle.kts`).
- Icon: adaptive icon (nền teal + mặt cười), có cả icon đơn sắc cho "Themed icons" của Android 13+.

## Nhắc nhở hoạt động thế nào

- Dùng `AlarmManager` (không dùng WorkManager vì WorkManager có thể bị trễ hàng giờ khi máy ở chế độ Doze).
- Android 13+: app xin quyền thông báo khi bạn bật một nhắc nhở.
- Android 12+: để nhắc **đúng giờ**, vào Cài đặt → Nhắc nhở → "Cho phép báo thức chính xác". Nếu không cấp, nhắc nhở vẫn chạy nhưng có thể trễ vài phút.
- Samsung/Xiaomi/Oppo… hay chặn app nền: đặt Moody ở "Không hạn chế/Không tối ưu pin" (có nút mở thẳng trong Cài đặt).
- Nhắc hằng ngày tự bỏ qua nếu bạn đã check-in hôm nay. Nút "Thử sau 10 giây" kiểm tra cả đường báo thức → thông báo (khóa màn hình rồi chờ).
- Báo thức được đặt lại tự động sau khi khởi động máy, cập nhật app, đổi giờ/múi giờ.

## Muốn thấy độ mượt thật sự

Bản **debug** của Jetpack Compose chậm hơn rất nhiều. Hãy thử bản release (đã ký tạm bằng khóa debug):
`gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.

## Cấu trúc

```
app/src/main/java/com/minhtrong67/moody/
  data/        models, Repository (Room ↔ Firestore), local/ remote/ auth/
  transfer/    xuất/nhập dữ liệu
  notify/      nhắc nhở (WorkManager)
  ui/          theme, components, screens, điều hướng (MoodyRoot)
  util/        ngày tháng, thống kê
```

> Lưu ý: project được viết và kiểm tra cú pháp bằng trình biên dịch Kotlin, nhưng **chưa build đầy đủ** với Android SDK trong môi trường tạo ra nó. Nếu Android Studio báo lỗi nhỏ ở lần Sync/Build đầu (phiên bản thư viện, API đã đổi tên…), gửi nguyên văn lỗi cho mình để sửa.
