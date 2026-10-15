# Kiểm kê tài nguyên Wincavn

Kiểm tra tĩnh ngày 15/10/2026 trên bản tải cục bộ `C:/Users/ZGAMESVN/Downloads/Resource_For_TN_Launcher/wincavn`. Không cài, chạy, sửa hay tải lên các APK gốc. Đây là bản sao do người dùng cung cấp, không chứng minh trạng thái ứng dụng đang cài hoặc toàn bộ firmware trên xe.

## Phạm vi và công cụ

- Thư mục gốc hiện có `APK/` và `File/`; không thấy `Video/` hoặc file CanBox ở cấp gốc trong lần liệt kê này. Không tải lại từ Drive.
- `APK/` có 4 APK cấp gốc; `APK/apks/` có 30 APK vendor. Đã đọc metadata/Manifest của 22 APK trong bảng dưới; 12 APK còn lại mới có tên file. Đây không phải phân tích đầy đủ mọi lớp trong 22 APK.
- `File/` có `AppCfg`, `3603d`, `Theme`, `touchCfg`, `carlink`, `CarMark` và các thư mục dữ liệu/diagnostic. Chỉ liệt kê tên thư mục; không đọc backups, crash, log hoặc dữ liệu riêng.
- AAPT `v0.2-11948202`, apksigner `0.9` thuộc SDK Build Tools `35.0.0`; Java `21.0.8` được chọn riêng theo tiến trình. Đã dùng `aapt dump badging`, `aapt dump xmltree … AndroidManifest.xml`, `apksigner verify --verbose --print-certs`, `Get-FileHash -Algorithm SHA256` và đọc danh mục ZIP `lib/`.

## APK đã kiểm tra

Tên file tương đối so với `APK/`. Version dưới đây là `versionCode`; không suy diễn thời gian phát hành từ số phiên bản.

| File | Package | Version | min / target SDK | Byte |
| --- | --- | ---: | --- | ---: |
| `VF3-190000007_com.android.launcher8.apk` | `com.android.launcher8` | 26031215 | 16 / 27 | 98,735,795 |
| `190000007_com.android.launcher7.apk` | `com.android.launcher7` | 26031214 | 16 / 27 | 103,166,137 |
| `Launcher3.apk` | `com.android.launcher3` | 1 | 28 / 29 | 4,032,139 |
| `apks/com.syu.fourcamera2.apk` | `com.syu.fourcamera2` | 2112291612 | 26 / 29 | 9,454,256 |
| `apks/com.syu.rightcamera.apk` | `com.syu.rightcamera` | 2125122009 | 16 / 26 | 3,556,087 |
| `apks/com.syu.doublecamera.apk` | `com.syu.doublecamera` | 2112291612 | 29 / 29 | 4,304,760 |
| `apks/com.syu.music.apk` | `com.syu.music` | 2124092317 | 22 / 27 | 1,496,347 |
| `apks/com.syu.bt.apk` | `com.syu.bt` | 2119010940 | 8 / 27 | 833,565 |
| `apks/com.syu.settings.apk` | `com.syu.settings` | 2125011015 | 21 / 27 | 9,485,857 |
| `190000000_com.syu.canbus.apk` | `com.syu.canbus` | 2125032710 | 19 / 19 | 84,920,312 |
| `apks/com.syu.canbus.apk` | `com.syu.canbus` | 2126091414 | 23 / 23 | 95,473,857 |
| `apks/com.syu.carui.apk` | `com.syu.carui` | 2126092411 | 8 / 16 | 28,195,101 |
| `apks/com.syu.radio.apk` | `com.syu.radio` | 25011809 | 16 / 26 | 1,060,302 |
| `apks/com.syu.carradio.apk` | `com.syu.carradio` | 2124091919 | 16 / 16 | 1,644,351 |
| `apks/com.syu.eq.apk` | `com.syu.eq` | 1 | 19 / 26 | 36,191,432 |
| `apks/com.syu.filemanager.apk` | `com.syu.filemanager` | 2124081916 | 19 / 24 | 1,173,951 |
| `apks/com.syu.onekeynavi.apk` | `com.syu.onekeynavi` | 23071809 | 16 / 16 | 290,561 |
| `apks/com.syu.video.apk` | `com.syu.video` | 2124102515 | 16 / 26 | 1,026,815 |
| `apks/com.syu.av.apk` | `com.syu.av` | 2124090910 | 16 / 26 | 735,451 |
| `apks/com.syu.carlink.apk` | `com.syu.carlink` | 2126092011 | 23 / 23 | 35,492,802 |
| `apks/com.syu.guide.apk` | `com.syu.guide` | 24091314 | 26 / 26 | 6,951,704 |
| `apks/com.syu.ms.apk` | `com.syu.ms` | 2126091217 | 24 / 24 | 8,263,069 |

Các `versionName` là `1.0`, ngoại trừ Bluetooth `5.0`, carlink `2.26.0920.1143`, ms `26.0912` và hai gói fourcamera2/doublecamera có chuỗi trống. Hai APK CAN cùng package nhưng khác hash, phiên bản và minSdk: không thay thế lẫn nhau, không cài thử. `aapt dump badging` của carradio thoát 1 do resource `android:icon` trỏ tới mục không tồn tại; phần package/version/SDK vẫn đọc được và `dump xmltree` thành công. Không sửa APK để che lỗi này.

## SHA-256 nguyên bản

| File | SHA-256 |
| --- | --- |
| `VF3-190000007_com.android.launcher8.apk` | `F6BB96726180B12284AF05261F5F3AFB464FC9C37A74A954D908C700A198BE84` |
| `190000007_com.android.launcher7.apk` | `66E84CD563D78BDBA87EE8F26F84FD5D9B0E61B9A56852C3221AC952D501E3AE` |
| `Launcher3.apk` | `60858FE8E00BFCE9AD5A15D2297F814B757AA907B90C6A11A0FC7AD0070F041D` |
| `apks/com.syu.fourcamera2.apk` | `81AEF2BF7A3E5ECBE7108058BC916C58DD65C3FC40C81CD8BF80F67368FB6AE8` |
| `apks/com.syu.rightcamera.apk` | `11608679BB5575DDD2591A2816D0DD64305B667D7E0DDF48001DC1931B6F3CAF` |
| `apks/com.syu.doublecamera.apk` | `88C340A84261D2076B7680D19A3A7BDD188F4CEDF17C261D0FF68C873691A345` |
| `apks/com.syu.music.apk` | `053213C679C33450E95EE1B6DB9D3B32EBDF7C7692F178CC41E637277DFC874D` |
| `apks/com.syu.bt.apk` | `7B1A7F74459F0C92640D7B41686CDBF8B8992C8621D3F66727ACBD398C73BC83` |
| `apks/com.syu.settings.apk` | `6CCBF1A434CA2D2AB7D27BFB332E93B72C4F6D3DD1122EEDD6F89FAE17319323` |
| `190000000_com.syu.canbus.apk` | `94895EA4B957768F7A9EC426144E6AB567470BACD74F21405A8546B0E8B3436F` |
| `apks/com.syu.canbus.apk` | `8EBD65505D0F13B1ECAC0FA7A720435A82F879DB647851659F4D5A355CE67A41` |
| `apks/com.syu.carui.apk` | `3DB7580A4D61C8A0BDBF6B9CB4307365419C5B4B429CF27872337324075AE3B0` |
| `apks/com.syu.radio.apk` | `031AC623FC80531390C7BB38D31DED69EF7181642D7F7622B75BDB9CB750DD6D` |
| `apks/com.syu.carradio.apk` | `E2FDDDC7DAA420A9F1A6A9583A0037AEDE77EFDE9BE495A137CD368EDFB0C6C5` |
| `apks/com.syu.eq.apk` | `FE2B081D766E9749D95CC50EA8C84941D94D14CEE7DDD353A53BFA03FB09647E` |
| `apks/com.syu.filemanager.apk` | `262A3B4AA6468AE6D2053BF2324722C9C69ABEB14AB95CD23B6121FCE41C45BF` |
| `apks/com.syu.onekeynavi.apk` | `F838B1FEDB813A68AD82DB33A3E789B023A42AA3F3D4F97B2D7E8385136607CD` |
| `apks/com.syu.video.apk` | `C30B883BCC2EE2079A5F85314A91B489A98DE8A3DCB5765570C7587161241AB4` |
| `apks/com.syu.av.apk` | `6BD035C2C5130C5777E07AC69D0D3768262323DF4CB52C7A3C80A1BF0F11F259` |
| `apks/com.syu.carlink.apk` | `18199E0AEA5086E2022A62AB756880267690A70659FD66BCB5B3FF75BA6D0294` |
| `apks/com.syu.guide.apk` | `D2DED2549A8B0948267FA6CF333C113EC8B9BF462510989AB3CEEF3FDD243ADD` |
| `apks/com.syu.ms.apk` | `0494EA8CA3BE1B317A832C5E58761DA5E6A31EBCAFD2662405E7ED1CD63043C` |

## Chữ ký và native

`apksigner verify --verbose --print-certs` thành công cho 21/22 APK; cùng digest chứng thư SHA-256 `c8a2e9bccf597c2fb6dc66bee293fc13f2fc47ec77bc6b2b0d52c11f51192ab8`. Subject chứng thư ghi Android; thông tin này không chứng minh nhà phát hành, quyền của TN Launcher hoặc chứng thư trên firmware đang chạy. Launcher3 dùng v2; carlink v1+v2; ms v2+v3; 18 mẫu còn lại v1. Không trích xuất khóa ký.

**EQ không vượt xác minh chữ ký:** `apksigner verify` thoát 1, `DOES NOT VERIFY` / `ERROR: No JAR signatures`. Chỉ sử dụng Manifest mẫu để định danh ứng viên activity; không coi mẫu đó là bản APK tin cậy/cài được hoặc bằng chứng package đang có trên xe. Không cài, ký lại hay sửa mẫu. Adapter TN vẫn phải kiểm tra package/component thực tại runtime; không phân phối mẫu này.

- Không có entry `lib/` trong 10 APK ngoài rightcamera và carui. Không đồng nghĩa không phụ thuộc JNI/platform/vendor thư viện ngoài APK.
- rightcamera có `libZipUtil.so`, `libc++_shared.so` cho arm64-v8a, armeabi, armeabi-v7a, x86, x86_64 và `lib/x86_64/libNative.so`.
- carui có thư viện qcar/image/audio/OSD cho arm64-v8a cùng ZipUtil/c++ và x86_64 Native. Chỉ kiểm kê tên/ABI; chưa phân tích chức năng native.
- Trong 10 mẫu bổ sung, radio/carradio/EQ/filemanager/onekeynavi/video/AV/guide không có `lib/`. Carlink có 30 entry `.so` ở arm64-v8a, armeabi-v7a, x86 và x86_64 cho audio/projection/receiver; chỉ kiểm kê ZIP. ms có `libsyu_uinput.so` cho cả bốn ABI. Không giải mã hoặc gọi JNI; tên thư viện không chứng minh hành vi hay tính tương thích.

Mẫu ms khai báo UID `android.uid.system`, EQ/video/AV cũng dùng system; filemanager/guide dùng `android.uid.shell`, carlink dùng `android.uid.systemui`. Đây là UID của gói gốc, không được sao chép sang TN Launcher. Radio để sharedUserId rỗng; carradio/onekeynavi không khai báo sharedUserId. Chi tiết entry point và hợp đồng Binder chưa đủ điều kiện triển khai nằm trong [phân tích Launcher](original-launcher-analysis.md).

Nguồn gốc Wincavn được bảo toàn ngoài Git. Không có APK, mã decompile hoặc log riêng được đưa vào repo; tài liệu chỉ giữ metadata và kết luận đã khử dữ liệu nhạy cảm.
