# TN-Launcher — Kế hoạch

Launcher phong cách **macOS Tahoe (Liquid Glass)**, có giao diện **Sáng** và **Tối**, cho đầu Android SID S170+ Pro trên VinFast Limo Green.

## 1. Thiết bị mục tiêu

| Hạng mục | Giá trị |
|---|---|
| Android | 10 (API 29), build QP1A.190711.020 |
| Màn hình | 1280×800, ngang cố định |
| CPU / RAM / ROM | UIS8581A 8 nhân 1.6 GHz / 4 GB / 64 GB |
| Hệ quả | Không có `RenderEffect` (cần API 31) → làm kính bằng ảnh nền làm mờ sẵn; hạn chế animation nặng |

## 2. Tham chiếu thiết kế Apple

- Newsroom WWDC25: Liquid Glass là lớp điều khiển nằm trên nội dung; macOS Tahoe có Dock/widget với giao diện sáng, tối, tint và clear.
- HIG – Materials: Liquid Glass chỉ cho điều hướng/điều khiển (sidebar, dock), dùng tiết kiệm; vật liệu chuẩn (blur, vibrancy) cho lớp nội dung.
- WWDC25 session 219 "Meet Liquid Glass": hai biến thể Regular (dễ đọc, mặc định) và Clear (cần lớp phủ tối). Không trộn hai biến thể trong cùng một giao diện.
- Áp dụng: dùng **Regular** cho toàn bộ; sidebar + dock là lớp kính; thẻ nội dung (xe, thời tiết, nhạc) là vật liệu chuẩn, không chồng kính lên kính.

Link: https://www.apple.com/newsroom/2025/06/apple-introduces-a-delightful-and-elegant-new-software-design/ · https://developer.apple.com/design/human-interface-guidelines/materials · https://developer.apple.com/videos/play/wwdc2025/219/

## 3. Màn hình (theo mockup)

1. **Trang chủ**: sidebar trái (Trang chủ, Bản đồ, Bluetooth, Âm nhạc, Ứng dụng, Cài đặt) · thẻ xe + 3 nút (Camera 360°, Trợ lái, Thông tin xe) · thẻ thông tin xe · đồng hồ + thời tiết · trình phát nhạc · Dock 5 ứng dụng.
2. **Ứng dụng**: lưới biểu tượng + ô tìm kiếm.
3. **Camera 360°**: mở ứng dụng camera của đầu máy (cần package name, lấy bằng ADB).
4. **Thông tin xe**.
5. **Cài đặt**: Kết nối, Hiển thị, Âm thanh, Ứng dụng, Hệ thống, Tùy chỉnh, Thông tin.
6. **Hành trình xanh**: màn hình chờ/ảnh xe toàn màn hình.

## 4. Kiến trúc kỹ thuật

- Kotlin + Jetpack Compose, `minSdk 29`, `targetSdk 29`, chỉ landscape.
- Manifest: `MAIN` + `HOME` + `DEFAULT`, `launchMode=singleTask`.
- Danh sách ứng dụng: `PackageManager.queryIntentActivities`; ẩn ứng dụng do người dùng chọn; thứ tự dock lưu bằng DataStore.
- Nhạc: `NotificationListenerService` + `MediaSessionManager` (cần cấp quyền thủ công 1 lần).
- Thời tiết: Open-Meteo (không cần khóa API), cache 30 phút.
- Bản đồ: hiển thị tóm tắt hướng dẫn nếu ứng dụng bản đồ phát thông báo; không tự làm điều hướng.
- Camera 360° / Trợ lái: gọi intent tới ứng dụng sẵn có của đầu máy.
- Hiệu năng: không `blur` thời gian thực; kính = ảnh nền đã làm mờ sẵn (cắt theo vị trí thẻ) + lớp phủ màu + viền sáng 1dp. Mục tiêu ≥ 30 fps, khởi động < 2 s.

## 5. Hệ thống thiết kế (token)

Xem `docs/design-tokens.md`. Hai bộ giá trị Sáng/Tối chung một cấu trúc token (Global → Semantic → Component).

Giao diện Sáng/Tối: theo hệ thống, theo giờ (mặt trời mọc/lặn), hoặc chọn tay.

## 6. Lộ trình

| Giai đoạn | Nội dung | Kết quả |
|---|---|---|
| 0 | Chốt thiết kế, lấy package name camera/trợ lái qua ADB | Danh sách intent |
| 1 | Khung dự án, theme Sáng/Tối, sidebar + dock + lưới ứng dụng | Đặt được làm launcher mặc định |
| 2 | Thẻ xe, thông tin xe, đồng hồ, thời tiết | Trang chủ hoàn chỉnh |
| 3 | Widget nhạc, Camera 360°, Cài đặt | Dùng hằng ngày |
| 4 | Tối ưu, kiểm thử trên xe, CI build APK | Bản phát hành 1.0 |

## 7. Rủi ro

- Nhiều đầu SID khóa launcher hoặc tự quay về launcher gốc khi khởi động → cần thử "Home" mặc định; có thể phải dùng ADB `cmd package set-home-activity`.
- Nút vật lý/vô lăng và MCU không đi qua launcher; chỉ ảnh hưởng nếu launcher gốc nhận các phím đó.
- Nâng Android 10 → 11 có thể xóa launcher; luôn giữ APK để cài lại.
- Giữ nguyên launcher gốc, không gỡ, để quay lại nếu lỗi.
