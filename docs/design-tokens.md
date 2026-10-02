# Design tokens

Cấu trúc: Global → Semantic → Component. Quy tắc đặt tên: `loại-thuộc tính-biến thể-trạng thái`.

## Global
| Token | Sáng | Tối |
|---|---|---|
| `bg-wallpaper-base` | #CFE3F7 | #0B1530 |
| `glass-fill` | #FFFFFF @ 45% | #1A2748 @ 55% |
| `glass-stroke` | #FFFFFF @ 70% | #FFFFFF @ 14% |
| `text-primary` | #0F1B2D | #F2F6FF |
| `text-secondary` | #0F1B2D @ 60% | #F2F6FF @ 62% |
| `accent` | #0A84FF | #3B82F6 |
| `success` | #34C759 | #30D158 |

## Hình khối và khoảng cách
- Bo góc: thẻ 28dp, nút 20dp, biểu tượng 22% cạnh (squircle), dock 32dp.
- Khoảng cách: 8 / 12 / 16 / 24dp.
- Mục chạm tối thiểu: 56dp (màn hình xe, thao tác khi lái).

## Chữ
- Font: Inter (hoặc Roboto nếu muốn nhẹ); cỡ: 14 / 16 / 20 / 28 / 56 (đồng hồ).
- Tối thiểu 16sp cho văn bản chính vì khoảng nhìn xa.

## Quy tắc kính
- Chỉ sidebar và dock là kính; thẻ nội dung dùng nền mờ đặc hơn.
- Không chồng kính lên kính.
- Tương phản chữ/nền ≥ 4.5:1 ở cả hai giao diện.
