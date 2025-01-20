package com.app.docthongbaochuyenkhoan.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import java.text.NumberFormat
import java.util.Currency

class AppUtils {
    companion object {
        private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance().apply {
            maximumFractionDigits = 0
            currency = Currency.getInstance("VND")
        }

        fun formatCurrency(amount: Long): String {
            return currencyFormat.format(amount)
        }

        @SuppressLint("ClickableViewAccessibility")
        fun View.addClickAnimation() {
            this.setOnTouchListener { v: View, event: MotionEvent ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.9f).scaleY(0.9f)
                        .setDuration(50)
                        .start()

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> v.animate()
                        .scaleX(1f).scaleY(1f).setDuration(50)
                        .start()
                }
                false
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        fun ImageButton.addClickAnimation() {
            this.setOnTouchListener { v: View, event: MotionEvent ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.8f).scaleY(0.8f)
                        .setDuration(100)
                        .start()

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> v.animate()
                        .scaleX(1f).scaleY(1f).setDuration(50)
                        .start()
                }
                false
            }
        }

        fun getDialogDimension(activity: Activity, dialog: Dialog): WindowManager.LayoutParams {
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(dialog.window!!.attributes)
            lp.width =
                (activity.resources.displayMetrics.widthPixels * 0.95).toInt() // 90% of screen width
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            return lp
        }
    }
}