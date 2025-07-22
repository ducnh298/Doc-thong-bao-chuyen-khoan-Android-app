package com.app.docthongbaochuyenkhoan.controller

import android.util.Log
import com.app.docthongbaochuyenkhoan.model.Bank
import com.app.docthongbaochuyenkhoan.model.Transaction

class NotificationMessageParser {

    companion object {
        fun extractTransactionFromRawNotificationMessage(
            packageName: String,
            title: String,
            content: String
        ): Transaction? {
            val bank = Bank.fromPackageName(packageName)

            Log.d("MainActivity", "Raw notification: Bank: $bank\nTitle: $title\nContent: $content")
            val amount: Long? =
                when (bank) {
                    Bank.MOMO -> extractAmountMomo(content)
                    else -> extractAmount(title, content)
                }

            Log.d("MainActivity", "Amount: $amount")

            return if (amount != null && amount != 0L && bank != null)
                Transaction(
                    bank = bank,
                    title = title,
                    content = content,
                    amount = amount,
                )
            else null
        }

        private fun extractAmountTechComBank(title: String?): Long? {
            if (title == null)
                return null

            val amount =
                title.replace("[^0-9]".toRegex(), "").trim() // Loại bỏ ký tự không phải số

            return if (amount.startsWith("+")) amount.toLong() else -amount.toLong()
        }

        private fun extractAmountMBBank(content: String?): String? {
            if (content == null)
                return null

            val amount = content.substringAfter("GD: ").substringBefore("VND").trim()

            return amount
        }

        private fun extractAmountVietComBank(content: String?): String? {
            if (content == null)
                return null

            val amount: String = if (content.contains("+"))
                "+" + content.substringAfter("+").substringBefore(" VND").trim()
            else "-" + content.substringAfter("-").substringBefore(" VND").trim()

            return amount
        }

        private fun extractAmountVPBank(title: String?): String? {
            if (title == null)
                return null

            val amount =
                title.replace("[^0-9]".toRegex(), "") // Loại bỏ ký tự không phải số và dấu phẩy

            return "${title.first()} $amount"
        }

        private fun extractAmountMomo(content: String?): Long? {
            if (content == null)
                return null

            // Tìm cụm "Số tiền:" trong nội dung
            val amountRegex = """Số tiền:\s*([\d.,]+)\s*₫""".toRegex()

            // Kiểm tra và lấy số tiền
            val matchResult = amountRegex.find(content)
            var amount = matchResult?.groupValues?.get(1)

            if (amount != null) {
                amount = amount.replace(",", "").replace(".", "")

                // Nếu có dấu "+" hoặc "-"
                if (amount.startsWith("-")) {
                    amount.replace("-", "").trim()
                    return -amount.toLong()
                } else {
                    amount.replace("+", "").trim()
                    return amount.toLong()
                }
            } else return null
        }

        private fun extractAmount(title: String?, content: String?): Long? {
            return extractAmountFromTitle(title, content)
                ?: extractAmountFromContent(content)
        }

        private fun extractAmountFromTitle(title: String?, content: String?): Long? {
            if (title.isNullOrBlank())
                return null

            // Bước 1: Kiểm tra các dấu hiệu của đơn vị tiền tệ trong tiêu đề
            val hasCurrencyUnit = title.contains("VND", ignoreCase = true) ||
                    title.contains("₫") ||
                    title.contains("đ")

            if (!hasCurrencyUnit)
                return null

            // Bước 2: Xác định loại giao dịch (cộng hay trừ) dựa trên các từ khóa phổ biến trong tiêu đề.
            // Ưu tiên các từ khóa chỉ rõ dấu hiệu tiền ra/tiền vào.
            val isCredit = title.contains("+", ignoreCase = true) ||
                    title.contains(  "nhận", ignoreCase = true) ||
                    title.contains("cộng", ignoreCase = true) ||
                    title.contains("được cộng", ignoreCase = true) ||
                    title.contains("tăng", ignoreCase = true) ||
                    title.contains("nạp", ignoreCase = true)

            val isDebit = title.contains("-", ignoreCase = true) ||
                    title.contains("trừ", ignoreCase = true) ||
                    title.contains("chuyển", ignoreCase = true) ||
                    title.contains("thanh toán", ignoreCase = true) ||
                    title.contains("giảm", ignoreCase = true) ||
                    title.contains("chi", ignoreCase = true) ||
                    title.contains("giao dịch", ignoreCase = true) // 'giao dịch' có thể là cả 2, nhưng thường là tiền ra nếu không có 'nhận/cộng'

            // Bước 3: Trích xuất số tiền.
            // Regex tìm số có thể có dấu +/- ở đầu, dấu phân cách hàng nghìn (chấm/phẩy) và phần thập phân.
            // Ưu tiên tìm số lớn nhất hoặc số nằm gần các từ khóa/đơn vị tiền tệ.
            // Mẫu regex: [+-]?\s*\d{1,3}(?:[.,]\d{3})*(?:[.,]\d+)?
            // `[+-]?`: Tùy chọn dấu cộng hoặc trừ.
            // `\s*`: Tùy chọn khoảng trắng.
            // `\d{1,3}`: 1 đến 3 chữ số đầu tiên.
            // `(?:[.,]\d{3})*`: Các nhóm .XXX hoặc ,XXX lặp lại.
            // `(?:[.,]\d+)?`: Tùy chọn phần thập phân (vd: .50).
            val amountRegex = Regex("""([+-]?)\s*(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d+)?)""")

            val allMatches = amountRegex.findAll(title).toList()

            // Lấy số tiền lớn nhất hoặc số cuối cùng trong tiêu đề có vẻ là số tiền
            // Thường số tiền sẽ là số lớn nhất hoặc là số cuối cùng xuất hiện trong chuỗi
            val bestMatch = allMatches
                .maxByOrNull { it.groupValues[2].replace("[.,]".toRegex(), "").toLongOrNull() ?: 0L }
                ?: allMatches.lastOrNull() // Nếu không tìm thấy max, lấy match cuối cùng

            val amountStrWithSeparators = bestMatch?.groupValues?.get(2) ?: run {
                return null
            }
            var cleanAmountStr =
                amountStrWithSeparators.replace("[.,]".toRegex(), "") // Loại bỏ dấu phân cách

            val signFromRegex = bestMatch.groupValues[1] // Dấu lấy được từ regex (+/-)

            val parsedAmount = cleanAmountStr.toLongOrNull() ?: run {
                return null
            }

            // Bước 4: Áp dụng dấu dựa trên loại giao dịch và dấu tìm được trong chuỗi
            // Ưu tiên dấu rõ ràng trong tiêu đề (nếu có)
            return when {
                signFromRegex == "-" -> -parsedAmount
                signFromRegex == "+" -> parsedAmount
                isDebit && !isCredit -> -parsedAmount // Nếu là giao dịch trừ và không phải là cộng
                isCredit && !isDebit -> parsedAmount  // Nếu là giao dịch cộng và không phải là trừ
                // Trường hợp không rõ ràng hoặc có cả hai dấu hiệu, mặc định là dương hoặc cần thêm logic
                else -> parsedAmount
            }
        }

        private fun extractAmountFromContent(str: String?): Long? {
            if (str.isNullOrBlank()) {
                return null
            }

            // Bước 1: Kiểm tra xem chuỗi có phải là một tin nhắn giao dịch hay không.
            // Thêm các từ khóa chỉ báo giao dịch.
            val isTransactionMessage = str.contains("SD", ignoreCase = true) ||
                    str.contains("số dư", ignoreCase = true) ||
                    str.contains("nhận", ignoreCase = true) ||
                    str.contains("chuyển", ignoreCase = true) ||
                    str.contains("giao dịch", ignoreCase = true) ||
                    str.contains(
                        "biến động",
                        ignoreCase = true
                    ) // Thêm các từ khóa liên quan đến giao dịch

            if (!isTransactionMessage) {
                // Nếu không có các từ khóa chỉ báo giao dịch, không phải là tin nhắn giao dịch
                return null
            }

            // Bước 2: Tìm vị trí của đơn vị tiền tệ ("VND", "₫", "đ")
            val vndRegex =
                Regex("""(VND|₫|đ)""", RegexOption.IGNORE_CASE) // Regex để tìm VND hoặc ₫/đ
            val matchResult =
                vndRegex.find(str) ?: return null // Tìm vị trí đầu tiên của đơn vị tiền tệ

            val vndIndex = matchResult.range.first // Lấy vị trí bắt đầu của đơn vị tiền tệ

            // Lấy một đoạn substring ngắn trước đơn vị tiền tệ để tìm số tiền
            val startIndex =
                (vndIndex - 30).coerceAtLeast(0) // Lấy 30 ký tự trước VND hoặc từ đầu chuỗi
            val searchArea = str.substring(startIndex, vndIndex)

            // Bước 3: Regex mạnh mẽ hơn để tìm số tiền trong khu vực tìm kiếm.
            // Cải tiến regex để ưu tiên các số lớn và có dấu +/-
            // - `[+-]?`: Có hoặc không có dấu cộng hoặc trừ ở đầu.
            // - `\s*`: Có thể có khoảng trắng sau dấu.
            // - `\d{1,3}`: Bắt đầu với 1-3 chữ số.
            // - `(?:[.,]\d{3})*`: Theo sau bởi nhóm (dấu phẩy/chấm và 3 chữ số) lặp lại 0 hoặc nhiều lần (ví dụ: 1.000.000).
            // - `(?:\.\d+)?`: Tùy chọn phần thập phân (dấu chấm và ít nhất 1 chữ số).
            // - `(?<!\d)`: Lookbehind âm, đảm bảo không có chữ số ngay trước đó (để không bắt số điện thoại, số seri,... mà không có đơn vị tiền tệ)
            val amountRegex = Regex("""([+-]?)\s*(\d{1,3}(?:[.,]\d{3})*(?:\.\d+)?)""")

            // Tìm tất cả các khớp trong khu vực đã xác định
            val allAmountsInArea = amountRegex.findAll(searchArea).toList()

            // Chọn số tiền cuối cùng (gần đơn vị tiền tệ nhất)
            val lastAmountMatch = allAmountsInArea.lastOrNull() ?: return null

            var amountStr =
                lastAmountMatch.groupValues[2] // Lấy phần số tiền (không bao gồm dấu +/-)
            val signStr = lastAmountMatch.groupValues[1] // Lấy dấu (+/-)

            // Loại bỏ dấu phân cách hàng nghìn (dấu chấm hoặc phẩy)
            amountStr = amountStr.replace(".", "").replace(",", "")

            // Chuyển đổi sang Long, áp dụng dấu
            val parsedAmount = amountStr.toLongOrNull() ?: return null

            return when (signStr) {
                "-" -> -parsedAmount
                "+" -> parsedAmount // Dấu + không cần thiết cho giá trị dương
                else -> parsedAmount // Không có dấu, coi là dương
            }
        }
    }
}