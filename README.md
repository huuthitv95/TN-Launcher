# TN Launcher

Launcher Android native Kotlin/Views cho Android 10 (API 29), application ID `vn.tnlauncher.app`, phiên bản MVP `0.1.0` (1). Source và APK dùng thử cá nhân; không tự cài lên xe. Commit/push chỉ thực hiện theo yêu cầu rõ của người dùng.

Home hiển thị ứng dụng thực; Dock có Apps/Cài đặt cố định và tối đa 6 favorite. Có tìm kiếm tiếng Việt không dấu, chỉnh thứ tự bằng kéo/menu, theme sáng/tối/theo hệ thống, giảm chuyển động, hình nền qua trình chọn Android và lưu cấu hình bằng Preferences DataStore.

Phase hiện tại ưu tiên phục dựng chức năng Launcher gốc trước thiết kế Figma. Source đã bổ sung app-info/yêu cầu gỡ ứng dụng qua xác nhận Android, thư mục Home có tên/thành viên/thứ tự/lưu trạng thái, màn hình host Android Widgets thật qua consent/configuration, cài đặt Wi-Fi/ngày giờ, yêu cầu tìm kiếm hệ thống và chế độ hiển thị hình nền hệ thống. Widget nằm ở màn hình Tiện ích riêng, chưa có kéo/resize widget vào lưới Home. Kết quả kiểm thử bản mở rộng phải xem báo cáo hiện tại; cổng MVP trước đó không chứng minh full Wincavn parity.

Các chức năng MVP đã có trong source. Giao diện hiện ở mức native cơ bản: Home dạng lưới, Dock gồm icon favorite và nút chữ Ứng dụng/Cài đặt, Settings là danh sách cuộn. Lượt tinh chỉnh độ trung thực với tham chiếu iPadOS chưa hoàn thiện; chưa phải giao diện pixel-perfect. Quyết định thiết kế hiện tại được ghi trong [design system](docs/design/design-system.md); tình trạng kiểm chứng được ghi riêng trong [compatibility report](docs/testing/compatibility-report.md).

Source đã tích hợp SF Pro Text Regular 400 cho nội dung, SF Pro Display Semibold 600 cho tiêu đề/đồng hồ và MRCH NewYork Regular 400 chỉ cho nhãn cố định “TN Launcher”. Có fallback Android sans khi tải font lỗi hoặc thiếu tài nguyên. Giấy phép ba nguồn font là `USER_CONFIRMED` theo AGENTS.md §22; font gốc được giữ nguyên. Typography đã kiểm tra trên API 29 ở cả hai profile và theme sáng/tối; nguồn, hash, thang chữ và giới hạn tiếng Việt nằm trong [design system](docs/design/design-system.md).

Bản phục dựng Android đã PASS25unit, lint0errors/51warnings,33instrumentation và9E2E ở mỗi profile API29. Cổng cuối và checksum APK được chốt tại [compatibility report](docs/testing/compatibility-report.md); full Wincavn parity vẫn chưa hoàn tất. Frame timing1920 trên SwiftShader còn chậm; không cam kết FPS trên xe. Regression dùng animation hệ thống1.0, theme riêng LIGHT/DARK và kiểm tra chữ lớn1.5; giới hạn OS night mode và crash image0.0 được ghi trong báo cáo.

## Build trên Windows

Java 21, Android SDK platform 35 và build-tools tương thích. Không thay đổi môi trường hệ thống:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
# Tạo local.properties với sdk.dir trỏ đến SDK của máy (file được ignore).
./gradlew.bat tasks --all
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Artifact duy nhất: `app/build/outputs/apk/debug/app-debug.apk`. Wrapper Gradle 8.11.1, AGP 8.10.1, Kotlin 2.1.21; minSdk 29, compile/targetSdk 35. Debug signing key do Android tooling quản lý ngoài repo; không dùng như khóa phát hành.

## Kiểm thử emulator

AVD `TN_Launcher_API29`, image `system-images;android-29;google_apis;x86_64`, WHPX. Chỉ dùng serial emulator đã xác minh. Không cài APK vendor lên emulator.

```powershell
./tools/verify-emulator.ps1 -Serial emulator-5580 -Profile 1280
./tools/verify-emulator.ps1 -Serial emulator-5580 -Profile 1920
```

Script chọn TN Launcher làm Home **trên emulator** rồi chạy unit/lint/build/instrumentation. APK kiểm thử có ứng dụng mẫu và component kiểm tra quyền; chúng không nằm trong APK bàn giao. Kết quả, lifecycle/E2E và giới hạn: [compatibility report](docs/testing/compatibility-report.md).

## Sử dụng

- Nhấn Ứng dụng để mở danh sách và tìm ứng dụng. Nhấn giữ icon rồi thả tại chỗ để mở menu ghim/bỏ ghim hoặc di chuyển; giữ rồi kéo để đổi thứ tự Home/Dock.
- Apps và Cài đặt luôn có sẵn. Không ghi đè Launcher gốc. Ứng dụng tạm thiếu vẫn giữ vị trí/favorite đã lưu và có thể bỏ ghim.
- Cài đặt cho phép đổi theme, giảm chuyển động, chọn/reset hình nền, sắp xếp Home theo tên và xem khả năng tích hợp.
- Menu ứng dụng có thông tin và yêu cầu gỡ với hộp thoại Android; TN chặn yêu cầu gỡ chính nó, app hệ thống và các gói vendor được bảo toàn. Không gọi API xóa package trực tiếp.
- Tạo/chỉnh thư mục từ Settings hoặc menu ứng dụng. Drawer vẫn liệt kê toàn bộ app; xóa thư mục đưa các app về Home. Ứng dụng tạm thiếu vẫn giữ thành viên đã lưu.
- Tiện ích Android ở màn riêng: chọn provider đã cài, cho phép trong hộp thoại Android và hoàn tất cấu hình của provider. Có cập nhật/click RemoteViews thật, lưu ID, hủy/xóa riêng bản widget; chưa kéo/resize widget trong lưới Home. Dữ liệu hỏng được giữ và chặn ghi; không có quyền BIND_APPWIDGET đặc biệt.
- Hình nền động được chọn trong UI Android; nút Dùng hình nền hệ thống bật hiển thị trên Home. Mở rồi hủy picker không tự đổi hình nền riêng. Cài ảnh SAF mới hoặc dùng nền mặc định tắt chế độ hệ thống.
- Dữ liệu cấu hình lỗi được giữ trong vùng riêng trước khi phục hồi. Không có tài khoản, backend, telemetry hoặc quyền camera/overlay.

## Wincavn và tài nguyên

Adapter chỉ mở activity gốc đã xác minh bằng Manifest và kiểm tra runtime; không gọi Binder/service/broadcast vendor. Mở camera thủ công không chứng minh camera tự bật khi lùi. [Feature parity](docs/research/feature-parity-matrix.md) ghi riêng implementation, verification và phần cần thiết bị thật.

Catalog có 12 yêu cầu thủ công: camera, nhạc, Bluetooth, Settings, radio, video, EQ, quản lý tệp, AUX, hướng dẫn, Bluetooth audio và điều hướng. Hai mục cuối dùng component riêng thay vì launcher chung của package. `Opened` chỉ nghĩa Android nhận yêu cầu; phát radio/media, DSP/routing, default navigation, CAN và firmware vẫn cần contract/bằng chứng riêng. Quyền `REQUEST_DELETE_PACKAGES` bình thường chỉ phục vụ hộp thoại gỡ ứng dụng do người dùng yêu cầu; không có DELETE_PACKAGES đặc quyền.

Agent mới đọc AGENTS/STATUS/parity và source trước; `.ua/knowledge-graph.json` hỗ trợ tra cứu kiến trúc. [Workflow chuyển giao](docs/architecture/architecture.md#chuyển-giao-ngữ-cảnh-cho-agent) giải thích kiểm tra freshness khi source chưa commit, refresh có giới hạn và dashboard loopback. Không bật hook tự động hoặc tự publish graph.

Không đóng gói APK vendor, mã decompile hay tài nguyên từ Apple UI Kit. Ba font có trạng thái `USER_CONFIRMED` theo AGENTS.md §22; không lấy font từ UI Kit. Icon TN do người dùng cung cấp; mapping theme và giấy phép trong [icon mapping](docs/design/icon-mapping.md). Người dùng đã yêu cầu “Commit và push lên nhánh main”; phạm vi source đã rà gồm ba binary `tn_new_york.ttf`, `tn_sf_body.otf`, `tn_sf_heading.otf`, khớp byte/hash với nguồn. Archive Wincavn, dữ liệu riêng, thư mục font nguồn và APK/build output không được đưa vào commit. Kiểm thử được thực hiện trên worktree triển khai trước lần commit này, với HEAD nền `4491b15`; SHA commit và kết quả push phải đối chiếu Git, không suy ra từ báo cáo kiểm thử.

Mọi kiểm thử Wincavn/xe cần cổng emulator hoàn tất và cho phép riêng. Hướng dẫn: [installation](docs/release/installation-guide.md), [rollback](docs/release/rollback-guide.md). Không có bằng chứng `DEVICE_VERIFIED` hoặc `AUTOMOTIVE_VERIFIED` trong phiên triển khai này.
