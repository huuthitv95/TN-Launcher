# Kiến trúc TN Launcher

Một module `app`, Kotlin/Views, API29+. `LauncherApplication` giữ repository cấp application; Activity chỉ dựng UI và nhận thao tác. Không có backend, telemetry, foreground service, boot receiver hoặc overlay.

`HomeActivity`/ViewBinding → `LauncherViewModel`/StateFlow → các repository/adapter. Dependency truyền constructor; không dùng service locator ngoài composition root Application hoặc framework DI. ViewModel sống qua Activity recreation, giữ công việc import ảnh ngoài lifecycle Activity.

| Contract | Trách nhiệm |
| --- | --- |
| LauncherAppRepository | MAIN/LAUNCHER query, ComponentName, label/icon trên IO, typed launch result |
| LauncherSettingsRepository | Preferences DataStore, transactional update, phục hồi dữ liệu và retention ảnh riêng |
| DeviceCapabilities / WincavnIntegration | Dò package/component/exported/enabled/suspended/quyền; chỉ mở activity từ thao tác người dùng |
| CameraIntegration | Mở camera thủ công; không có contract giả cho reverse/firmware |
| WallpaperImporter | SAF URI → temp stream tối đa32MB → decode cạnh tối đa2048 → JPEG riêng; cleanup file mới khi lỗi/cancel |
| LauncherTypography | Ba font static người dùng cho phép; sans cho nội dung/tiêu đề, serif chỉ brand Latin cố định; lỗi tải dùng system sans |
| FolderRules / FolderCodec | Tên/thành viên/thứ tự thư mục, membership duy nhất, giữ component tạm thiếu; cùng giao dịch DataStore với Home |
| LauncherWidgetRepository / WidgetsActivity | Bookkeeping riêng và AppWidgetHost chuẩn Android; bind/configure do hệ thống/provider xác nhận, RemoteViews thật ở màn Tiện ích |
| AppManagement / SystemSettingsIntegration | Trang thông tin, yêu cầu gỡ có xác nhận, Wi-Fi/ngày giờ/global search/live wallpaper qua intent Android đã resolve |

Home lưu thứ tự component và favorite độc lập danh sách package hiện tại. App tạm thiếu không bị xóa khỏi cấu hình; UI dùng trạng thái thiếu rõ ràng. Apps mới được nối theo thứ tự tên chuẩn hóa tiếng Việt. Kéo chỉ đổi subset đang hiển thị; menu có bước di chuyển tương đương qua ranh giới trang.

Repository tái sử dụng tên/icon theo component, updateTime/sourceDir/UID và bản sao Configuration. Mỗi refresh vẫn query và kiểm tra quyền/enabled/exported; Mutex bảo vệ cache, cancellation được kiểm tra trước publish, cache của app vắng bị loại nhưng bố cục lưu vẫn giữ. Refresh bình thường không bật loading lại khi đã có app; UI chỉ dựng lại palette khi theme đổi và thay item animator khi giảm chuyển động đổi trạng thái.

Settings dùng protobuf Preferences DataStore trong `files/datastore/launcher.preferences_pb`. Lỗi protobuf hoặc giá trị sai kiểu/nội dung được giữ nguyên bytes trong `files/recovery/` trước khi phục hồi. Nếu không bảo toàn được file gốc, không ghi đè. Lỗi đọc IO giữ Home bằng cấu hình mặc định và chặn ghi trong phiên đó. Hình nền cũ do app tạo chỉ được xóa sau commit thay thế thành công; không quét/xóa file không biết chủ sở hữu.

Package/time receiver đăng ký khi STARTED và hủy khi STOPPED. Không polling vendor, không tự mở lại Home từ background. Quyền/package gốc chỉ được coi khả dụng sau kiểm tra runtime; các class vendor không nằm trong UI. `Opened` nghĩa Android nhận yêu cầu mở activity, không chứng minh hình camera hoặc thiết bị xe hoạt động.

Widgets dùng host ID riêng và SharedPreferences JSON version 1. Allocation và lưu pending được bảo vệ trước cancellation; cleanup dùng chung mutex với complete để không xóa widget đã commit hoặc pending của luồng khác. Record sai kiểu/hỏng chặn ghi và giữ raw; ID Android không khớp bookkeeping được giữ để tránh xóa tiện ích của người dùng. Không có bind quyền đặc biệt, polling hoặc tự launch Home. Màn Widgets riêng chưa hỗ trợ kéo/resize widget trong lưới Home.

## Chuyển giao ngữ cảnh cho agent

Agent mới đọc `AGENTS.md`, `docs/progress/STATUS.md` và feature-parity matrix trước khi sửa code. Source hiện tại là chuẩn implementation; kết quả kiểm thử và chứng cứ APK gốc quyết định mức kiểm chứng. `.ua/knowledge-graph.json` chỉ giúp tra kiến trúc, dependency và tour, không thay thế các nguồn này hoặc chứng minh chức năng trên xe.

- Tra node theo `filePath`/tên bằng `rg`, theo edge liên quan rồi mở source thật. Có thể dùng `$understand-chat`, `$understand-explain` và `$understand-diff`; diff overlay chỉ giữ cục bộ.
- Trước khi tin graph, kiểm tra `project.gitCommitHash`, staged diff, working diff và file mới/xóa. `gitCommitHash` là commit nền lúc phân tích, không phải SHA của commit chứa chính graph; đối chiếu nội dung từng file mới quyết định freshness. Mỗi file-level node có `contentHash`, `hashAlgorithm: sha256-lf-normalized-bytes`: chuẩn hóa CRLF thành LF trước khi SHA-256 để checkout Windows/Linux không báo stale sai. Khi kiểm tra commit, so với bytes từ `git show HEAD:<filePath>` theo cùng quy tắc. Fingerprint/meta và `.ua/local-freshness.json` chỉ hỗ trợ kiểm tra cục bộ; không dùng chúng làm bằng chứng publish.
- Sau thay đổi có ý nghĩa, gọi skill `$understand --language vi --no-auto-update` trong ngữ cảnh TN Launcher; đây không phải lệnh PowerShell. Ưu tiên incremental cho phạm vi đổi thật, không rebuild toàn bộ theo thói quen. Nếu bộ kiểm tra symbol chặn cập nhật Kotlin do parser/ownership không đủ, dừng publish graph, kiểm tra source và thực hiện full refresh có giới hạn khi cần; không chép symbol cũ để lách cổng.
- Graph dùng scanner/import map/tree-sitter đã cài và semantic review. Kotlin/import wildcard hoặc nhiều class trong một file chưa được resolve đầy đủ; symbol bổ sung trực tiếp từ source được ghi `languageNotes`. XML/PowerShell/wrapper không được mô tả là có AST khi extractor không hỗ trợ. Coverage file không đồng nghĩa coverage toàn bộ symbol hoặc runtime.
- Tập chia sẻ tối thiểu đề xuất: `.ua/knowledge-graph.json`, `.ua/config.json`, `.ua/.understandignore` cùng tài liệu canonical hiện có. Không đưa tmp/intermediate, diff overlay, token URL, fingerprint/meta cục bộ hoặc dump vendor vào commit. Kiểm tra lại size, freshness và dữ liệu nhạy cảm trước khi đề nghị commit/push; chưa có quyền tự publish. Giữ `autoUpdate: false`, không bật post-commit hook.

Dashboard của installation hiện có có thể chạy trực tiếp ở terminal cục bộ sau khi graph qua validation, không cần cài/cập nhật plugin:

```powershell
$uaDashboard = Join-Path $env:USERPROFILE '.understand-anything/repo/understand-anything-plugin/packages/dashboard'
$env:GRAPH_DIR = (Get-Location).Path # Chạy từ thư mục TN Launcher.
$env:BROWSER = 'none'              # Không tự mở browser hoặc chạy BROWSER script.
Push-Location $uaDashboard
try { node ./node_modules/vite/bin/vite.js --host 127.0.0.1 --port 5173 --strictPort }
finally { Pop-Location }
```

Giữ server ở loopback, dừng bằng Ctrl+C. URL có token do server in chỉ dùng cục bộ; không chép token vào source, graph hoặc báo cáo chia sẻ. Việc dashboard khởi chạy thành công phải được báo từ kết quả thực tế, không suy ra từ đoạn lệnh trên. Chức năng freshness của dashboard gọi các lệnh Git chỉ đọc; nó vẫn báo dirty khi source chưa commit dù bytes khớp snapshot graph.
