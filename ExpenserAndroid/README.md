# Expenser — Android (Kotlin + Jetpack Compose, Material 3)

Bản native của web app React "Expenser". Dùng **cùng Firebase (Auth + Firestore)** với bản web nên dữ liệu đồng bộ giữa web và app.

## Thiết lập Firebase (bắt buộc, làm 1 lần)
1. Firebase Console → dự án **của bản web** → *Project settings* → *Add app* → Android, package name: `com.expenser.app`.
2. Lấy SHA-1 của khóa debug: `./gradlew signingReport` (hoặc Android Studio → Gradle → app → Tasks → android → signingReport). Dán SHA-1 vào mục *SHA certificate fingerprints* của app vừa thêm. (Khi phát hành thêm SHA-1 của khóa release / Play App Signing.)
3. Authentication → Sign-in method → bật **Google**.
4. Tải **google-services.json** (sau khi đã thêm SHA-1) và đặt vào `app/google-services.json`.
5. Sync Gradle → Run. Không có file này app sẽ hiện màn "Chưa cấu hình Firebase".

## Chạy thử
Android Studio (Ladybug 2024.2+) → mở thư mục này → Gradle sync → Run (API 26+).

## Giao diện
- Giống bản web mobile: thanh trên (menu / tìm trang / đổi chế độ sáng-tối), drawer đủ mục, thanh dưới 4 mục (Tổng quan, Giao dịch, Thống kê, Cài đặt), nút FAB theo từng màn, form toàn màn hình, vuốt sửa/xóa, bộ lọc thu gọn, cài đặt dạng accordion.
- Màu chủ đạo: **Material 3 baseline blue** (#0061A4); có thể đổi màu nhấn hoặc dùng Material You (Android 12+).
- Icon adaptive Material 3 (108dp, vùng an toàn 66dp, có lớp monochrome cho themed icon) + splash screen.

## Tính năng mới (1.5)
- Biểu đồ chỉ dùng biểu đồ tròn, có chú thích đầy đủ (màu, tên, số tiền, %); xuất báo cáo thống kê thành ảnh PNG.
- Cài đặt gọn: Tài khoản, Ngôn ngữ, Giao diện, Nhắc nhở, Dữ liệu (accordion), Giới thiệu.
- Dữ liệu: xuất / nhập CSV hoặc JSON cho từng mục (giao dịch, danh mục, ví, quán cafe, đồ uống) và xuất tất cả (JSON, hoặc CSV nén .zip), nhập tất cả từ JSON.
- Cafe recap giống bản web (bộ lọc có dropdown, thẻ ảnh 16:9, buổi trong ngày), thẻ Quán cafe có mở bản đồ / sao chép liên kết / ảnh lỗi có placeholder.
- Image lightbox: chạm ảnh để xem phóng to, vuốt hoặc dùng mũi tên để chuyển ảnh trước / sau, nhấn đúp để zoom.
- Vuốt ngang để chuyển tab (thanh dưới, Cafe recap / Thống kê).

## Dữ liệu (tương thích web)
`users/{uid}/transactions | categories | wallets | cafeShops | cafeDrinks | cafeLogs`. Ghi dữ liệu theo kiểu offline-first (không chờ server), số dư ví cập nhật bằng `FieldValue.increment`.

## Khác biệt so với bản web
- Không có Landing, xuất PDF/Excel/PNG, upload ảnh (ảnh cafe nhập bằng link URL).
- Nhập/xuất CSV cùng cột với web; báo cáo thống kê xuất CSV theo kỳ.
- Nhắc nhở hàng ngày dùng WorkManager.

## Lưu ý
Code chưa được biên dịch trong môi trường tạo (không có Android SDK). Nếu Gradle báo lỗi nhỏ, gửi log để chỉnh tiếp.
