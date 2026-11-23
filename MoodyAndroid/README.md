# Moody for Android

Bản **Android native** (Kotlin + Jetpack Compose + Material Design 3) của web app Moody — nhật ký tâm trạng hằng ngày.

- Kotlin 2.0, Jetpack Compose (BOM 2024.12), Material 3
- Room + DataStore (chế độ khách, lưu trên máy)
- Firebase Auth (Google, qua Credential Manager) + Firestore — **cùng cấu trúc dữ liệu với web**, nên cùng một tài khoản Google sẽ thấy cùng dữ liệu ở web và Android
- WorkManager cho nhắc nhở nền (chạy cả khi app đã đóng — web không làm được)
- Song ngữ Tiếng Việt / English (đổi ngôn ngữ từng app, theo thiết bị hoặc chọn thủ công)

## Chạy thử

1. Mở thư mục này bằng **Android Studio** (Ladybug trở lên), JDK 17.
2. Đợi Gradle Sync, chọn thiết bị/emulator (API 26+), bấm Run.

App chạy được ngay ở **chế độ khách** (dữ liệu lưu trên máy) mà không cần Firebase.

## Bật đăng nhập Google + đồng bộ (dùng chung project Firebase với web)

1. Firebase Console → project Moody hiện tại → **Add app → Android**, package name `com.minhtrong67.moody`.
2. Thêm **SHA-1** (debug): chạy `./gradlew signingReport` và copy SHA-1 của biến thể `debug`. Khi phát hành thì thêm SHA-1 của keystore release.
3. Authentication → Sign-in method → bật **Google** (đã bật nếu web đang dùng).
4. Tải `google-services.json`, đặt vào `app/google-services.json`, rồi Sync lại.
   (File này đã nằm trong `.gitignore`; Gradle chỉ áp dụng plugin Firebase khi file tồn tại.)
5. Firestore rules của web (`users/{uid}/...` chỉ chủ tài khoản truy cập) dùng được nguyên cho Android.

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
| Nhắc nhở (chỉ khi mở app) | Nhắc hằng ngày / báo cáo tuần / tháng bằng WorkManager |

## Chưa chuyển (có chủ đích)

- Landing page marketing và nút "Cài đặt ứng dụng" (PWA) — không còn ý nghĩa trong app native.
- Xuất/nhập **.xlsx** (CSV/JSON/ZIP đã đủ để trao đổi dữ liệu với web). Có thể thêm bằng thư viện `fastexcel`.
- Chọn font chữ (Google Sans Flex, Inter…): app dùng font hệ thống; cỡ chữ vẫn chỉnh được.

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
