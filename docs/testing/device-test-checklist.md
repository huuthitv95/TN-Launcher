# Checklist kiểm thử Wincavn sau cổng emulator

Ngày lập: 15/10/2026. **Tất cả ca HW: NOT_RUN.** PRE-01 đã đạt trên emulator; các điều kiện/ca trên thiết bị thật còn lại chưa thực hiện. Chưa có DEVICE_VERIFIED hoặc AUTOMOTIVE_VERIFIED. Tài liệu là bộ ca cho một phiên được cho phép riêng; không phải đề nghị cài hoặc đổi Home ngay lúc này.

**Đường rollback trên Wincavn chưa được xác minh, đang chặn mọi bước cài/chọn TN Launcher làm Home.** Xem [rollback guide](../release/rollback-guide.md). Cổng emulator là điều kiện bổ sung, không tự mở cổng thiết bị.

## Điều kiện trước phiên thử

| ID | Điều kiện cần có bằng chứng trước khi tiếp tục | Trạng thái |
| --- | --- | --- |
| PRE-01 | `EMULATOR_GATE_PASSED` được ghi trong compatibility report cho đúng APK, phiên bản và SHA-256; không còn lỗi chặn chức năng MVP; giới hạn frame/night mode được ghi rõ | PASS — chỉ emulator |
| PRE-02 | Người dùng cho phép rõ từng hành động: chẩn đoán, cài APK, chọn Home và các ca camera/phần cứng. Chỉ thực hiện những mục đã được cho phép | NOT_RUN |
| PRE-03 | Có người trực tiếp giám sát tại xe; xe đứng yên trong điều kiện thử được người tại xe xác nhận an toàn. Không điều khiển xe từ xa | NOT_RUN |
| PRE-04 | Xác minh cách vào Settings/chọn Home và mở Launcher gốc qua lối độc lập với TN Launcher; ghi đường điều hướng, component thực tế và cách phục hồi khi TN Launcher không mở được | NOT_RUN |
| PRE-05 | Xác minh AirDroid thực sự có thể xem/điều khiển/truyền file ở mức nào. Chỉ dùng ADB thiết bị thật nếu có kết nối được ủy quyền riêng; không suy ra ADB/root từ AirDroid | NOT_RUN |
| PRE-06 | Ghi baseline Launcher mặc định, camera/cảnh báo hiện có, Android/firmware, độ phân giải/mật độ thực nếu đọc được; giữ nguyên app gốc và dữ liệu/cấu hình | NOT_RUN |

PRE-04 phải hoàn tất trước khi cài/chọn Home. Đổi Home là hành động riêng cần cho phép, dù việc cài APK đã được cho phép. Không disable, gỡ, ghi đè Launcher/camera/service gốc hoặc cấp quyền nâng cao để vượt một lỗi tích hợp.

## Ca phần cứng và tiêu chí đạt

Thực hiện baseline bằng hệ thống gốc trước, rồi mới so với TN Launcher trong phạm vi đã được cho phép. Nếu kết quả baseline không rõ, giữ ca chưa đạt và không suy đoán parity.

| ID / Liên kết parity | Thao tác thuộc phiên thử được cho phép | Bằng chứng và tiêu chí đạt | Trạng thái |
| --- | --- | --- | --- |
| HW-01 / V-01 | Mở trạng thái tích hợp TN Launcher trên thiết bị | Package/component thực, enabled/exported/quyền và lỗi được phản ánh đúng; không coi AVAILABLE là chức năng camera đã đạt | NOT_RUN |
| HW-02 / CAM-01 | Người tại xe mở shortcut 360 thủ công | App camera gốc mở, hình camera thật hiển thị đúng; không crash hoặc điều khiển TN đè lên. Ghi riêng package/component và khác biệt baseline | NOT_RUN |
| HW-03 / CAM-02 | Người trực tiếp tại xe thực hiện trigger lùi theo quy trình an toàn được cho phép, từ Home TN | Hệ thống tự hiển thị camera như baseline, không cần bấm shortcut; ghi độ trễ quan sát được, hình/cảnh báo và mọi khác biệt. Không dùng broadcast/CAN giả để thay ca này | NOT_RUN |
| HW-04 / CAM-03 | Trong một trigger OEM được cho phép, quan sát khi Home, Drawer, Settings/dialog/bàn phím hoặc app khác đang ở foreground | Camera/cảnh báo OEM hiển thị đầy đủ và nhận thao tác như baseline; TN không giành lại foreground. Mỗi trạng thái nền được ghi riêng | NOT_RUN |
| HW-05 / CAM-03 | Sau khi kết thúc camera/trigger theo quy trình đã cho phép, quan sát rồi nhấn Home/Back | Thoát camera và trở về màn trước/Home theo baseline, không vòng lặp mở Home, không mất cảnh báo đang còn hiệu lực | NOT_RUN |
| HW-06 / V-02–04 | Mở riêng Nhạc, Bluetooth và Settings Wincavn qua shortcut, rồi quay lại | Mở đúng UI gốc; Home sống ổn định, không lỗi quyền/force close. Không đổi cấu hình xe, ghép nối/gọi điện hoặc điều khiển media ngoài phạm vi cho phép | NOT_RUN |
| HW-07 / CORE-01, UI-01 | Kiểm tra Home/Back/Recents và UI ở màn hình thực, theme sáng/tối, chữ lớn | Chạm/nhãn đủ đọc ở khoảng cách sử dụng, không bị cắt bởi insets, hiệu năng/khôi phục Home phù hợp; ghi số đo thực thay vì dùng profile emulator làm thông số xe | NOT_RUN |
| HW-08 / CORE-05 | Khởi chạy lại TN và kiểm tra bố cục/Dock/theme sau các bước trong phiên | Cấu hình đã lưu được phục hồi; package tạm thiếu không mất favorite. Chu kỳ nguồn/ACC chỉ thử nếu được cho phép cụ thể, không tự thực hiện | NOT_RUN |
| HW-09 / rollback | Theo lối độc lập đã xác minh, chọn lại Home gốc khi được cho phép | Launcher gốc trở lại, camera/cảnh báo baseline vẫn hoạt động; lưu đường phục hồi đã thực tế dùng | NOT_RUN |

HW-02, HW-03, HW-04 và HW-05 là các ca độc lập. Mở được camera thủ công không chứng minh tự chuyển khi lùi hoặc ưu tiên overlay. Không tạo cảnh báo nguy hiểm chỉ để có dữ liệu thử.

## Dừng thử và ghi kết quả

Nếu camera/cảnh báo bị che, ảnh đen, mất thao tác hoặc Home giành foreground: dừng ca đang thử, người tại xe dùng lối rollback đã xác minh để khôi phục hệ thống gốc. Không tiếp tục ca camera khi chưa khôi phục baseline.

Sau phiên, ghi phiên bản/hash APK, điều kiện, ca đã được cho phép, PASS/FAIL/NOT_RUN và bằng chứng quan sát đã khử dữ liệu riêng. Cập nhật kết quả vào [feature parity matrix](../research/feature-parity-matrix.md); chỉ nâng từng mục lên mức kiểm chứng thực sự đạt. Ca không thực hiện vẫn giữ NOT_RUN; cổng emulator và kết quả xe là hai bằng chứng riêng.
