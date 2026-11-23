# Expenser — Android (Kotlin + Jetpack Compose, Material 3)

Bản chuyển đổi native của web app React "Expenser" (quản lý chi tiêu cá nhân).

## Chạy thử
1. Mở thư mục này bằng **Android Studio (Ladybug 2024.2+)**, chờ Gradle sync (Studio sẽ tự tạo Gradle wrapper 8.9).
2. Chọn thiết bị/emulator (API 26+) → Run.

Stack: Kotlin 2.0, Compose BOM 2024.10, Material 3, Room, DataStore, WorkManager, Navigation Compose, Coil.

## Ánh xạ từ bản web
| Web (React) | Android |
|---|---|
| Tổng quan (Dashboard) | `ui/screens/Dashboard.kt` |
| Giao dịch (danh sách theo ngày, lịch, lọc, vuốt sửa/xóa) | `Transactions.kt`, `TxItems.kt` |
| Form giao dịch + gợi ý số tiền (gõ `2` → 2.000…2.000.000) | `TransactionForm.kt`, `Common.kt` (`MoneyField`) |
| Danh mục (emoji, màu, sắp xếp, chặn xóa khi đang dùng) | `Categories.kt` |
| Ví + chuyển tiền | `Wallets.kt` |
| Thống kê (Hôm nay/Tuần/Tháng/Năm/Tất cả, biểu đồ tròn, xu hướng) | `Statistics.kt` |
| Cafe: recap, thống kê, quán, đồ uống | `Cafe.kt` |
| Cài đặt: ngôn ngữ vi/en, sáng/tối, màu nhấn, Material You, cỡ chữ, nhắc nhở hàng ngày, nhập/xuất CSV | `Settings.kt`, `reminder/` |
| `DataContext.jsx` (Firestore + batch cập nhật số dư) | `data/Repository.kt` (Room, transaction nguyên tử) |
| `defaults.js`, `format.js`, `cafe.js` | `util/` |
| Palette M3 sinh từ seed #4CAF50 (`colors.css`) | `ui/theme/Theme.kt` (dùng đúng bảng màu gốc) |

## Khác biệt so với bản web
- **Dữ liệu lưu trên máy (Room)**, không còn Firebase Auth/Firestore → không có đăng nhập Google, không đồng bộ giữa thiết bị. `Repository` là điểm duy nhất truy cập dữ liệu nên có thể thay bằng Firestore sau này.
- Không có trang Landing, xuất PDF/Excel/PNG, upload ảnh imgbb (ảnh cafe nhập bằng link URL), gợi ý "nhập nhanh từ lần trước".
- Nhập/xuất **CSV** cùng định dạng cột với web (Ngày, Giờ, Loại, Danh mục, Ví, Số tiền, Ghi chú); Excel/JSON chưa hỗ trợ.
- Màu nhấn tuỳ chọn dùng thuật toán tông màu HSL xấp xỉ (không phải material-color-utilities); màu mặc định là bảng màu gốc chính xác.
- Nhắc nhở dùng WorkManager (không cần server push).
- Xóa giao dịch chuyển khoản hoàn lại số dư cả hai ví (bản web chưa xử lý).

## Lưu ý
Code được viết mà **chưa biên dịch** (môi trường tạo không có Android SDK). Nếu Gradle báo lỗi nhỏ về import/API, đó là chỗ cần chỉnh khi build lần đầu.
