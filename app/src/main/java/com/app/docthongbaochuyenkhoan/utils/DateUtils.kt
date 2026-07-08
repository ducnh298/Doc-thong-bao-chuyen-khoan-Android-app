package com.app.docthongbaochuyenkhoan.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import java.util.Locale

class DateUtils {
    companion object{
        private val sdfDateTime = SimpleDateFormat(" HH:mm:ss - dd/MM/yyyy")
        private val sdfDate = SimpleDateFormat("dd/MM/yyyy")
        private val sdfMonth = SimpleDateFormat("MM/yyyy")
        private val sdfRawDate = SimpleDateFormat("ddMMyyyy")
        private val sdfFileDate = SimpleDateFormat("dd-MM-yyyy_HH-mm", Locale.getDefault())

        fun formatDateTime(timestamp: Long): String {
            return sdfDateTime.format(java.util.Date(timestamp))
        }

        fun formatDate(timestamp: Long): String {
            return sdfDate.format(java.util.Date(timestamp))
        }

        fun formatMonth(timestamp: Long): String {
            return sdfMonth.format(java.util.Date(timestamp))
        }

        fun formatRawDate(timestamp: Long): String {
            return sdfRawDate.format(java.util.Date(timestamp))
        }

        fun formatFileDate(timestamp: Long): String {
            return sdfFileDate.format(java.util.Date(timestamp))
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

        @RequiresApi(Build.VERSION_CODES.O)
        fun getMonthsBetweenDates(startDate: Long, endDate: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
            // 1. Chuyển đổi Long (mili giây) thành Instant
            val startInstant = Instant.ofEpochMilli(startDate)
            val endInstant = Instant.ofEpochMilli(endDate)

            // 2. Chuyển đổi Instant thành LocalDate bằng múi giờ cụ thể.
            // Việc này là cần thiết vì "tháng" là một khái niệm trong lịch, không phải là khoảng thời gian thuần túy.
            val startLocalDate = startInstant.atZone(zoneId).toLocalDate()
            val endLocalDate = endInstant.atZone(zoneId).toLocalDate()

            // 3. Sử dụng ChronoUnit.MONTHS để tính số tháng
            // ChronoUnit.MONTHS.between() tính số tháng đầy đủ đã trôi qua.
            return ChronoUnit.MONTHS.between(startLocalDate, endLocalDate)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun getFirstDayOfMonth(startDateMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
            // 1. Chuyển đổi Long (mili giây) thành Instant
            val instant = Instant.ofEpochMilli(startDateMillis)

            // 2. Chuyển đổi Instant thành LocalDate bằng múi giờ cụ thể.
            // Việc này là cần thiết để xác định ngày, tháng, năm theo lịch.
            val localDate = instant.atZone(zoneId).toLocalDate()

            // 3. Sử dụng TemporalAdjusters.firstDayOfMonth() để lấy ngày đầu tiên của tháng
            val firstDayOfMonth = localDate.with(TemporalAdjusters.firstDayOfMonth())

            // 4. Chuyển đổi LocalDate (ngày đầu tháng) trở lại thành Instant (đầu ngày)
            // và sau đó thành mili giây từ Epoch.
            // Chúng ta cần chỉ định múi giờ để chuyển đổi LocalDate thành ZonedDateTime,
            // sau đó mới sang Instant.
            return firstDayOfMonth.atStartOfDay(zoneId).toInstant().toEpochMilli()
        }

    }
}