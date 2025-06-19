package com.app.docthongbaochuyenkhoan.model

interface Amount {
    val label: String
    val received: Long
    val sent: Long
}

data class DailyAmount(
    val day: String,
    override val received: Long,
    override val sent: Long
) : Amount {
    override val label: String get() = day // Trả về ngày
}

data class MonthlyAmount(
    val month: String,
    override val received: Long,
    override val sent: Long
) : Amount {
    override val label: String get() = month // Trả về tháng
}