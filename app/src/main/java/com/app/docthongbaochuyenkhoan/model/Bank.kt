package com.app.docthongbaochuyenkhoan.model

import com.app.docthongbaochuyenkhoan.R
import java.io.Serializable

enum class Bank(val speakName: String, val logo: Int, vararg val aliases: String): Serializable {
    UNKNOWN("", 0, ""),
    TECHCOMBANK(
        "Techcombank", R.drawable.logo_techcombank, "techcombank", "tcb", "tcbank"
    ),
    MBBANK(
        "MBBank",
        R.drawable.logo_mbbank,
        "mbmobile",
        "mbbank",
        "mbb"
    ),
    VIETCOMBANK(
        "Vietcombank",
        R.drawable.logo_vietcombank,
        "VCB",
        "vietcombank",
        "vcb"
    ),
    AGRIBANK("Agribank", R.drawable.logo_agribank, "Agribank3g", "agrib"),
    VPBANK(
        "VPBank",
        R.drawable.logo_vpbank,
        "vpbankonline",
        "vpb"
    ),
    TPBANK("TPBank", R.drawable.logo_tpbank, "tpb", "tpbank"),
    SACOMBANK(
        "Sacombank",
        R.drawable.logo_sacombank,
        "sacombank"
    ),
    BIDV("BIDV", R.drawable.logo_bidv, "bidv"),
    VIETINBANK(
        "Vietinbank",
        R.drawable.logo_vietinbank,
        "vietinbank"
    ),
    ACB("ACB", R.drawable.logo_acb, "acb", "acbbanking"),
    HDBANK(
        "HDBank",
        R.drawable.logo_hdbank,
        "hdbank"
    ),
    MOMO("Momo", R.drawable.logo_momo, "momo", "momotransfer"),
    VIETTELMONEY(
        "Viettel money", R.drawable.logo_viettel_money, "vtpay", "viettelpay", "viettelmoney"
    ),
    ZALOPAY("Zalopay", R.drawable.logo_zalo_pay, "zalopay"),
    SHINHANBANK(
        "Shinhan Bank",
        R.drawable.logo_shinhanbank,
        "shinhan"
    ),
    OCB(
        "OCB", R.drawable.logo_ocb, "ocb", "ocbbanking", "ocbmobile"
    ),

    MSB(
        "MSB", R.drawable.logo_msb, "msb", "msbbank", "msbmobile"
    ),

    SEABANK(
        "SeABank", R.drawable.logo_seabank, "seabank", "seabankmobile"
    ),

    LIENVIETPOSTBANK(
        "LienVietPostBank", R.drawable.logo_lpbank, "lienvietpostbank", "lpb", "lpbank"
    ),

    BACABANK(
        "Bac A Bank", R.drawable.logo_babank, "bacabank", "bab"
    ),

    ABBANK(
        "ABBank", R.drawable.logo_abbank, "abbank", "abbanking"
    ),

    PVCOMBANK(
        "PVcomBank", R.drawable.logo_pvcombank, "pvcombank", "pvcom"
    ),

    EXIMBANK(
        "Eximbank", R.drawable.logo_eximbank, "eximbank", "eximbankmobile"
    ),

    NAMABANK(
        "Nam A Bank", R.drawable.logo_namabank, "namabank", "nab"
    ),

    KIENLONGBANK(
        "KienlongBank", R.drawable.logo_kienlongbank, "kienlongbank", "klb"
    ),

    BAOVIETBANK(
        "BaoViet Bank", R.drawable.logo_baovietbank, "baovietbank", "baoviet"
    ),

    SAIGONBANK(
        "SaigonBank", R.drawable.logo_sgb, "saigonbank", "sgb"
    ),

    COOPBANK(
        "Co-opBank", R.drawable.logo_coopbank, "coopbank"
    ),

    HSBC(
        "HSBC",
        R.drawable.logo_hsbc,
        "hsbc",
        "hsbcvietnam"
    );


    companion object {
        fun fromName(name: String?): Bank? {
            return entries.find { it.speakName.equals(name, ignoreCase = true) }
        }

        fun fromPackageName(packageName: String): Bank {
            for (bank in entries) {
                if (bank != UNKNOWN) for (alias in bank.aliases) {
                    if (packageName.contains(alias, ignoreCase = true)) return bank
                }
            }
            return UNKNOWN
        }
    }
}