# APK và cài đặt thử nghiệm

MVP cá nhân: `vn.tnlauncher.app`, debug `0.1.0` (1), APK tại `app/build/outputs/apk/debug/app-debug.apk`. SHA-256 và kết quả xác minh cuối cùng xem [compatibility report](../testing/compatibility-report.md).

## Emulator

Chỉ cài vào serial emulator đã kiểm tra `ro.kernel.qemu=1` và API29. `tools/verify-emulator.ps1` kiểm tra điều kiện này trước thao tác cài/chọn Home. Image sử dụng: `system-images;android-29;google_apis;x86_64`. Dữ liệu AVD, logs và screenshot nằm ngoài repo.

```powershell
./tools/verify-emulator.ps1 -Serial emulator-5580 -Profile 1280
./tools/verify-emulator.ps1 -Serial emulator-5580 -Profile 1920
```

Sau instrumentation, Android test tooling có thể gỡ APK kiểm thử/target. Để xem UI, cài lại APK debug vào emulator và chọn TN Launcher làm Home trên emulator đó. APK androidTest chứa fixture, không dùng trên xe hoặc bàn giao như sản phẩm.

Bản phục dựng bổ sung quyền Android bình thường `REQUEST_DELETE_PACKAGES` để mở hộp thoại gỡ ứng dụng do người dùng yêu cầu. Không có quyền `DELETE_PACKAGES`, bind widget đặc quyền hoặc quyền UID hệ thống. Widgets yêu cầu consent/configuration của Android/provider; hủy không cài hoặc gỡ ứng dụng nào.

## Wincavn

Phiên này không cài, gỡ hoặc đổi Home trên Wincavn. AirDroid không chứng minh ADB/shell/quyền hệ thống. APK debug không có quyền UID hệ thống hoặc khóa ký vendor.

Chỉ xem xét bước thiết bị thật sau `EMULATOR_GATE_PASSED`, xác minh lối khôi phục độc lập và cho phép cụ thể cho thao tác cài/chọn Home. Xe đứng yên, có người giám sát tại xe; giữ nguyên Launcher/camera/service gốc. Xem [rollback](rollback-guide.md) trước mọi thử nghiệm được cho phép sau này. Cài APK hoặc mở shortcut camera không chứng minh tương thích camera tự lùi.
