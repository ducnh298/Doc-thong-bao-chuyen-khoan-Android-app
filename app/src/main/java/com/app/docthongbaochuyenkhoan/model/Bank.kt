package com.app.docthongbaochuyenkhoan.model

import com.app.docthongbaochuyenkhoan.R

enum class Bank(val displayName: String, val logo: Int, vararg val aliases: String) {
    UNKNOWN("", 0, ""),
    TECHCOMBANK(
        "Techcombank", R.drawable.logo_techcombank, "techcombank", "tcb", "tcbank"
    ),
    MBBANK("MBBank", R.drawable.logo_mbbank, "mbmobile", "mbbank", "mbb"),
    VIETCOMBANK("Vietcombank", R.drawable.logo_vietcombank, "VCB", "vietcombank", "vcb"),
    VPBANK("VPBank", R.drawable.logo_vpbank, "vpbankonline", "vpb"),
    TPBANK("TPBank", R.drawable.logo_tpbank, "tpbank"),
    SACOMBANK("Sacombank", R.drawable.logo_sacombank, "sacombank"),
    BIDV("BIDV", R.drawable.logo_bidv, "bidv"),
    VIETINBANK("Vietinbank", R.drawable.logo_vietinbank, "vietinbank"),
    ACB("ACB", R.drawable.logo_acb, "acbbanking"),
    HDBANK("HDBank", R.drawable.logo_hdbank, "hdbank"),
    MOMO("Momo", R.drawable.logo_momo, "momo", "momotransfer"),
    VIETTELMONEY(
        "Viettelmoney",
        R.drawable.logo_viettel_money,
        "vtpay",
        "viettelpay",
        "viettelmoney"
    ),
    ZALOPAY("Zalopay", R.drawable.logo_zalo_pay, "zalopay");

    companion object {
        fun fromName(name: String?): Bank? {
            return entries.find { it.displayName.equals(name, ignoreCase = true) }
        }

        fun fromPackageName(packageName: String): Bank {   //  Example: com.mbmobile
            for (bank in entries) {
                if (bank != UNKNOWN)
                    for (alias in bank.aliases) {
                        if (packageName.contains(alias, ignoreCase = true))
                            return bank
                    }
            }
            return UNKNOWN
        }
    }
}