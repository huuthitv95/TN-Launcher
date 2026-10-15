# Nguồn icon và ánh xạ

Ánh xạ được giới hạn theo package thật. Khi không có ánh xạ đã xác minh, dùng `ActivityInfo.loadIcon` của ứng dụng cài đặt. Không đoán icon theo tên hiển thị.

| Mục | Nguồn | Ánh xạ / xử lý |
| --- | --- | --- |
| TN Launcher | `Resource_For_TN_Launcher/TN_Launcher.png` do người dùng cung cấp | `drawable-nodpi/tn_brand.png`; giữ ảnh nguyên bản, fitCenter |
| Android Settings | MacTahoe `src/apps/scalable/preferences-system.svg` | Ánh xạ TN lựa chọn riêng cho `com.android.settings`; không áp dụng `com.syu.settings` hoặc app tên tương tự |
| Các ứng dụng khác | PackageManager trên thiết bị | Icon native của từng launcher activity |

## Bằng chứng MacTahoe Settings

- Đã xem bản render: tile xám bo góc có bánh răng; phù hợp ý nghĩa Settings chung. Đây là ánh xạ do TN lựa chọn, không phải tuyên bố upstream hỗ trợ package Android.
- Nguồn thực: `C:/Users/ZGAMESVN/Downloads/Resource_For_TN_Launcher/MacTahoe-icon-theme-main/src/apps/scalable/preferences-system.svg`, kích thước SVG 64×64, 59,975 byte.
- SHA-256 SVG: `8FF99C6C70C0A33282AA351E3DE4F0FC29BB4437D3CFE6C6AE6CFDA4A7B97632`.
- Alias `links/apps/scalable/org.gnome.Settings.svg` trong bản giải nén là file 22 byte chứa `preferences-system.svg`; không phải SVG để đưa trực tiếp vào renderer.
- `COPYING` chứa GNU GPL v3. `AUTHORS` ghi Vince Liuice, nguồn WhiteSur và bản tùy chỉnh MacTahoe tại `https://github.com/vinceliuice/MacTahoe-icon-theme`.
- Giữ nguyên SVG, COPYING và AUTHORS trong `app/src/main/assets/licenses/mactahoe/`; PNG 256×256 trong `drawable-nodpi/mactahoe_settings.png` được render bằng libvips 8.18.6: `vips svgload preferences-system.svg mactahoe_settings.png --scale 4`. Không chỉnh nội dung SVG; không đưa toàn bộ theme vào repo.
- TN brand nguồn và bản đóng gói có cùng SHA-256: `C44B6095537EA5DE410366B7E47EE8ED8C0AD1DE131304756CC07727ADD1BB8E`.

Chưa có ánh xạ MacTahoe cho camera, Bluetooth, media hoặc các gói xe. Không nhập asset Apple UI Kit. Người dùng đã yêu cầu commit/push source lên main; tài nguyên được rà cho commit gồm TN brand và đúng một mapping MacTahoe cùng SVG/COPYING/AUTHORS nêu trên. Không đưa toàn bộ theme, archive vendor hoặc APK/build output vào publication. SHA commit và kết quả push đối chiếu Git; đây không phải xác nhận phát hành APK hoặc kiểm chứng trên xe.
