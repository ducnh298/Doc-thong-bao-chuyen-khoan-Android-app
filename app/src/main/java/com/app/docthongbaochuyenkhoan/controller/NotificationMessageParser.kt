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
            val amountRegex = """Số tiền:\s*([\d.,]+)\s*[₫]""".toRegex()

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
            return extractAmountFromTitle(title)
                ?: extractAmountFromContent(content)
        }

        private fun extractAmountFromTitle(title: String?): Long? {
            if (title == null || !title.contains("VND") && !title.contains("₫"))
                return null

            val amount =
                title.replace("[^0-9+-]".toRegex(), "").trim() // Remove non-numeric characters

            if (amount.length < 4)
                return null

            return if (amount.isEmpty()) null
            else when (amount.firstOrNull()) {
                '+' -> amount.substringAfter("+").toLong()
                '-' -> -amount.substringAfter("-").toLong()
                else -> amount.trim().toLong()
            }
        }

        private fun extractAmountFromContent(str: String?): Long? {
            if (str.isNullOrBlank() ||
                (!str.contains("SD", true) && !str.contains("số dư", true))
            ) return null

            // Find the position of "VND" or "₫" in the notification
            val vndIndex =
                str.indexOf("VND").takeIf { it != -1 } ?: str.indexOf("₫").takeIf { it != -1 }
                ?: return null

            // Get the substring from the beginning to before "VND" or "₫"
            val beforeVND = str.substring(0, vndIndex).trim()

            // Regex to find amount (allows +, -, and spaces)
            val regex = Regex("""[+-]?\s?\d{1,3}(?:,\d{3})*(?:\.\d+)?""")
            var amountStr =
                regex.findAll(beforeVND).lastOrNull()?.value?.replace("\\s".toRegex(), "")
                    ?: return null

            amountStr = amountStr.replace(".", "").replace(",", "")

            // Convert amountStr to Long
            return when (amountStr.firstOrNull()) {
                '+' -> amountStr.substringAfter("+").trim().toLong()
                '-' -> -amountStr.substringAfter("-").trim().toLong()
                else -> amountStr.trim().toLong()
            }
        }
    }
}