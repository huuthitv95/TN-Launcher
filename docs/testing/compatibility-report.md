# Kiểm chứng Android 10

## Phiên nghiên cứu OEM 18/10/2026 — kết nối và chẩn đoán trực tiếp

Owner cho phép quan sát OEM/chẩn đoán có mục tiêu qua scrcpy và xác nhận xe đứng yên, có người giám sát. Baseline main/0eebfc8f0b4f886673f191259dd87f32c06c6e81 không đổi source Android so với6a02097. Host `scrcpy --version` PASS3.3.4; `adb version` PASS1.0.41/platform-tools36.0.0. Dùng executable trong bộ scrcpy và server riêng, không thay hoặc dừng phiên ADB khác.

Lần đầu `adb -P <dedicated-port> connect <authorized-endpoint>` FAIL timeout10060; get-state không thấy thiết bị, devices rỗng. TCP FAIL trong khi ping PASS; lần thử lại bị từ chối kết nối. Qua AirDroid Business đã xem được màn hình OEM, mở Terminal theo yêu cầu Owner; nhập từ xa ban đầu không hoạt động, sau khi Owner bật/chọn AirDroid Keyboard đã chạy được các lệnh chỉ đọc. `getprop service.adb.tcp.port` ban đầu trả rỗng. Resolve Home từ UID Terminal bị từ chối user-scope; không cấp quyền để vượt lỗi đó.

Owner tiếp tục yêu cầu thực hiện đúng guide để kết nối scrcpy; yêu cầu mới cho phép riêng hai lệnh `setprop service.adb.tcp.port 5555`, `setprop ctl.restart adbd`. Đã gửi từng lệnh qua Terminal/AirDroid, đọc lại port5555; ADB riêng kết nối thành công, get-state=device. Không root mới, đổi SELinux, cấu hình nhà máy, cài TN hoặc đổi Home. IP/mã truy cập và đường dẫn data/app chứa token ngẫu nhiên không lưu trong repo.

| Check thực tế | Kết quả / giới hạn |
| --- | --- |
| scrcpy3.3.4 | PASS: helper90980bytes được đẩy/chạy, direct3d renderer, texture1280×800; no-control/no-audio/no-clipboard-autosync/no-power-on,1280px/30fps/2Mbps. Giữ cleanup mặc định; không tính cấu hình30fps là FPS đo được |
| Fingerprint runtime | OBSERVED: model uis8581a2h10_Automotive, Android10/API29, ABI arm64-v8a/armeabi-v7a/armeabi; wm physical1280×800/160dpi, khác hai profile emulator đại diện |
| Home hiện tại | OBSERVED: `cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME` → com.android.launcher8/.Launcher |
| Active APK | PASS7/7 SHA khớp inventory: L8/MS/US/CAN-new/FC/DC/RC. Dùng pm path bản active; các bản Hidden system packages trong dumpsys không phải active version |
| Package state user0 | L8/MS/US/CAN-new/RC enabled=0 (default), FC enabled=1, DC enabled=2 (disabled); installed=true/hidden=false/suspended=false cho bảy mẫu. Không enable/disable package |
| Property có mục tiêu | ro.build.fytmanufacturer=124; ro.fyt.canbus_id/type, persist.fyt.showfloatradar và persist.fyt.radar_click trả rỗng. Rỗng không chứng minh active CAN type hoặc radar on/off |
| UI OEM qua AirDroid/ADB screencap | OBSERVED: Home gốc có hình phía trước và ghép nhìn từ trên. Snapshot không đo freshness/độ trễ/chất lượng camera hoặc an toàn xe |
| Nút nổi hình xe, Owner chỉ định thao tác tự động | Đã gửi tap, một lần nhấn120ms và một click qua cửa sổ scrcpy có điều khiển vào đúng nút Owner xác định. Các snapshot sau vẫn mResumedActivity=L8; chưa thấy chuyển sang full-screen camera. Không ghi PASS cho việc mở activity FC hoặc camera ưu tiên |
| Window camera | OBSERVED: hai window com.syu.fourcamera2 ty=SYSTEM_DIALOG, isOnScreen/isVisible=true; frame[1183,604][1279,700] và[443,147][836,655] ở thời điểm đọc. Đây là cấu hình/layout đang thấy, không là precedence khi lùi |
| Camera tự lùi / radar transition / trở về Home từ activity camera riêng | Giai đoạn quan sát/nút nổi ban đầu: NOT_RUN. Ca lùi OEM được Owner cho phép tiếp theo và kết quả thực tế ghi riêng dưới đây; radar transition vẫn NOT_RUN |

### Baseline OEM tự chuyển khi lùi — 18-OEM-R01

Owner yêu cầu riêng “Bạn sẽ cùng tôi kiểm tra trực tiếp camera 360 độ tự chuyển khi lùi”, sau đó xác nhận xe đỗ an toàn, đứng yên, có người trực tiếp kiểm soát và khu vực thử đã trống. Quyền này thay giới hạn không thử số lùi của giai đoạn trước, chỉ cho ca OEM hiện tại; không cho phép cài TN/chọn Home hoặc tác động CAN. Người tại xe trực tiếp thao tác chọn R và trở về trạng thái đỗ. Trong ca này Codex chỉ đọc screencap/activity/window qua ADB, không gửi input, broadcast, Binder hoặc lệnh điều khiển xe.

Preflight đầu tiên thấy camera “Phía trước” toàn màn hình và có người gần xe; chưa hướng dẫn chọn R. Owner sau đó xác nhận đã về Home và khu vực trống; lấy lại baseline trước ca thử. Không gán lần thấy camera toàn màn hình này cho những lần chạm nút nổi trước đó vì chưa xác định trigger.

| Mốc / ca riêng | Bằng chứng trực tiếp và xác nhận Owner | Kết quả trong phạm vi OEM |
| --- | --- | --- |
| Trước R, Home gốc | Snapshot Home hiển thị P/0, ô camera “Phía trước” và ghép nhìn từ trên; mResumedActivity=com.android.launcher8/.Launcher. Window FC có frame[443,147][836,655]; đây là số đọc UI, không kiểm chứng CAN speed/gear/freshness | PASS: baseline được ghi trước trigger |
| Chọn R, không bấm nút camera | Owner xác nhận “Đã chọn R, camera tự chuyển sang phía sau”. Snapshot có nhãn “Phía sau”, hình ghép360 và camera toàn màn hình; mResumedActivity=com.syu.fourcamera2/com.syu.fourcamera.act.ActFourCamera. Window FC SYSTEM_DIALOG và activity frame[0,0][1280,800], isOnScreen/isVisible=true | PASS: quan sát một lần tự chuyển camera OEM từ Home L8; chưa kiểm chứng từ app khác/TN |
| Rời R về trạng thái đỗ, chưa bấm Home/Back | Owner xác nhận camera tự trở về Home. Snapshot và activity xác nhận Home L8 resumed, UI P/0, ô camera “Phía trước”/ghép360 trở lại; window camera frame[443,147][836,655] và nút nổi96×96 | PASS: tự trở lại bố cục Home gốc trong cùng chu kỳ |

Đã quan sát **một chu kỳ** OEM Home→camera phía sau toàn màn hình→Home. Xác nhận trigger vật lý dựa vào người tại xe; không đọc/inject gear qua CAN. Screenshot và activity/window là các mẫu tuần tự, không đồng bộ với thời điểm chọn số: **không đo độ trễ**, FPS, chất lượng/freshness hình, hiệu quả cảnh báo hay độ tin cậy lặp/soak. Radar, app khác ở foreground, cold boot/ACC, TN trên xe và rollback vẫn NOT_RUN. Không nâng TN lên DEVICE_VERIFIED/AUTOMOTIVE_VERIFIED; giữ nguyên trạng thái parity. Ảnh chứa cảnh riêng chỉ nằm trong TEMP ngoài Git; báo cáo giữ kết quả đã khử dữ liệu riêng.

Observer no-control của tác vụ đã kết thúc khi kiểm tra cuối; phiên scrcpy hiện có của Owner vẫn mở và ADB get-state=device. Không dừng phiên đó hoặc server ADB khác. ADB Wi-Fi còn bật theo yêu cầu kết nối; guide có cách tắt khi không sử dụng.

Không có TN DEVICE_VERIFIED/AUTOMOTIVE_VERIFIED mới. Build/unit/lint/instrumentation/E2E **NOT_RUN** trong increment tài liệu; cổng emulator dưới đây là bằng chứng lịch sử cho source không đổi. Rollback/chọn TN Home vẫn chưa kiểm chứng. Quan sát OEM và khả năng ADB shell không chứng minh ordinary-app TN được quyền gọi toolkit.

## Lịch sử — navigation 15/10/2026

15/10/2026 — increment navigation trên worktree `main/15dc34f`: **EMULATOR_GATE_PASSED cho phạm vi Android tại animation1.0**; build/unit/lint, instrumentation và E2E cả hai profile đã PASS cho APK mới. **Full original reconstruction INCOMPLETE; Figma deferred.** Không có DEVICE_VERIFIED hoặc AUTOMOTIVE_VERIFIED.

## Lượt contract và navigation hiện tại

Đã đối chiếu sâu toolkit/camera/CAN/media/navigation và hash nguyên bản22APK (sửa một lỗi chép SHA MS trong tài liệu). [Contract specification](../architecture/wincavn-integration.md) ghi bằng chứng, confidence và các lần decompile không thành công; không có APK vendor được chạy/cài, IPC hoặc thao tác xe. Thay đổi production duy nhất là exact navigation MainActivity/ACTION_MAIN thay direct LaunchNavi, theo bootstrap callback28 mới xác định. Nguy cơ mất command cold start là suy luận tĩnh, chưa là lỗi đã tái hiện/sửa trên vendor thật.

Lệnh thực tế: `gradlew.bat tasks --all --console=plain` PASS; `tools/verify-emulator.ps1 -Serial emulator-5580 -Profile 1280` và `-Profile 1920` chạy assembleDebug,25unit,lint0errors/51warnings và33instrumentation. Fixture kiểm tra đúng explicit screen/actionMAIN cùng các ca protected/missing/error cũ. Lượt cuối sau sửa fixture PASS33/33,0skip ở cả1280 (2m36s) và1920 (3m06s). E2E1280 PASS9/9 (177.49s), E2E1920 PASS9/9 (184.90s), cùng APK production SHA22c3c663…ff763c. E2E1280 chạy trước sửa ACK test-only; driver không dùng helper ComponentStateFixtureActivity nên kết quả này vẫn áp dụng cho đúng APK production không đổi. Logs/JSON/screenshots task ở TEMP/tn-launcher-analysis/root-contracts, ngoài repo.

Lượt1920 đầu **FAIL**1/33,0skip tại `disabledFixtureIsUnavailableAndOriginalComponentStateIsRestored`: lần re-enable thứ ba expected1/actual2. Harness chỉ đợi PM bằng giá trị rồi tiếp tục, helper chỉ xử lý onCreate; log có ba activity requests sát nhau, không thấy background-start denial trong đoạn đã kiểm tra. Pending/reused request là giả thuyết phù hợp, chưa chứng minh chính xác cơ chế giao activity của platform. Sửa test-only bằng ResultReceiver/latch mới cho từng request, ACK sau PM write/readback thật, xử lý onNewIntent và giữ finally restore. Không tăng timeout5s, bỏ assertion hoặc fake PackageManager. `assembleDebugAndroidTest` PASS; focused `am instrument -w -r -e class vn.tnlauncher.app.ActivityAccessInstrumentedTest#disabledFixtureIsUnavailableAndOriginalComponentStateIsRestored vn.tnlauncher.app.test/vn.tnlauncher.app.LauncherTestRunner` lặp4/4 PASS trên1920, rồi full1920 và full1280 đều PASS33/33. Không tính lượt FAIL vào cổng; giữ log đầu và kết quả sửa riêng.

Stock graph `prepare-incremental.mjs <repo> 15dc34f…` thực tế thoát1 vì relevant uncommitted changes; không commit/stash để vượt guard. Working-tree refresh có giới hạn dùng stock scanner/import/extractor, rà source/lexical scope, bảo toàn identities/edges, schema chính thức và SHA chuẩn hóa LF. Đây không phải stock incremental PASS. Graph chia sẻ đã ghi321nodes/449edges/68files, đạt schema chính thức,68 LFhash và inventory ổn định, giữ47 source identities trước cùng4 callable mới được ghi đúng scope. Tổng449edges tính cả liên kết Markdown thật từ STATUS đến đặc tả contract mới. Graph được ghi sau freeze tài liệu; kiểm tra marker/contentHash khi tiếp tục. Local committed fingerprints/meta không được giả nâng baseline.

## Baseline đã đối chiếu

APK MVP trước sửa có17,120,134byte, SHA-256 `46f72880f8901d320fb89c135ff250ee7ab58840e17cc3cba065de7061074f97`. Đã đối chiếu artifact và bằng chứng giữ lại:19 unit, lint0errors/45warnings,11 instrumentation và8 E2E mỗi profile. Các kết quả cũ đúng trong phạm vi MVP; không chứng minh parity Wincavn và không phải checksum APK hiện tại.

## Môi trường thực tế

- Windows; Java21.0.8 được chọn theo tiến trình, Java8 mặc định không đổi. Gradle8.11.1 / AGP8.10.1 / Kotlin2.1.21; min29, compile/target35, bytecode JVM17.
- AVD `TN_Launcher_API29`, Google APIs x86_64 API29, WHPX, emulator37.2.12.0, SwiftShader. Serial `emulator-5580`, qemu1/API29 được kiểm tra trước cài/chọn Home.
- Viewport landscape thực1280×720/160dpi và1920×1080/320dpi đã được kiểm tra bằng dumpsys display và PNG IHDR. Đây là cấu hình thử, chưa phải số đo màn hình xe.
- Regression cuối dùng platform animation scales1.0, font scale1.0. Instrumentation riêng kiểm tra chữ lớn1.5, LIGHT/DARK, glyph Việt và persistence. Đã nhìn trực tiếp màn Widgets rỗng và editor thư mục1.5 ở1920; cancel editor giữ nguyên Preferences bytes.

## Kết quả và lệnh tái lập

Bảng sau là regression cuối cho APK navigation hiện tại; các lượt FAIL và dữ liệu lịch sử được phân biệt riêng trong báo cáo.

| Kiểm tra | Kết quả |
| --- | --- |
| `gradlew.bat tasks --all` | PASS; xác nhận tasks có thật trước dùng wrapper |
| `:app:assembleDebug` | PASS, debug0.1.0(1) |
| `:app:testDebugUnitTest` | PASS25/25: rules8, ViewModel11, folder/app-management6 |
| `:app:lintDebug` | PASS0errors/51warnings; không suppress lỗi |
| `:app:connectedDebugAndroidTest`,1280/160 | PASS33/33,0skip |
| `:app:connectedDebugAndroidTest`,1920/320 | PASS33/33,0skip |
| E2E1280/160 | PASS9/9 |
| E2E1920/320 | PASS9/9 |
| Manifest / signing / fixture exclusion | PASS; app ID, Home, v2 signature và không test runner/fixture trong DEX chính |
| Wincavn/xe | NOT_RUN; không cài APK vendor hoặc thay đổi thiết bị thật |

```powershell
./tools/verify-emulator.ps1 -Serial emulator-5580 -Profile 1280
./tools/verify-emulator.ps1 -Serial emulator-5580 -Profile 1920
python -X utf8 tools/emulator-e2e.py --adb <SDK>/platform-tools/adb.exe --serial emulator-5580 --fixture-apk app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk --wallpaper app/src/main/res/drawable-nodpi/tn_brand.png
```

Sau instrumentation, cài lại APK debug/androidTest nếu tooling đã gỡ. Driver từ chối serial thật/API khác; fixture APK được kiểm tra đúng package trước cài lại. E2E gồm search/Back, pin, theme/restart, app thật/foreground/Recents, tạo thư mục, process death, gỡ qua xác nhận Android/cài lại/cập nhật, SAF và metrics. Thư mục/favorite giữ cấu hình khi fixture thiếu rồi phục hồi khi package trở lại; harness tìm qua các trang Home thật.

Probe trong LauncherTestRunner chỉ thuộc APK test: xác nhận emulator/API29/token, mở Home rồi fixture, tự kết thúc PID của chính tiến trình. Host xác minh PID cũ mất, PID mới và state/UI phục hồi. Việc instrumentation bị chấm dứt ở probe là dự kiến, tách khỏi bộ JUnit33 ca; không dùng root/SELinux hoặc force-stop thay ca process death.

Bộ33 gồm Launcher11, Reconstruction8, ActivityAccess4, Widgets4, WidgetStorage6. Widgets dùng provider test thật và RemoteViews: bind cho phép/từ chối, configure/cancel/recreate, update/click, đóng/mở lại, remove, provider thiếu; storage kiểm tra corruption/wrong-type pending, cancellation trong allocation và bảo vệ widget đã commit. Fixture Java độc lập Kotlin runtime của app chính; không là ứng dụng vendor giả lập.

## Artifact hiện tại

- `C:/Users/ZGAMESVN/Downloads/TN-Launcher/app/build/outputs/apk/debug/app-debug.apk`
- `vn.tnlauncher.app`, variant debug, version0.1.0, versionCode1;17,544,918byte.
- SHA-256 sau sửa navigation: `22c3c663b46a68d71b7c896c1d0649bd2896758db23ed04fbb98c3afc8ff763c`. APK trước là `eba36322e7a0f0872c47019f38f09ab3cd3d909459480d8e925d5f24f590951c`; không có bản APK dư được chép vào repo.
- `apksigner verify --verbose`: PASS, v2, một signer Android Debug. `aapt dump badging/permissions`: đúng label/ID/version/API.
- Home exported/singleTask, MAIN/HOME/DEFAULT và MAIN/LAUNCHER. WidgetsActivity không exported. Quyền bình thường REQUEST_DELETE_PACKAGES chỉ mở xác nhận gỡ; còn permission signature nội bộ AndroidX. Không sharedUID/DELETE_PACKAGES/BIND_APPWIDGET/camera/storage/overlay/vendor permission, service hoặc boot receiver.
- Không có test fixtures/probe, APK gốc, mã decompile, thư viện vendor hoặc Apple UI Kit trong APK chính. Native libraries là dependency DataStore. Ba font user-authorized vẫn đóng gói; nguồn gốc giữ nguyên, USER_CONFIRMED theo AGENTS22, không tái kiểm giấy phép.
- Regression cũ chạy trên worktree nền `4491b15`, sau đó source được commit/push thành `15dc34f`. Lượt navigation hiện tại là thay đổi local trên `main/15dc34f`; yêu cầu tiếp theo của người dùng cho phép commit/push source lên main, với SHA và kết quả push đối chiếu Git. Archive vendor, dữ liệu riêng và APK/build output không publish; không cài xe hoặc phát hành APK.

## Lint đã điều tra

51warnings, tăng6 so với baseline45: UseKtx21; GradleDependency12; SetTextI18n6; UseCompatLoadingForDrawables2; HardcodedText2; DiscouragedApi2; mỗi loại IconLauncherShape/OldTargetApi/DataExtractionRules/AndroidGradlePluginVersion/Overdraw/NotifyDataSetChanged có1. Không có lỗi correctness/NewApi.

Version notices không tự nâng dependency/toolchain đã khóa. DiscouragedApi là landscape của Home/Widgets theo phạm vi xe; chưa khẳng định phù hợp mọi form factor. Text/i18n còn nợ chuẩn hóa resources; UI hiện tiếng Việt. Overdraw/NotifyDataSetChanged là nơi cần đo thêm, không kết luận là nguyên nhân jank chỉ từ lint. Không thêm suppression hoặc xóa code để làm đẹp số cảnh báo.

## Hiệu năng quan sát

Số đo APK navigation hiện tại, thu trong E2E; không dùng để suy ra cải thiện render từ thay đổi activity entry:

| Profile | Cold TotalTime3 mẫu; median | Idle CPU một core / PSS KB | PSS sau20 chuyển app | Frame p50/p95; jank |
| --- | --- | --- | --- | --- |
|1280×720/160|1218,1370,1008ms;1218ms|0.000% /70,862|87,763|18/69ms;24/40(60.00%)|
|1920×1080/320|1442,1270,1240ms;1270ms|0.328% /73,726|82,303|26/125ms;28/42(66.67%)|

CPU đo lần lượt3.027s/3.051s. Cả hai E2E không quan sát crash/ANR Launcher trong buffers kể từ marker. Đây là mẫu ngắn và hai snapshot PSS, chưa là soak/leak test hoặc benchmark có kiểm soát. Frame timing1920 vẫn là giới hạn cần đo riêng, không cam kết FPS xe.

Bảng này giữ số đo lịch sử của APK `eba36322…590951c`; không tự gán cho APK navigation mới. Số đo hiện tại được tách ở trên; không suy ra cải thiện render từ thay đổi activity entry.

| Profile | Cold TotalTime3 mẫu; median | Idle CPU một core / PSS KB | PSS sau20 chuyển app | Frame p50/p95; jank |
| --- | --- | --- | --- | --- |
|1280×720/160|789,890,870ms;870ms|0.000% /74,549|89,528|11/38ms;9/44(20.45%)|
|1920×1080/320|763,773,781ms;773ms|0.000% /74,439|82,883|20/81ms;37/43(86.05%)|

Cold start giữ fixture foreground, force-stop Home, kiểm tra không PID rồi am start -W và xác minh PID mới. CPU là delta utime+stime trong khoảng3giây; bộ nhớ chỉ là hai snapshot, không chứng minh không rò heap. Không quan sát crash/ANR Launcher trong buffers của các E2E cuối; buffers hữu hạn, không phải soak test. Không có polling vendor/công việc nền lặp được thêm trong increment.

Jank1920 còn cao trên SwiftShader; PASS ở journey metrics nghĩa thu thập được số đo và hoàn tất thao tác. Không nghĩa60FPS hoặc đã giải quyết hiệu năng. So với baseline median1087/1146ms và p9573/73ms, kết quả host thay đổi theo tải/môi trường; không phải benchmark có kiểm soát và không suy ra GPU/FPS xe. Cần pilot render/overdraw có phép đo riêng nếu tiếp tục tối ưu.

## Lượt lỗi và giới hạn môi trường

Các lượt abort trước đó không được tínhPASS. API29 image từng crash native `surfaceflinger`, SIGABRT `ubsan: add-overflow`, stack Rect::offsetBy → Region::translate → Layer::fillInputInfo, trong hành trình dialog và uninstall; sau đó system services mất. Đã ghi nhận ở10:35/10:42/10:45/10:48. Khi ấy scales0.0; đã reboot AVD không wipe, giữ scales1.0 và tắt việc Gradle tự ép0, rồi cả suites/E2E hoàn tất. Đây là tương quan thử nghiệm, chưa chứng minh nguyên nhân hoặc sửa platform. Không đổi firmware/quyền hoặc bỏ test để che crash. Cấu hình0 chưa được chứng nhận ổn định trên image này.

[AOSP Android10 Layer.cpp](https://android.googlesource.com/platform/frameworks/native/+/android-10.0.0_r47/services/surfaceflinger/Layer.cpp#2040) có đường translate trong fillInputInfo; [AOSP mới](https://android.googlesource.com/platform/frameworks/native/+/5a3e356fe0/services/surfaceflinger/Layer.cpp) có guard overflow. Tham chiếu giải thích loại lỗi; không chứng minh binary/image hay trigger cụ thể của TN.

Các lỗi đã sửa gồm allocation widget bị cancellation mất ID, cleanup đụng widget đã commit và pending JSON sai kiểu bị bỏ qua. Harness cũng được sửa để tương tác cửa sổ Home thật sau app ngoài và tìm app đã khôi phục qua phân trang. Lỗi đầu fixture Kotlin process riêng thiếu stdlib được sửa bằng fixture Java framework-only. Các lượt hỏng giữ FAIL; kết quả bảng chỉ dùng các lượt regression đạt sau sửa.

Image khóa global night mode ởlight; SYSTEM được kiểm theo trạng thái thật. LIGHT/DARK riêng của app vẫn được thử. Không cấp quyền/đổi policy để giả lập OS night thành công.

## Cổng và giới hạn chức năng

**EMULATOR_GATE_PASSED** trong phạm vi chức năng Android đã nêu, trên scales1.0. Đã rà source/Manifest/signature, diff/ignore và artifact. Source/tài nguyên gốc giữ nguyên; logs/screenshots và phân tích APK ởTEMP, build tại thư mục Gradle-managed. Knowledge graph là handoff source, không thay kiểm thử hoặc nâng mức vendor.

Folders chỉ ở Home; Dock là component apps. Widgets ở màn riêng, chưa drag/resize trên Home; corruption fail-closed giữ raw, không tự xóa dữ liệu/ID không rõ chủ. Global search cần provider; live wallpaper dùng UI/selection Android, chưa xác minh provider OEM. Không hoàn tất legacy shortcut/skin plugin dormant chỉ bằng chức năng TN khác.

12manual vendor requests chỉ IMPLEMENTED/STATIC_VERIFIED đối với vendor thật. Radio/media transport, DSP/routing, CAN, default navigation backend, camera tự lùi/overlay, ACC/firmware và rollback Wincavn vẫn chưa kiểm chứng/thiếu contract. Xem [parity matrix](../research/feature-parity-matrix.md) và [checklist thiết bị](device-test-checklist.md). Không đề nghị thử xe hoặc chuyển sang Figma từ kết quả emulator này.
