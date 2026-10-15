# Kiểm chứng Android 10

15/10/2026 — **EMULATOR_GATE_PASSED cho phạm vi increment Android trên hai profile/animation1.0 bên dưới**. Build/unit/lint,33instrumentation và9E2E mỗi profile PASS; assertion phân trang được xác nhận riêng lại ở1280 sau sửa harness, rồi E2E1280 cũng chạy lại9/9. **Full original reconstruction INCOMPLETE; Figma NOT_READY.** Không có DEVICE_VERIFIED hoặc AUTOMOTIVE_VERIFIED.

## Baseline đã đối chiếu

APK MVP trước sửa có17,120,134byte, SHA-256 `46f72880f8901d320fb89c135ff250ee7ab58840e17cc3cba065de7061074f97`. Đã đối chiếu artifact và bằng chứng giữ lại:19 unit, lint0errors/45warnings,11 instrumentation và8 E2E mỗi profile. Các kết quả cũ đúng trong phạm vi MVP; không chứng minh parity Wincavn và không phải checksum APK hiện tại.

## Môi trường thực tế

- Windows; Java21.0.8 được chọn theo tiến trình, Java8 mặc định không đổi. Gradle8.11.1 / AGP8.10.1 / Kotlin2.1.21; min29, compile/target35, bytecode JVM17.
- AVD `TN_Launcher_API29`, Google APIs x86_64 API29, WHPX, emulator37.2.12.0, SwiftShader. Serial `emulator-5580`, qemu1/API29 được kiểm tra trước cài/chọn Home.
- Viewport landscape thực1280×720/160dpi và1920×1080/320dpi đã được kiểm tra bằng dumpsys display và PNG IHDR. Đây là cấu hình thử, chưa phải số đo màn hình xe.
- Regression cuối dùng platform animation scales1.0, font scale1.0. Instrumentation riêng kiểm tra chữ lớn1.5, LIGHT/DARK, glyph Việt và persistence. Đã nhìn trực tiếp màn Widgets rỗng và editor thư mục1.5 ở1920; cancel editor giữ nguyên Preferences bytes.

## Kết quả và lệnh tái lập

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
- SHA-256: `eba36322e7a0f0872c47019f38f09ab3cd3d909459480d8e925d5f24f590951c`.
- `apksigner verify --verbose`: PASS, v2, một signer Android Debug. `aapt dump badging/permissions`: đúng label/ID/version/API.
- Home exported/singleTask, MAIN/HOME/DEFAULT và MAIN/LAUNCHER. WidgetsActivity không exported. Quyền bình thường REQUEST_DELETE_PACKAGES chỉ mở xác nhận gỡ; còn permission signature nội bộ AndroidX. Không sharedUID/DELETE_PACKAGES/BIND_APPWIDGET/camera/storage/overlay/vendor permission, service hoặc boot receiver.
- Không có test fixtures/probe, APK gốc, mã decompile, thư viện vendor hoặc Apple UI Kit trong APK chính. Native libraries là dependency DataStore. Ba font user-authorized vẫn đóng gói; nguồn gốc giữ nguyên, USER_CONFIRMED theo AGENTS22, không tái kiểm giấy phép.
- Kiểm thử chạy trên worktree triển khai trước commit, với baseline main/HEAD `4491b15`. Người dùng sau đó yêu cầu “Commit và push lên nhánh main”; source và đúng ba font USER_CONFIRMED đã rà file/byte/hash nằm trong phạm vi này. SHA commit và kết quả push đối chiếu Git. Archive vendor, dữ liệu riêng và APK/build output không được publish; không cài xe hoặc phát hành APK.

## Lint đã điều tra

51warnings, tăng6 so với baseline45: UseKtx21; GradleDependency12; SetTextI18n6; UseCompatLoadingForDrawables2; HardcodedText2; DiscouragedApi2; mỗi loại IconLauncherShape/OldTargetApi/DataExtractionRules/AndroidGradlePluginVersion/Overdraw/NotifyDataSetChanged có1. Không có lỗi correctness/NewApi.

Version notices không tự nâng dependency/toolchain đã khóa. DiscouragedApi là landscape của Home/Widgets theo phạm vi xe; chưa khẳng định phù hợp mọi form factor. Text/i18n còn nợ chuẩn hóa resources; UI hiện tiếng Việt. Overdraw/NotifyDataSetChanged là nơi cần đo thêm, không kết luận là nguyên nhân jank chỉ từ lint. Không thêm suppression hoặc xóa code để làm đẹp số cảnh báo.

## Hiệu năng quan sát

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
