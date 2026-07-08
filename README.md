# Đọc Thông Báo Chuyển Khoản

> ⭐️ Hoàn toàn miễn phí — Không thu thập dữ liệu cá nhân

Ứng dụng tự động lắng nghe thông báo từ app ngân hàng và đọc to số tiền giao dịch bằng giọng Tiếng Việt, giúp bạn kiểm soát dòng tiền ngay lập tức mà không cần nhìn vào điện thoại.

[![Google Play](https://img.shields.io/badge/Google_Play-Download-green)](https://play.google.com/store/apps/details?id=com.app.docthongbaochuyenkhoan)

---

## Tính năng

- **Đọc tự động** — Đọc to số tiền khi nhận/chuyển khoản từ mọi ngân hàng hỗ trợ
- **Thống kê** — Theo dõi và thống kê tổng thu/chi theo ngày, tháng với biểu đồ
- **Sao lưu & Khôi phục** — Xuất/nhập toàn bộ lịch sử giao dịch ra file JSON nén
- **Cá nhân hóa** — Tùy chỉnh nội dung đọc, âm thanh thông báo, chế độ tối
- **Cập nhật tự động** — Thông báo và cài đặt phiên bản mới ngay trong app

## Ngân hàng hỗ trợ

Techcombank, MBBank, Vietcombank, Agribank, VPBank, TPBank, Sacombank, BIDV, Vietinbank, ACB, HDBank, OCB, MSB, SeABank, LienVietPostBank, Shinhan Bank, Bac A Bank, ABBank, PVcomBank, Eximbank, Nam A Bank, KienlongBank, BaoViet Bank, SaigonBank, Co-opBank, HSBC, Momo, Viettel Money, ZaloPay

## Yêu cầu

- Android 7.0 (API 24) trở lên
- Cấp quyền **Truy cập thông báo** cho app

## Build

```bash
# Debug
./gradlew assembleDebug

# Release
./gradlew assembleRelease
```

## Test thông báo qua ADB

```bash
# Tiền đến
adb shell 'cmd notification post -S bigtext -t "MB Bank +500,000 VND" mb_in "TK:1234567890 +500,000VND SD:2,000,000VND"'

# Tiền đi
adb shell 'cmd notification post -S bigtext -t "MB Bank -200,000 VND" mb_out "TK:1234567890 -200,000VND SD:1,800,000VND"'
```

Xem thêm lệnh test tại [`test_command.txt`](test_command.txt).
