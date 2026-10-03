# TN-Launcher

Launcher phong cách macOS Tahoe (Liquid Glass), giao diện Sáng/Tối, cho đầu Android 10 (1280×800) trên VinFast Limo Green.

- Kế hoạch: [docs/PLAN.md](docs/PLAN.md) · Design tokens: [docs/design-tokens.md](docs/design-tokens.md)
- Build: GitHub Actions (`.github/workflows/build.yml`) tạo APK debug trong tab Actions → Artifacts.
- Cài lên xe: `adb install -r app-debug.apk`, rồi Cài đặt → Đặt launcher mặc định. Giữ launcher gốc để quay lại.
- Biển số, tọa độ thời tiết, package app camera/trợ lái đặt trong `Prefs` (không đưa dữ liệu cá nhân vào mã nguồn).

Trạng thái: giai đoạn 1–2 (khung dự án, theme, sidebar, dock, lưới ứng dụng, trang chủ, thời tiết). Widget nhạc thật ở giai đoạn 3.
