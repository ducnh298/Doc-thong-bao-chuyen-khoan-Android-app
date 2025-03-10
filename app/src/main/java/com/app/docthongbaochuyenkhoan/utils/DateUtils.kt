package com.app.docthongbaochuyenkhoan.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DateUtils {
    companion object{
        private val sdfDateTime = SimpleDateFormat(" HH:mm:ss - dd/MM/yyyy")
        private val sdfDate = SimpleDateFormat("dd/MM/yyyy")

        fun formatDateTime(timestamp: Long): String {
            return sdfDateTime.format(java.util.Date(timestamp))
        }

        fun formatDate(timestamp: Long): String {
            return sdfDate.format(java.util.Date(timestamp))
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
    }
}