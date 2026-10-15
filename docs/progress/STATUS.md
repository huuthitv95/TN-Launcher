# Trạng thái TN Launcher

15/10/2026 — Baseline Git trước commit: main / HEAD `4491b156a51deeabdb617be92ff33db418652130`; kiểm thử bên dưới chạy trên worktree triển khai tại baseline này. Người dùng đã yêu cầu “Commit và push lên nhánh main”, bao gồm source và ba font USER_CONFIRMED đã rà đúng file/byte/hash; archive vendor, dữ liệu riêng và APK/build output không nằm trong publication. SHA commit và kết quả push đối chiếu Git; đoạn này không xác nhận push thành công. AGENTS.md không đổi trong increment này. Kế hoạch sống: [EXECPLAN](EXECPLAN.md).

Phase hiện tại: **phục dựng chức năng Android đã triển khai và EMULATOR_VERIFIED; full original reconstruction INCOMPLETE; Figma NOT_READY**.

- Track A: metadata/Manifest22APK và trace hành vi gốc có bằng chứng. Adapter12 yêu cầu mở activity gốc, BTAV/navigation explicit-only; toàn bộ chức năng vendor thật vẫn STATIC_VERIFIED. Radio/media/DSP/CAN/camera lùi/overlay/ACC còn thiếu contract hoặc phụ thuộc firmware/phần cứng.
- Core mở rộng: app info, gỡ có xác nhận Android và guard system/self/vendor, thư mục Home/persistence, widget host thật ở màn riêng, Wi-Fi/ngày giờ/global search, wallpaper hệ thống. Giữ nguyên Home/Dock/Drawer/search/theme/SAF/recovery đã có.
- Track B: UI native sử dụng ba font USER_CONFIRMED, icon ứng dụng thật, TN brand và một mapping MacTahoe có nguồn. Chữ lớn1.5 và các trạng thái mới đã được kiểm tra; chưa làm lại bố cục Figma hoặc đóng gói Apple UI Kit.
- Kiểm chứng:25unitPASS; lint0errors/51warnings;33instrumentation và9E2E PASS mỗi profile1280×720/160dpi,1920×1080/320dpi, API29, animation scales1.0. Harness tìm app/thư mục qua các trang thật; folder cuối được xác nhận riêng lại ở1280 sau chỉnh assertion phân trang.
- Cổng increment: **EMULATOR_GATE_PASSED cho phần Android trên hai profile/animation1.0**. E2E1280 đã chạy lại9/9 sau xác nhận harness cuối. Cổng này không chứng minh full Wincavn parity hoặc ổn định image ởscales0.0.

Artifact: `app/build/outputs/apk/debug/app-debug.apk`, debug0.1.0(1), `vn.tnlauncher.app`. SHA-256 hiện tại `eba36322e7a0f0872c47019f38f09ab3cd3d909459480d8e925d5f24f590951c`. Số đo, lệnh và giới hạn nằm ở [compatibility report](../testing/compatibility-report.md); ma trận feature là nguồn per-feature, không sao chép vào đây.

Chuyển giao: `.ua/knowledge-graph.json` và [workflow agent](../architecture/architecture.md#chuyển-giao-ngữ-cảnh-cho-agent). Graph không thay source/chứng cứ; HEAD trùng không đủ vì worktree còn dirty. Kiểm tra contentHash từng file và inventory trước khi tin graph. Config tiếng Việt, autoUpdatefalse; không hook tự động, không chia sẻ token/intermediate/vendor dumps.

Giới hạn: folders chỉ trên Home, Dock vẫn component apps; widgets chưa drag/resize trong Home. Widget corruption fail-closed giữ raw/ID không rõ chủ; provider thiếu giữ bookkeeping. SwiftShader1920 còn jank cao; image từng abort SurfaceFlinger ởscales0.0, regression1.0đạt nhưng chưa xác định nguyên nhân. OS night mode bị khóa light, SYSTEM theo trạng thái thật. Không có DEVICE_VERIFIED/AUTOMOTIVE_VERIFIED hoặc rollback xe đã xác minh.

Tiếp theo: tiếp tục đọc contract vendor/firmware có bằng chứng và đo pilot render1920 nếu tối ưu. Figma vẫn hoãn; không đề nghị cài hoặc đổi Home trên xe.
