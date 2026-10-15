# Hệ thống thiết kế TN Launcher MVP

Giao diện native Android Views, landscape-first và thích ứng với diện tích khả dụng. Tham chiếu Figma đã được kiểm tra ở bước lập kế hoạch: Home `144:62674`, Dock `558:50382` trong file `z9h85ehDzhCsLpV5n7M0Kb`. Đây là cảm hứng bố cục; không tuyên bố pixel-perfect hoặc sao chép hành vi iPadOS. Các token bên dưới là quyết định Android thực dụng trong source, không gắn nhãn là giá trị trích nguyên từ Figma.

## Mức hoàn thiện hiện tại

Phần chức năng MVP đã được triển khai: Home phân trang, Dock, Drawer/tìm kiếm, Settings, theme, hình nền và lưu bố cục. UI hiện dùng lưới ứng dụng native, nền gradient, Dock có icon favorite cùng nút chữ Ứng dụng/Cài đặt và Settings dạng danh sách cuộn cơ bản. Chưa hoàn tất lượt đối chiếu và tinh chỉnh độ trung thực iPadOS về khoảng cách, phân cấp thị giác, cách trình bày Dock và sự đồng nhất của icon. Có chức năng không đồng nghĩa đã hoàn tất phần hình ảnh hoặc cổng kiểm thử; xem [compatibility report](../testing/compatibility-report.md) để biết kết quả thực tế.

## Token và bố cục

| Thuộc tính | Giá trị triển khai |
| --- | --- |
| Font | SF Pro Text 400 cho nội dung; SF Pro Display 600 cho tiêu đề/đồng hồ; MRCH NewYork 400 chỉ cho nhãn Home cố định; fallback Android sans |
| Icon | 64dp, fitCenter; nhãn 16sp, tối đa 2 dòng |
| Home cell | Cơ sở 96 × 112dp; giảm số cột/hàng theo diện tích và font scale |
| Vùng chạm điều hướng | Tối thiểu 56dp; menu cung cấp thao tác di chuyển thay thế kéo |
| Lề nội dung | 24dp ngang; tôn trọng system bar/IME insets |
| Dock | Bo 30dp; favorite + Apps/Settings; cuộn ngang khi thiếu chiều rộng |
| Search | Cao 56dp, chữ 18sp, bo 16dp |
| Nội dung sáng | `#142033` trên surface `#EEE9EFF8` (alpha EE) |
| Nội dung tối | `#F5F7FA` trên surface `#EE233047` (alpha EE) |
| Nền sáng | Gradient `#F1F5FA` → `#B8CBDF` |
| Nền tối | Gradient `#111A2B` → `#30455D` |
| Accent | `#0A64D9` |

Hình nền tự chọn được phủ nhẹ theo theme để giữ chữ đọc được; không blur thời gian thực hoặc RenderEffect API 31. Giảm chuyển động tắt item animations. Home có phân trang; Drawer dùng danh sách ứng dụng/tìm kiếm. Settings là màn cuộn; bố cục không giả lập widget hoặc dữ liệu xe.

Nhấn giữ icon rồi thả tại chỗ mở menu ghim/bỏ ghim hoặc di chuyển; giữ rồi kéo đổi thứ tự Home/Dock. Hai theme dialog sáng/tối đã override chiều cao tối thiểu hàng menu thành 56dp; instrumentation có kiểm tra kích thước hàng menu. Không suy diễn từ một kiểm tra này rằng toàn bộ UI đã vượt cổng emulator.

## Typography đã tích hợp trong source

Đã đọc thư mục `C:/Users/ZGAMESVN/Downloads/Resource_For_TN_Launcher/fonts` và kiểm tra kỹ thuật bằng fontTools 4.63.0. Chọn ba biến thể static; giữ nguyên byte font nguồn, không cài font toàn hệ thống hoặc đóng gói cả thư mục. Trạng thái giấy phép của ba nguồn là **USER_CONFIRMED**, theo AGENTS.md §22; không yêu cầu hoặc thực hiện kiểm tra giấy phép bổ sung. Theo yêu cầu hiện tại “Commit và push lên nhánh main”, ba binary tích hợp dưới đây đã được rà đúng file, kích thước và SHA-256 để đưa cùng source. Thư mục font nguồn vẫn giữ nguyên ngoài repo. SHA commit và kết quả push đối chiếu Git.

Đường dẫn nguồn trong bảng tương đối với thư mục fonts trên. Bản tích hợp nằm trong `app/src/main/res/font/` và có cùng SHA-256 với nguồn.

| Vai trò | File nguồn | Resource | Family / style / weight | Định dạng | Byte |
| --- | --- | --- | --- | --- | ---: |
| Nội dung | `San Francisco Pro/OTF/SFProText-Regular.otf` | `tn_sf_body.otf` | SF Pro Text / Regular / 400 | OpenType CFF, static | 2.585.096 |
| Tiêu đề, đồng hồ | `SF-Pro-Display/SF-Pro-Display-Semibold.otf` | `tn_sf_heading.otf` | SF Pro Display / Semibold / 600 | OpenType CFF, static | 2.325.324 |
| Brand cố định | `MRCH-NewYork.ttf` | `tn_new_york.ttf` | MRCH NewYork / Regular / 400 | TrueType, static | 149.812 |

SHA-256 nguyên bản và bản tích hợp:

- `tn_sf_body.otf`: `C58083B48AC4705F55530C3A1B4C407661267703E30BF60151A8410F583EB095`.
- `tn_sf_heading.otf`: `6C4FA1BE203EC1B7A5135CF636D87E1E23A91A9AABD556309664251745FD8249`.
- `tn_new_york.ttf`: `6ED7AD6FE8F1810E807D26FBAB502FD9C8624ADD55F1DD2D73C7DB91DC12AEF4`.

| Vùng UI | Font và cỡ chữ cơ sở |
| --- | --- |
| Nhãn Home, Dock, Drawer | SF Pro Text 400, 16sp, tối đa hai dòng |
| Search | SF Pro Text 400, 18sp |
| Đồng hồ | SF Pro Display 600, 24sp |
| Tiêu đề Drawer; tiêu đề nhóm/trang Settings | SF Pro Display 600, 22sp / 28sp |
| Nội dung, nút, radio và switch Settings | SF Pro Text 400, 18sp; ghi công 16sp |
| Nhãn Home cố định “TN Launcher” | MRCH NewYork 400, 22sp; không áp dụng cho thông tin cần đọc nhanh hoặc nội dung động |
| Dialog và toast native | Font hệ thống Android |
| Tiện ích Android ở màn riêng | Host dùng SF Pro Display 600 cho tiêu đề trang 28sp/nhãn provider 20sp; SF Pro Text 400 cho nội dung, nút và danh sách provider 18sp. RemoteViews giữ typography/màu của provider; chưa kéo/resize widget trong Home |

Cỡ chữ dùng sp và tôn trọng font scale; Home điều chỉnh số hàng/cột, Dock tăng chiều cao và kích thước nút theo font scale. `LauncherTypography` bắt lỗi tải font và dùng Android sans khi tài nguyên thiếu/không tải được. Font brand có kiểm tra glyph của nhãn Latin cố định, nếu thiếu thì dùng font nội dung.

Kiểm tra cmap tĩnh cho cả ba file đạt 196 ký tự tiếng Việt dựng sẵn, chữ hoa/thường Latin và số. SF Pro Text và SF Pro Display phủ đủ các chuỗi “Tiếng Việt; Điều hướng; Cài đặt; Âm lượng; Kết nối; Bản đồ; Đường” ở cả dạng NFC và NFD. MRCH NewYork phủ dạng NFC nhưng thiếu các dấu tổ hợp `U+0300`, `U+0301`, `U+0302`, `U+0306`, `U+0309`, `U+031B`, `U+0323` cần cho những chuỗi NFD; vì vậy chỉ dùng cho “TN Launcher” cố định.

**Typography: EMULATOR_VERIFIED trên API 29 ở cả hai profile landscape.** Typeface tải được, fallback resource lỗi hoạt động, glyph các chuỗi Việt NFC/số/hoa thường đạt; Home/Dock/Drawer/Search/Settings, theme LIGHT/DARK và font scale1.5 qua instrumentation/ảnh thực. SYSTEM kiểm tra theo OS sáng thực; image khóa global night mode nên không tuyên bố đã đổi toàn OS sang tối. Kết quả và giới hạn phép đo tại [compatibility report](../testing/compatibility-report.md). Khả năng đọc ngoài trời/trên xe vẫn chưa đo.

## Tài nguyên và quyền sử dụng

- Không lấy font, wallpaper, SVG, screenshot hoặc icon từ Apple UI Kit để đóng gói. Ba font người dùng cung cấp cục bộ ở trên được sử dụng theo xác nhận riêng trong AGENTS.md §22.
- TN brand dùng `TN_Launcher.png` do người dùng cung cấp, nguyên bản bảo toàn; resource `tn_brand.png` có cùng SHA-256 và giữ tỉ lệ.
- Chỉ một icon MacTahoe được chọn cho ứng dụng Settings Android; SVG nguồn, GPLv3 và AUTHORS đi kèm trong assets. Chi tiết tại [icon mapping](icon-mapping.md). Không suy diễn mọi icon trong theme đều có nguồn/quyền tương đương.
- Ứng dụng còn lại dùng icon cài trên máy; trạng thái package tạm thiếu được biểu diễn như không khả dụng, không dùng emoji hay icon ứng dụng không liên quan.

Hai profile emulator đại diện là 1280×720/160dpi và 1920×1080/320dpi. Chúng không phải số đo màn hình xe. Kiểm tra tương phản ánh sáng thực, khoảng cách nhìn và thao tác trên Wincavn vẫn cần kiểm chứng thiết bị sau cổng emulator.
