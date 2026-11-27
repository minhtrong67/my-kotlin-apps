# Calendar Pro — Android (Kotlin + Material Design 3)

Bản chuyển từ web `calendar-pro` (React) sang ứng dụng Android native dùng **Kotlin + Jetpack Compose + Material 3**.

## Yêu cầu
- Android Studio mới (Ladybug trở lên), **JDK 21** (OpenJDK 21.0.2 chạy tốt: *Settings → Build Tools → Gradle → Gradle JDK = 21*)
- Android SDK Platform 35 (Studio sẽ tự đề nghị tải khi Sync)
- Thiết bị / máy ảo Android 8.0+ (minSdk 26)

## Build
1. Mở thư mục này bằng Android Studio → chờ *Gradle Sync*.
2. Chạy trực tiếp (▶) hoặc dùng dòng lệnh:
   ```
   ./gradlew assembleDebug      # APK: app/build/outputs/apk/debug/calendar-pro-1.1.0-debug.apk
   ./gradlew assembleRelease    # APK: app/build/outputs/apk/release/calendar-pro-1.1.0-release.apk (ký bằng khoá debug để cài ngay)
   ./gradlew testDebugUnitTest  # test lịch âm / ngày lễ
   ```

## Cấu trúc
```
app/src/main/java/com/gnort67/calendarpro/
├── core/      Lịch âm (Hồ Ngọc Đức), ngày lễ, tiến độ năm — Kotlin thuần, đã đối chiếu 2169 kết quả với bản JS
├── data/      Đọc assets JSON, DataStore (cài đặt + sự kiện cá nhân)
├── ui/        Theme M3 (19 màu), thành phần dùng chung, 4 màn hình chính, Cài đặt, bottom sheet
└── util/      Thêm vào Lịch hệ thống, chia sẻ ảnh
```
- `assets/data/*.json`, `res/values*/strings.xml`, `ui/theme/Palettes.kt`, `ui/shapes/PolygonData.kt` được sinh từ dữ liệu của bản web.

## Cử chỉ & hiệu ứng
Vuốt ngang chuyển tab · vuốt ngang đổi tháng trong lưới lịch · vuốt phải ở tab đầu mở menu · kéo xuống để làm mới (Trang chủ) · vuốt thẻ sự kiện cá nhân (trái = xoá, phải = sửa) · vuốt xuống đóng bottom sheet · back dự đoán (Cài đặt) · nhấn có hiệu ứng lò xo + rung nhẹ · nút lên đầu trang.

## Tính năng mới (1.1.0)
- **Material You**: lấy màu theo hình nền (Android 12+), bật/tắt trong *Cài đặt → Màu chủ đề*. Có 19 màu thủ công, hỗ trợ cả giao diện tối.
- **Thông báo hằng ngày**: *Cài đặt → Thông báo* — chọn giờ nhắc, có nút *Gửi thử*. Nội dung: âm lịch hôm nay, ngày lễ/sự kiện, sao ngày, ngày chay, sự kiện ngày mai.
- **Xoay ngang**: thanh điều hướng chuyển sang bên trái, các màn hình chia hai cột.
- Dùng font mặc định của máy; tên APK theo dạng `calendar-pro-<phiên bản>-<loại build>.apk`.
