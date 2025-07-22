package com.app.docthongbaochuyenkhoan.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DateUtils {
    companion object{
        private val sdfDateTime = SimpleDateFormat(" HH:mm:ss - dd/MM/yyyy")
        private val sdfDate = SimpleDateFormat("dd/MM/yyyy")
        private val sdfMonth = SimpleDateFormat("MM/yyyy")

        fun formatDateTime(timestamp: Long): String {
            return sdfDateTime.format(java.util.Date(timestamp))
        }

        fun formatDate(timestamp: Long): String {
            return sdfDate.format(java.util.Date(timestamp))
        }

        fun formatMonth(timestamp: Long): String {
            return sdfMonth.format(java.util.Date(timestamp))
        }

        fun getStartTimeOfToday(): Long {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            // Get the first time today
            return calendar.timeInMillis
        }

        fun getDayOfWeek(dateString: String): String {
            val date = sdfDate.parse(dateString) ?: return ""

            val calendar = Calendar.getInstance()
            calendar.time = date

            val daysOfWeek = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            return daysOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        }

        fun getMonthOfYear(dateMonthString: String): String {
            val date = sdfMonth.parse(dateMonthString) ?: return ""
            val sdfOutput = SimpleDateFormat("MMM", Locale.ENGLISH)  // "MMM" -> Jan, Feb, Mar, ...
            return sdfOutput.format(date) // Ví dụ: "Jun"
        }

        fun getDaysBetweenDates(startDate: Long, endDate: Long): Long {
            // 1. Tính khoảng thời gian chênh lệch (đơn vị: mili giây)
            val difference = endDate - startDate

            // 2. Định nghĩa số mili giây trong một ngày
            val millisecondsPerDay = 1000 * 60 * 60 * 24

            // 3. Chuyển đổi mili giây sang ngày và trả về
            return difference / millisecondsPerDay
        }
    }
}