# Phân tích Launcher gốc và entry point Wincavn

Mức bằng chứng: **STATIC_VERIFIED** ngày 15/10/2026. Chưa quan sát thiết bị thật. Hash và phiên bản mẫu nằm trong [kiểm kê tài nguyên](wincavn-resource-inventory.md).

## Bằng chứng và cách tái lập

AAPT/SDK Build Tools 35.0.0 đọc metadata và Manifest của các APK; apksigner xác minh chữ ký. Apktool `3.0.3` giải mã Manifest/resources launcher8; JADX bản `dev` chỉ decompile các lớp cần đọc. Đầu ra ở `%TEMP%/tn-launcher-analysis/`, không nằm trong repo. Các vị trí tham chiếu: `launcher8-resources/AndroidManifest.xml`, `Launcher.java` và `FytPackage.java`. Decompile hỗ trợ giải thích luồng gọi, không được xem là source nguyên bản hoặc bằng chứng runtime.

Lệnh đọc mẫu, với `$apk` là đường dẫn nguyên bản trong kiểm kê:

```powershell
& "$env:LOCALAPPDATA/Android/Sdk/build-tools/35.0.0/aapt.exe" dump badging $apk
& "$env:LOCALAPPDATA/Android/Sdk/build-tools/35.0.0/aapt.exe" dump xmltree $apk AndroidManifest.xml
& "$env:LOCALAPPDATA/Android/Sdk/build-tools/35.0.0/apksigner.bat" verify --verbose --print-certs $apk
Get-FileHash -LiteralPath $apk -Algorithm SHA256
```

## Ba Launcher khác nhau

| Mẫu | Home/entry point | Dấu hiệu kiến trúc |
| --- | --- | --- |
| launcher8 | `com.android.launcher8.Launcher`: MAIN/HOME/DEFAULT/MONKEY, `singleTask`, `nosensor` | `android.uid.system`; 5 activity, 6 service, 12 receiver, 1 provider |
| launcher7 | `com.android.launcher7.Launcher`: cùng bộ category, `singleTask`, `nosensor` | `android.uid.system`; cùng bố cục Manifest 24 component; chưa đối chiếu toàn bộ code |
| Launcher3 | `com.android.launcher3.Launcher`: MAIN/DEFAULT/LAUNCHER_APP; không thấy HOME trong Manifest đã đọc | `android.uid.system`; Quickstep `TouchInteractionService` cần `STATUS_BAR_SERVICE`, RecentsActivity và NotificationListener |

Launcher8/7 không khai báo `android:exported` ở Home; trên API 29, activity có intent filter được suy ra exported=true. Không có quyền ở application hoặc chính Home. Đây là suy luận từ Manifest mẫu, chưa xác nhận package/component enabled trong firmware thật. Launcher3 ở archive không phải bằng chứng của Home candidate đang hoạt động.

Launcher8 khai báo quyền đặc biệt như `BIND_APPWIDGET`, `CHANGE_CONFIGURATION`, `READ_LOGS`, `MOUNT_UNMOUNT_FILESYSTEMS`, `SYSTEM_ALERT_WINDOW`, cùng quyền vị trí, lưu trữ và điện thoại. `PRELOAD_WORKSPACE` có protection `signature|privileged`; `RECEIVE_LAUNCH_BROADCASTS` là `signature`. Việc gốc khai báo UID hệ thống và các quyền này không trao quyền tương đương cho `vn.tnlauncher.app`.

## Entry point có căn cứ để mở thủ công

Các activity trong bảng có MAIN/LAUNCHER, không khai báo exported riêng, không có permission riêng hoặc permission kế thừa từ application; trên API 29 được suy ra exported=true. `uses-permission` của gói đích là quyền gói đó xin sử dụng, không tự động trở thành quyền caller cần có để mở activity. Kiểm tra bằng PackageManager tại runtime vẫn bắt buộc.

| Gói | Activity | Action phụ tìm thấy | Cách TN Launcher sử dụng |
| --- | --- | --- | --- |
| `com.syu.fourcamera2` | `com.syu.fourcamera.act.ActFourCamera` | `com.syu.fourcamera` | Mở camera thủ công sau thao tác người dùng |
| `com.syu.music` | `com.syu.app.Activity_All` | `com.syu.music`, `.btav`, `.grid` | Mở giao diện nhạc gốc; chưa điều khiển phát nhạc |
| `com.syu.bt` | `com.syu.bt.BtAct` | `com.syu.bt` | Mở Bluetooth gốc; không đọc danh bạ/điện thoại |
| `com.syu.settings` | `com.syu.settings.MainActivity` | MAIN/LAUNCHER | Mở trang đầu; không tự đổi cấu hình xe |

`Launcher.java:4982` gọi `FytPackage.getIntent(...fourcamera2Action)` cho nút 360. `FytPackage.getIntent` truy vấn activity MAIN theo package rồi dùng explicit component. Đây là bằng chứng đường vào thủ công.

Hai gói camera khác được phát hiện nhưng chưa được chọn làm fallback tự động: `com.syu.doublecamera.act.ActDoubleCamera` có MAIN/LAUNCHER; `com.syu.rightcamera.ActivityLaunch` có MAIN và action `com.syu.rightcamera` nhưng không có LAUNCHER category. Chưa xác minh camera nào phù hợp thực tế Limo Green; mở nhầm loại camera không phải fallback an toàn.

## Camera tự động, media và dependency

- `Launcher.java:421,6266` có `FOURCAMERA2_BROADCAST_SEND`; nhánh nhận chỉ phối hợp `WindowUtil.visible/sendBROADCAST` với PiP. Không tìm thấy bằng chứng đầy đủ của đường tự chuyển khi vào số lùi trong phần code đã đọc.
- Manifest fourcamera2 có `ServiceMain` (action `com.syu.fourcamera`) và `ServiceCamera` (action `com.autonavi.amapauto.gdarcameraservice`); doublecamera có service tương tự và `.keepAlive`. Tên service/action không chứng minh giao diện được hỗ trợ để TN Launcher gọi. TN Launcher không start/bind các service này.
- Camera gốc xin `CAMERA`, `CAPTURE_SECURE_VIDEO_OUTPUT`, `CAPTURE_VIDEO_OUTPUT`, `SYSTEM_ALERT_WINDOW`, quyền media/storage và UID hệ thống. Những declaration này chưa cho biết đường dữ liệu camera hoặc chủ sở hữu overlay lùi thực tế.
- Launcher8 có `com.fyt.car.MusicService`, `VideoService`, `BtavService`, `com.fyt.widget.DvrService`, `com.syu.widget.music.RestartService` và debug `MemoryTracker`. Gói music có `MService` cùng các action next/prev/playpause; Bluetooth có service tương ứng. Chưa gọi các giao diện điều khiển này trong MVP.
- Launcher8 có receiver gallery, package changes, AUTONAVI, wallpaper, car logo, widget và preload/shortcut. Provider `com.android.launcher8.settings` exported=true, có read/write permission riêng. TN Launcher không đọc/ghi database hoặc broadcast của launcher gốc.
- Code Launcher tham chiếu `CarStates`, `RemoteTools`, `Callback`, `SyuJniNative` và system properties. APK launcher8 không chứa `lib/`; dependency native có thể thuộc hệ thống. Chưa xác định hợp đồng Binder/JNI có thể dùng từ app thường.

## Giới hạn adapter TN Launcher

`AndroidWincavnIntegration` thường ưu tiên launcher activity công khai của package, sau đó dùng component nêu trên; BT audio và navigation là explicit-only vì package launcher dẫn sang hành vi khác. Adapter kiểm tra enabled, exported và quyền activity; bắt lỗi không tìm thấy, thiếu quyền và runtime. UI chỉ báo entry point có thể mở, không báo camera hoặc điều hướng đã hoạt động trên xe. Không sao chép mã vendor, không dùng UID hệ thống, overlay, broadcast, Binder hoặc native vendor.

Các kiểm thử thiếu package/component/quyền và exception trên emulator chứng minh khả năng suy giảm của adapter. Chúng không chứng minh hoạt động camera, tự chuyển khi lùi, thứ tự overlay, CAN hoặc an toàn xe. Các mục này giữ `VENDOR_DEPENDENT` / `DEVICE_TEST_REQUIRED`; chỉ kiểm thử xe sau cổng emulator và cho phép riêng.

## Phạm vi phục dựng mở rộng và độ chắc chắn của hành vi gốc

Prompt phục dựng mới ưu tiên chức năng gốc trước Figma. Cổng emulator MVP trước đây chỉ là baseline; không phải xác nhận full parity. Đọc bổ sung 10 Manifest/metadata và các lớp đích qua JADX, không decompile hàng loạt hoặc thực thi/cài APK. Tổng cộng 22 mẫu có metadata; hash/chữ ký và lỗi mẫu EQ/carradio được ghi riêng trong kiểm kê.

Không có hành vi nào trong tài liệu này được quan sát trực tiếp trên Wincavn. Ba loại bằng chứng cần giữ riêng:

- **Có đường gọi trong Launcher:** các nhánh `onClick` và component/action dưới đây tồn tại trong DEX; trạng thái view còn tùy layout và cấu hình firmware.
- **Có cấu hình skin trong mẫu:** `res/layout-land/launcher.xml` chứa Workspace, hotseat, overview và apps_customize_pane. `res/values/bools.xml` có `apps_all_disable=false`, `apps_custom_page=true`, nhưng `iswidgetpages=false`, `apps_widget_design=false`, `wallpaper_show=false`, `apps_set_wallpaper=false`, `weather_show=false`. `res/xml/default_workspace.xml` rỗng. `Launcher.java:2549–2556` còn ẩn Workspace trừ khi `ro.build.fytmanufacturer` là 53 hoặc 124. Chưa có giá trị property trên firmware chạy thật.
- **Chỉ có khả năng trong code, chưa chứng minh được bật:** folder, generic AppWidget host, global search, legacy shortcut và live wallpaper. Không biến những mục này thành bằng chứng UI gốc đang hoạt động; cũng không gán chúng là lỗi phần cứng. TN chủ động triển khai folder/widget host trong đợt phục dựng này, độc lập với việc skin gốc bật chúng hay không.

Các số dòng dưới đây thuộc output JADX của đúng mẫu launcher8 đã có hash, tại `%TEMP%/tn-launcher-analysis/Launcher.java`, trừ khi ghi tên lớp khác. Chúng là vị trí tái lập cục bộ, không phải source vendor được nhập vào repo.

| Mã bằng chứng | Hành vi có trong mẫu | Vị trí / giao diện / giới hạn |
| --- | --- | --- |
| L8-HOME | Home, Back, Drawer, đổi trang và long press | `Launcher.java:4156–4183` MAIN/Home; `4538–4574` Back; `5089` Drawer; `5527–5598` Workspace overview/drag. Skin ảnh hưởng khả năng truy cập overview. |
| L8-APP | Xem thông tin và yêu cầu gỡ ứng dụng | `5125–5130` `android.settings.APPLICATION_DETAILS_SETTINGS` + `package:`; `5132–5143` `android.intent.action.DELETE` + `package:` và chặn system app theo flags. `DeleteDropTarget.java:243–306` phân biệt xóa shortcut với uninstall, kiểm tra lại app khi trở về. TN phải giữ xác nhận của Android, không silent uninstall. |
| L8-FOLDER | Tạo/lưu/mở/đổi tên/đóng folder | `4463–4472` `FolderInfo` + DB; `5387–5403`, `5488–5522` mở/đóng và xử lý edit-name; `DeleteDropTarget.java:277–280` xóa container. Code capability; runtime skin chưa biết. |
| L8-WIDGET | Host widget Android và quyền đồng ý của người dùng | `1476–1478` AppWidgetManager/LauncherAppWidgetHost/startListening; `4351–4367` configure; `4426–4441` allocate ID, bindIfAllowed, fallback `APPWIDGET_BIND`; `1980–2038` nhận kết quả/cancel. `iswidgetpages=false` ẩn nút widget ở `2566–2583`, không xóa cơ chế host. TN không cần mạo nhận `BIND_APPWIDGET`: dùng system consent. |
| L8-WIFI | Mở Wi-Fi Settings | `4962–4964` `android.settings.WIFI_SETTINGS`; là mở trang hệ thống, không tự đổi Wi-Fi. |
| L8-DATE | Mở cài đặt ngày/giờ | `4903–4907`, `4986–4990` `android.settings.DATE_SETTINGS`; không xin SET_TIME hoặc chỉnh giờ âm thầm. |
| L8-ENTRY | Mở UI ứng dụng vendor | Radio `4787–4803`; video `4814,4913`; filemanager `4892`; EQ `4975`; AUX `4824`; guide `5017–5020`; BT audio `3482,4917–4920`; camera `4982`. Kiểm tra bảng component dưới đây. |
| L8-RADIO | Hiển thị band/frequency, prev/next/pause và chọn radio OEM | `1538–1539` đăng ký RemoteTools module1; `3297–3338` gọi sendInt; `4788–4802` chọn carradio khi `CarStates.mExistCarRadio==1`, radio thường nếu không. Không biết cờ thực trên xe; không tự đoán carradio fallback. |
| L8-MEDIA | Nhạc/BT audio metadata, transport, seek; video transport | Nhạc `3341–3405` startService các action `.prev/.next/.playpause`; BT `3444–3484` tương tự; video `3515–3543` RemoteTools. `MusicService.java:42–70` nhận metadata từ vendor qua `com.fyt.launcher.music`, chuyển notifier/widget. Không chỉ là nút mở UI. |
| L8-NAV | Nút điều hướng mặc định và hướng dẫn rẽ | `3511,4770,4834` RemoteTools `sendInt(0,24,0)`; `3547–3764` hướng dẫn dựa trên dữ liệu receiver/ACC; Manifest có `MyAutoMapReceiver` action `AUTONAVI_STANDARD_BROADCAST_SEND`. Caller public activity trong onekeynavi xem phần dưới. |
| L8-AUDIO | Chọn nguồn, mute, volume | `4587–4591` module0/code0; `4967,4971,5027,5029` module4/code0. Mở app EQ không tương đương điều khiển âm thanh/đổi nguồn thật. |
| L8-DISPLAY | Brightness, đèn, tắt màn | `4955` module0/code10/-3; `4959` module0/code16/1; `5012–5014` `PopWindowBright`. Không thay các lệnh vendor bằng Settings.System rồi tuyên bố parity khi chưa biết đường điều khiển panel. |
| L8-CAR | Tốc độ, số P/R/N/D, đèn/cảnh báo | `734–945` Callback; `2190–2227` hiển thị số; `2516–2543` đổi km/h/MPH và animation; `1538–1543` đăng ký các module. Không có dữ liệu giả/fake status trong TN. |
| L8-BOOT | Khởi tạo app, ACC, SCREEN_OFF, PiP | `LauncherApplication.java:38–107` khởi tạo CarStates/skin/package state; `110–198` ghi system properties PiP. `Launcher.java:1183–1195` SCREEN_OFF và `4038` filter. Manifest launcher8 không có BOOT_COMPLETED receiver; `ms` mới có receiver đó. Không suy ra launcher chịu trách nhiệm toàn bộ wake/boot. |
| L8-THEME | Wallpaper, live wallpaper và `.skin` | `4479–4487` picker; `2559–2601` nút overview; `4600–4657` áp skin/resource; có flag skin tắt như trên. TN wallpaper private và Light/Dark là chức năng độc lập, chưa clone external skin hoặc live wallpaper gốc. |
| L8-EXTRA | Global search, legacy shortcut, voice, browser, weather | `4298–4330` GLOBAL_SEARCH; `4385–4398` CREATE_SHORTCUT; `4773–4784` voice; `5024–5064` browser; `1672–1707` weather. Browser có thể mở từ app list thật. Voice phụ thuộc package, weather phụ thuộc mạng/location/API vendor. Các chuỗi credential trong APK không được sao chép, dùng lại hoặc công bố. |

## Các activity bổ sung đã kiểm tra Manifest

Mọi activity trong bảng dưới không có caller permission ở activity/application. Trừ carlink khai báo exported=true rõ, các activity có intent-filter nên API 29 suy ra exported=true. Điều này không chứng minh enabled/install state hoặc thành công bên trong app đích. `sharedUserId` là của gói đích, không phải quyền mà TN nhận được khi mở nó.

| Package / component | Filter và vị trí `*-manifest.txt` trong TEMP | Giới hạn |
| --- | --- | --- |
| `com.syu.radio/com.syu.radio.Launch` | MAIN/LAUNCHER; `radio:96–104` | UI thường; `ActRadio` cũng có MAIN/DEFAULT và action `.radio/.radio.set`; không chọn thẳng activity nội bộ khi đã có launcher. |
| `com.syu.carradio/com.syu.carradio.ActivityLauncher` | MAIN/LAUNCHER/DEFAULT + `.carradio`; `carradio:35–50` | Khác gói radio, dùng khi cờ OEM đã xác định. Mẫu bị thiếu icon resource theo AAPT; không tự thay thế radio thường. |
| `com.syu.eq/com.syu.eq.activity.MainActivity` | MAIN/LAUNCHER/DEFAULT + `.eq`; `eq:40–51` | APK mẫu không vượt xác minh chữ ký; chỉ candidate component, kiểm tra runtime bắt buộc. Không chứng minh EQ/audio effect. |
| `com.syu.filemanager/com.syu.filemanager.FileManagerActivity` | MAIN/LAUNCHER, `.filemanager/.fromDvr`; `filemanager:92–107` | Chỉ mở UI. Không gọi `copyfile`, PreloadImage hoặc cơ chế cài APK. |
| `com.syu.video/com.syu.video.main.VideoListActivity` | MAIN/LAUNCHER, `.video`; `video:57–70` | UI danh sách; không tự phát file hoặc gọi PlayerService. |
| `com.syu.av/com.syu.av.ActivityLaunch` | MAIN/LAUNCHER; `av:65–73` | Mở AUX UI; nguồn video/audio thực cần vendor/phần cứng. |
| `com.syu.guide/com.syu.guide.GuideActivity` | MAIN/LAUNCHER/DEFAULT; `guide:34–45` | Hướng dẫn app gốc; không phân phối nội dung tài liệu vendor. |
| `com.syu.bt/com.syu.bt.act.ActBtAvStart` | `.btav`/DEFAULT; Manifest XML source line72–76 | Gốc mở explicit. Phải explicit-only; ưu tiên package launcher sẽ mở BtAct sai màn hình. Không fallback sang BtAct rồi báo thành công BT audio. |
| `com.syu.onekeynavi/com.syu.onekeynavi.MainActivity` | MAIN/LAUNCHER; `onekeynavi:29–37` | `MainActivity.java:13–18` notify RemoteTools module0/code28 và finish; không phải đường mở map trực tiếp. |
| `com.syu.onekeynavi/com.syu.onekeynavi.LaunchNavi` | `.onekeynavi`/DEFAULT; `onekeynavi:38–45` | `LaunchNavi.java:17–26` sendInt(0,24,0), finish. TN đã triển khai request explicit-only với action `com.syu.onekeynavi`; khác MAIN/LAUNCHER MainActivity. Chỉ request là IMPLEMENTED/STATIC_VERIFIED; backend/default-map/fallback chưa xác minh. |
| `com.syu.carlink/com.syu.carlink.MainActivity` | MAIN/LAUNCHER, exported=true; `carlink:101–110` | Có ứng viên projection UI; chưa chứng minh đúng thay thế các Easyconn/Carlife được Launcher tham chiếu. Native/USB/audio và cặp điện thoại vẫn chưa kiểm chứng. |

TN hiện có 12 manual entry requests: camera, music, Bluetooth, vendor Settings, radio, video, EQ, file manager, AUX, guide, Bluetooth audio và navigation. Phần request đã có source thật và kiểm tra runtime component; không phải bằng chứng chức năng bên trong ứng dụng vendor hoạt động. Riêng navigation dùng `Intent("com.syu.onekeynavi")` với explicit `LaunchNavi`, không fallback sang `MainActivity`. Việc app gốc tự gọi toolkit bên trong không đồng nghĩa TN bind Binder: TN chỉ mở activity theo thao tác người dùng. `NAV-UI` ghi riêng request IMPLEMENTED/STATIC_VERIFIED; `NAV-01` giữ backend/default navigation BLOCKED loại5/6. Chưa chạy app vendor thật hoặc kiểm thử xe.

Giới hạn widget TN hiện tại: `HomeActivity` mở `WidgetsActivity` qua nút Tiện ích Android; host hiển thị RemoteViews trong màn riêng và tính kích thước từ provider/diện tích thực. Chưa có kéo, thả hoặc resize widget trực tiếp trên Home. Cơ chế host chuẩn Android không tái tạo transport/metadata vendor hoặc chứng minh widget gốc hoạt động trên Wincavn.

## Kiểm chứng increment Android, tách khỏi phân tích APK gốc

Kết quả hiện có: **25 unit tests PASS, lint0errors/51warnings, 33 instrumentation và9 E2E PASS trên cả1280×720/160dpi và1920×1080/320dpi, API29, animation scale1.0**; viewport/density thực đã kiểm tra. Trạng thái cổng increment sau hygiene/handoff cuối xem [compatibility report](../testing/compatibility-report.md). Các kết quả này nâng mức phần chuẩn Android của TN, không nâng bằng chứng APK gốc hoặc chức năng vendor thật.

- App info đã mở Settings thật đúng package; uninstall request đã thử Cancel mà không gỡ fixture. E2E cả hai profile xác nhận gỡ fixture qua UI Android, giữ favorite/membership khi thiếu package rồi cài lại/cập nhật. System/self/vendor vẫn bị chặn khỏi request uninstall; TN không xóa trực tiếp.
- Folder đã kiểm tra codec/giới hạn/membership, tạo/mở ứng dụng thật/đổi tên/xóa/recreate và lưu/phục hồi dữ liệu. E2E cả hai profile kiểm tra folder/member qua process death và package changes. Folder chỉ được đặt ở Home; Dock vẫn chứa component ứng dụng, không có folder Dock.
- Widget đã dùng provider fixture thật: system bind consent cho phép/từ chối, configuration/cancel/recreate, RemoteViews update/click, đóng/mở lại, remove và provider thiếu. Bookkeeping provider thiếu vẫn được giữ. State hỏng, kể cả pending sai kiểu, fail-closed và giữ raw; không âm thầm ghi đè. Cancellation trong allocation có test deterministic để xác nhận ID mới và pending được dọn. Kiểm chứng giới hạn trong màn Tiện ích riêng như trên, không phải Home drag/resize hoặc media widget OEM.
- Wi-Fi/ngày giờ đã mở trang Android thật và trở về Home. Hardware phím T đã chuyển query sang global-search provider thật; TN không tạo kết quả tìm kiếm giả. Source còn fallback Drawer khi không có provider; không suy ra mọi firmware/provider đều tương thích.
- Live-wallpaper chooser thật đã mở/cancel mà giữ private wallpaper/cờ cũ. Chế độ wallpaper hệ thống đã kiểm tra FLAG_SHOW_WALLPAPER, root alpha, recreation và persistence. Đây là dùng wallpaper hệ thống hiện có, không clone picker/skin vendor hoặc xác minh live provider OEM.
- Đồng hồ được chờ qua phút thật khi Home vẫn foreground và đã cập nhật. TIME_CHANGED/TIMEZONE_CHANGED có receiver trong source, nhưng chưa thay timezone hoặc locale để kiểm chứng các nhánh đó.

Negative/fixture tests chứng minh resilience của adapter và lựa chọn package launcher/explicit subscreen. Cả12 manual vendor requests vẫn chỉ **IMPLEMENTED/STATIC_VERIFIED** đối với app vendor thật; `NAV-01` backend/default navigation vẫn BLOCKED. **Full reconstruction INCOMPLETE; Figma NOT_READY.**

## Hợp đồng RemoteTools đã xác định đến đâu

`CarStates.java:38–46` tạo RemoteTools và bind. `RemoteTools.java:47–57` dùng action `com.syu.ms.toolkit`, package `com.syu.ms`; `83–99` lấy module qua Binder. Mẫu ms Manifest `ms-manifest.txt:98–102` khai báo `app.ToolkitService` có filter, không application/service permission và không exported override: trên API29 có thể được suy ra public. `ToolkitService.java:12–13` trả `a.g.K2()`; `ToolkitBinder.java:22–67` dispatch module 0/main, 1/radio, 2/BT, 4/sound, 7/CAN, v.v. `MsToolkitInterface.java:46–75` dùng descriptor `com.syu.ipc.IRemoteToolkit`, transaction1 nhận module ID và trả Binder, tương ứng `IRemoteToolkit.java:38–48` phía Launcher.

Không thấy kiểm tra UID/chữ ký ở chính `onBind` hoặc stub lấy module đã đọc. Vì vậy **không được ghi mọi Binder call là signature-denied** chỉ dựa vào system UID của gói ms. Cũng chưa được khẳng định hợp đồng dùng được: quyền ở module sâu hơn, version/schema callback, firmware/native effects và caller access trên thiết bị thật chưa xác minh. Các lệnh điều khiển nguồn âm thanh, màn hình, CAN và camera không được gọi thử. TN không bind toolkit, không copy vendor interface/source/library và không dùng mã decompile trong ứng dụng.

Trạng thái hợp lý hiện tại: candidate interface loại3 nhưng chưa đủ căn cứ tương thích; gap loại6 (cần đọc sâu contract), loại5 khi phụ thuộc firmware/phần cứng, loại4 chỉ khi có bằng chứng permission/caller restriction cụ thể. Phải giữ các lý do độc lập.

## Tiêu chí hoàn tất phase phục dựng

1. Mỗi tính năng có bằng chứng đường gọi hoặc config được đưa vào ma trận với trạng thái gốc đang biết/không biết; không tự coi toàn bộ code kế thừa AOSP là UI đã bật trên Wincavn.
2. Hoàn thành và kiểm thử API29 các tính năng chuẩn Android đã scope: app info/uninstall có xác nhận, folder CRUD/persistence, widget host consent/configuration/cancel/remove/recovery, Wi-Fi/date entry và những chức năng MVP giữ lại.
3. Hoàn thành các manual activity entry có đủ bằng chứng và kiểm thử lỗi/thiếu vendor bằng fixture; chỉ đánh giá request/adapter, không đánh giá playback hoặc camera thật từ fixture.
4. Những tính năng chưa rõ contract, không có source/SDK hợp lệ, phụ thuộc signature/hardware/firmware phải có blocker chính xác và bước tiếp theo; không mô tả là implemented do có nút.
5. Broader emulator regression, diff/hygiene và handoff tri thức phải xong. Full original/automotive parity vẫn **chưa hoàn tất** khi các mục vendor trên còn mở; Figma không được dùng để che chức năng còn thiếu.

Legacy shortcuts, live wallpaper Home overview và skin plugin chưa có bằng chứng runtime bật được ghi thành gap điều tra riêng. Không biến chúng thành hardware blockers hoặc âm thầm tuyên bố đã tương đương với app search/theme TN. Global search có đường hardware key còn tồn tại như trace dưới đây.

## Quyết định phạm vi dựa trên guard thực tế

Không chỉ dựa vào tên boolean để kết luận tính năng bị tắt. Các trace bổ sung sau đây đã đọc trong đúng mẫu:

- **Live wallpaper từ Home overview bị tắt trong skin mẫu:** `Workspace.java:1604–1614` kiểm tra `!R.bool.wallpaper_show` và return sớm, trước `enableOverviewMode`. Chỉ `enableOverviewMode:1627–1640` mới đổi state OVERVIEW/hiện nút. Flag mẫu là false. `Launcher.java:2598–2603` vẫn gắn listener nút live wallpaper; `3874–3879` mở picker riêng với extra `live_wallpaper=1`. Picker có khả năng thật (`WallpaperPickerActivity.java:190–209,323–336` tách danh sách live/static), nhưng đường overview bị guard chặn. Manifest vẫn có external SET_WALLPAPER entry; trạng thái/tài nguyên overlay trong firmware thật chưa có. Không gọi live wallpaper là hardware-blocked hoặc đã được TN private wallpaper thay thế.
- **Legacy shortcut bị loại khỏi picker trong mẫu:** `AppsCustomizePagedView.java:345–384` duyệt `widgetsAndShortcuts` nhưng chỉ nhận `AppWidgetProviderInfo`; `ResolveInfo` của CREATE_SHORTCUT không được thêm. `274–281` đặt số widget pages bằng0 khi `iswidgetpages=false`. `Launcher.java:4387–4398` giữ hàm `processShortcutFromDrop`, song không tìm thấy đường chọn shortcut còn hoạt động trong picker này. Receiver legacy riêng có permission ở Manifest; TN không nhận broadcast vendor đó. Chưa có source/runtime overlay nào chứng minh các đường code cũ được bật lại.
- **Global search khác hai mục trên:** `Launcher.java:6140–6161` hardcode các nút search/voice thành GONE và trả false, nên touch entry bị ẩn. Nhưng `2318–2326` vẫn đưa ký tự gõ không rỗng đến `onSearchRequested:4343–4345`, rồi `4298–4330` mở global provider nếu SearchManager có provider. Đây là đường chuẩn Android còn hoạt động trong code, nên scope thêm public global-search capability/fallback thay vì gán hoàn toàn dormant.

Folder và generic widget host được scope như phục dựng khả năng code dù chưa chứng minh skin gốc bật. Legacy arbitrary shortcuts không phải exit criterion của batch active-entry hiện tại vì các guard trên; vẫn ghi NOT_STARTED, không xóa khỏi roadmap. Android live-wallpaper chooser và cờ hiển thị wallpaper hệ thống trong TN đã được kiểm thử ở phạm vi chuẩn Android như mục kết quả trên; không tuyên bố clone toàn bộ picker/overview của gốc. Nếu có bằng chứng runtime/overlay bật lại đường dormant, phải mở lại scope và kiểm thử trước khi tuyên bố tương đương.
