package com.app.docthongbaochuyenkhoan.dialog

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.DatePicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.databinding.DialogDatePickerBinding
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.addClickAnimation
import java.util.Calendar

class DatePickerDialogFragment : DialogFragment() {
    interface DatePickerDialogListener {
        fun onDateChanged(dialog: AlertDialog): DatePicker.OnDateChangedListener
    }

    private var today = 0L
    private var selectedDay = 0L
    private lateinit var listener: DatePickerDialogListener

    companion object {
        fun newInstance(
            today: Long,
            selectedDay: Long,
            listener: DatePickerDialogListener
        ): DatePickerDialogFragment {
            val fragment = DatePickerDialogFragment()
            fragment.today = today
            fragment.selectedDay = selectedDay
            fragment.listener = listener
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
        val bindingDialogDatePicker = DialogDatePickerBinding.inflate(layoutInflater)
        builder.setView(bindingDialogDatePicker.root)

        val dialogDatePicker = builder.create()
        dialogDatePicker.let { dialog ->
            dialog.window?.setGravity(Gravity.CENTER)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

            bindingDialogDatePicker.datePicker.let {

                val c: Calendar = Calendar.getInstance()
                c.timeInMillis = selectedDay
                it.init(
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH), null
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    it.setOnDateChangedListener(
                        listener.onDateChanged(dialog)
                    )

                it.maxDate = today
            }

            bindingDialogDatePicker.btnClose.setOnClickListener { dialog.dismiss() }
            bindingDialogDatePicker.btnClose.addClickAnimation()
        }
        return dialogDatePicker
    }
}