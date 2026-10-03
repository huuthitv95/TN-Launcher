# Bước tiếp theo (giai đoạn 4) — kết quả tham khảo và kế hoạch

## 1. Đã đọc

**Các repo tích hợp VinFast** (`thangnd85/vinfast-connected-car`, `vfdashboard`, `leolionart/vinfast`, ...)
- Là phần mềm cộng đồng, **không chính thức**. Chúng đăng nhập bằng tài khoản VinFast qua Auth0, lấy dữ liệu qua MQTT/AWS IoT và API nội bộ của app VinFast, có cả lệnh điều khiển từ xa (khóa/mở cửa, mở cốp, bật điều hòa).
- VFDashboard ghi rõ API cần ký request bằng X-HASH và X-HASH-2 (cơ chế ký nội bộ của app), và README nói VinFast đã báo cáo website đó là phishing. Vì vậy cách này có thể bị chặn bất kỳ lúc nào, và có rủi ro với tài khoản.
- Các bộ cảm biến chỉ có profile VF3/VF5/VF6/VF7/VF8/VF9/e34. **Không có Limo Green**, mã dự phòng là của VF5 nên chưa chắc đúng.

**wincavn.com/file-download** — phần mềm cho đầu Winca: Winca Control, Winca Widget, Winca Tracking, Áp Suất Lốp Winca (TPMS), VietMap Live, Winca Tube, KiKi/Giọng nói, Zing MP3, Dashcam, WincaStore.

## 2. Quyết định

- **Chưa** làm đăng nhập VinFast cloud trong launcher: dùng API không chính thức, phải bỏ qua cơ chế ký, và phải lưu mật khẩu tài khoản trên đầu xe ai cũng thấy. Nếu sau này vẫn muốn, đề xuất chỉ đọc (không có lệnh điều khiển từ xa), lưu token bằng Android Keystore, và bạn tự quyết định sau khi cân nhắc rủi ro tài khoản.
- Ưu tiên nguồn dữ liệu **ngay trên đầu máy** (MCU/CAN, app Winca) vì không cần tài khoản.

## 3. Đã làm trong bản 0.2.0

- Gán ứng dụng cho các nút Camera 360°, Trợ lái, Áp suất lốp, Bản đồ, Âm nhạc: tự nhận diện theo tên (ví dụ "TPMS", "Áp suất lốp", "VietMap"), có thể chọn lại trong Cài đặt.
- Tùy chỉnh Dock (tối đa 5 ứng dụng).
- Sửa tên xe, biển số và chọn thành phố cho thời tiết ngay trong Cài đặt.
- Nút Trợ lái chưa gán sẽ mở màn chọn ứng dụng thay vì báo lỗi.

## 4. Giai đoạn 5 (đề xuất)

1. Gửi APK hoặc ảnh danh sách ứng dụng cài sẵn (Winca Control, TPMS, trợ lái) để mình đọc package/activity và gắn chính xác.
2. Đọc dữ liệu xe từ đầu máy: tốc độ, cửa, số lùi. Cần phân tích dịch vụ của nền tảng SYU (`com.syu.*`) trong APK Winca Control.
3. Thẻ áp suất lốp trên trang chủ nếu app TPMS có thể đọc được.
4. Màn "Hành trình xanh" và chỉnh giao diện sát mockup.
