# Expenser — Android (Kotlin + Jetpack Compose, Material 3)

Bản native của web app React "Expenser". Dùng **cùng Firebase (Auth + Firestore)** với bản web nên dữ liệu đồng bộ giữa web và app.

## 1.9.0
- Form giao dịch điền theo thứ tự tuỳ ý (kiểm tra đầy đủ khi lưu); "Nhập nhanh" nằm trên ô số tiền, gọn hơn, tiền dạng 20K.
- Toast mới ở phía trên, có 4 loại (thành công / lỗi / cảnh báo / thông tin), tự tắt nhanh; form ví & danh mục dạng lưới cân đối; tab Cafe recap gọn đẹp hơn.
- Lightbox vuốt ngang xem ảnh được; hỗ trợ màn hình ngang (thanh điều hướng dạng rail, sidebar cuộn được, đăng nhập 2 cột); thông báo nhắc nhở ngắn gọn.

## 1.8.3
- Vuốt trái → phải ở tab Tổng quan mở sidebar; Cài đặt gộp một mục Xuất / nhập toàn bộ dữ liệu (JSON, CSV zip, CSV lẻ); lưới màu nhấn 6x3.
- "Nhập nhanh" 2 hàng cuộn ngang cùng lúc; skeleton cho Thống kê, Ví, Danh mục, Cafe và các sheet; animation form/điều hướng/sheet chậm và mượt hơn; hộp xác nhận lớn hơn, 2 nút bằng nhau.

## 1.8.2
- Sửa lỗi form giao dịch báo thiếu quán/đồ uống dù đã chọn (nút Lưu giờ luôn kiểm tra theo dữ liệu mới nhất); hai nút Hủy/Lưu bằng nhau.
- Ghi nhớ kiểu xem (card/list, lưới, danh sách/lịch) cho từng màn hình; "Nhập nhanh" nhỏ gọn hơn; đổi màu 2 thẻ top danh mục.

## 1.8.1
- Form giao dịch: báo lỗi rõ khi không lưu được; "Nhập nhanh" lọc theo số tiền đang gõ; chọn quán / đồ uống bằng sheet có ảnh, tìm kiếm và đổi list/lưới.
- Lightbox vuốt ngang thay cho nút trước/sau; nút lên đầu trang nằm bên phải (trên nút thêm), to hơn.
- Ảnh báo cáo tỉ lệ dọc 9:16; thẻ thống kê hiển thị danh mục chi / thu nhiều nhất; lưới đồ uống ảnh vuông 1:1; thêm thông tin tác giả.

## 1.8.0
- Ảnh trong form quán / đồ uống / giao dịch cafe: chọn ảnh từ máy, tự nén và tải lên imgbb (cần `IMGBB_API_KEY` trong `.env`).
- Form giao dịch: hàng "Nhập nhanh" gợi ý danh mục + số tiền đã dùng nhiều; chọn xong ngày giờ tự lấy giờ hiện tại.
- Lịch giao dịch: sheet theo ngày chỉ để xem; ô ngày xanh khi tổng ngày dương, đỏ khi âm.
- Quán cafe: placeholder ngắn hơn, đổi card/list và lưới/list mượt hơn (Crossfade).
- Nút lên đầu trang cuộn mượt.
- Ảnh báo cáo thống kê thiết kế lại (tổng thu/chi, top danh mục, biểu đồ tròn + thẻ số liệu).
- Màn đăng nhập thiết kế lại.

## Thiết lập Firebase (bắt buộc, làm 1 lần)
1. Firebase Console → dự án **của bản web** → *Project settings* → *Add app* → Android, package name: `com.expenser.app`.
2. Lấy SHA-1 của khóa debug: `./gradlew signingReport` (hoặc Android Studio → Gradle → app → Tasks → android → signingReport). Dán SHA-1 vào mục *SHA certificate fingerprints* của app vừa thêm. (Khi phát hành thêm SHA-1 của khóa release / Play App Signing.)
3. Authentication → Sign-in method → bật **Google**.
4. Tải **google-services.json** (sau khi đã thêm SHA-1) và đặt vào `app/google-services.json`, rồi chạy `powershell -File scripts\make-env.ps1` để chuyển nội dung vào `.env` (xem phần *Bảo mật bằng .env*). Sau đó có thể xóa file JSON.
5. Sync Gradle → Run. Không có cấu hình này app sẽ hiện màn "Chưa cấu hình Firebase".

## Bảo mật bằng .env
- `.env` (đã có trong `.gitignore`) chứa `GOOGLE_SERVICES_JSON_BASE64` — Gradle tự giải mã thành `app/google-services.json` (cũng bị git bỏ qua) mỗi lần build. Mẫu: `.env.example`.
- Ký bản release: chạy `powershell -File scripts\make-keystore.ps1` (tạo `keystore/expenser-release.jks` và ghi `KEYSTORE_*` vào `.env`, in ra SHA-1 để thêm vào Firebase). **Sao lưu keystore + .env**, mất là không thể cập nhật app.
- Chưa có keystore thì bản release được ký bằng khóa debug (vẫn cài được, Google sign-in dùng SHA-1 debug đã đăng ký).
- File APK: `app/build/outputs/apk/<debug|release>/Expenser-<phiên bản>-<debug|release>.apk`.
- Lưu ý: giá trị trong google-services.json (API key…) vẫn nằm trong APK; hãy giới hạn API key theo package + SHA-1 trên Google Cloud.

## Chạy thử
Android Studio (Ladybug 2024.2+) → mở thư mục này → Gradle sync → Run (API 26+).

## Giao diện
- Giống bản web mobile: thanh trên (menu / tìm trang / đổi chế độ sáng-tối), drawer đủ mục, thanh dưới 4 mục (Tổng quan, Giao dịch, Thống kê, Cài đặt), nút FAB theo từng màn, form toàn màn hình, vuốt sửa/xóa, bộ lọc thu gọn, cài đặt dạng accordion.
- Màu chủ đạo: **Material 3 baseline blue** (#0061A4); có thể đổi màu nhấn hoặc dùng Material You (Android 12+).
- Icon adaptive Material 3 (108dp, vùng an toàn 66dp, có lớp monochrome cho themed icon) + splash screen.

## Tính năng mới (1.7)
- Vuốt chuyển tab dạng pager: kéo từ từ thấy màn kế bên, thả quá nửa thì chuyển; vuốt phải ở Tổng quan mở sidebar.
- Giao dịch chỉ vuốt để sửa / xóa (không tap); lịch tô màu theo mức chi, chạm ngày mở sheet danh sách; chú thích "vuốt để sửa / xóa".
- Nút "lên đầu trang" ở các danh sách dài; Đồ uống có dạng lưới / danh sách; Quán cafe có dạng thẻ / danh sách gọn (kéo thả).
- Biểu đồ: bảng màu 16 màu khác nhau, xanh / đỏ cố định đậm (không phụ thuộc Material You); thẻ thống kê bằng nhau.
- Thông báo nhắc nhở dùng AlarmManager (tự đặt lại sau khi khởi động lại máy / cập nhật app), icon thông báo riêng.

## Tính năng (1.6)
- Kéo thả (nắm biểu tượng ⋮⋮) để sắp xếp Danh mục, Quán cafe, Đồ uống; ảnh quán cafe 16:9.
- Nhãn % ngay trong biểu đồ tròn; màu thu nhập / chi tiêu đậm hơn.
- Animation: chuyển tab trượt theo hướng vuốt, form thêm/sửa trượt lên, FAB, danh sách, ảnh hiện dần; skeleton khi tải; cache ảnh 150 MB + tải trước ảnh quán/đồ uống.
- Bản release ký sẵn (cài được), R8 + shrink, tên APK `Expenser-<ver>-<type>.apk`.

## Tính năng (1.5)
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
