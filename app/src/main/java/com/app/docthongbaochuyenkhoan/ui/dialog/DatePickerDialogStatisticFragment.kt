package com.app.docthongbaochuyenkhoan.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.databinding.DialogDatePickerStatisticBinding
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.addClickAnimation
import com.app.docthongbaochuyenkhoan.utils.DateUtils
import java.util.Calendar

class DatePickerDialogStatisticFragment : DialogFragment() {
    interface DatePickerDialogStatisticListener {
        fun onConfirmClicked(startDate: Long, endDate: Long)
    }

    private var selectedStartDate = 0L
    private var selectedEndDate = 0L
    private var isSelectStartDate = true
    private lateinit var listener: DatePickerDialogStatisticListener

    companion object {
        fun newInstance(
            listener: DatePickerDialogStatisticListener
        ): DatePickerDialogStatisticFragment {
            val fragment = DatePickerDialogStatisticFragment()
            fragment.listener = listener
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
        val binding = DialogDatePickerStatisticBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        val dialogDatePicker = builder.create()
        dialogDatePicker.let { dialog ->
            dialog.window?.setGravity(Gravity.CENTER)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

            binding.datePicker.let {

                val c: Calendar = Calendar.getInstance()
                c.timeInMillis = System.currentTimeMillis()
                it.init(
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH), { _, year, month, day ->
                        val calendar = Calendar.getInstance().apply {
                            set(year, month, day, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        if (isSelectStartDate) {
                            selectedStartDate = calendar.timeInMillis
                            binding.tvStart.text = DateUtils.formatDate(selectedStartDate)

                            isSelectStartDate = false
                        } else {
                            if (calendar.timeInMillis < selectedStartDate)
                                Toast.makeText(
                                    requireContext(),
                                    "Ngày kết thúc phải sau ngày bắt đầu",
                                    Toast.LENGTH_SHORT
                                ).show()
                            else {
                                selectedEndDate = calendar.timeInMillis
                                binding.tvEnd.text = DateUtils.formatDate(selectedEndDate)

                                isSelectStartDate = true
                            }
                        }
                    }
                )

                it.maxDate = System.currentTimeMillis()
            }

            binding.btnConfirm.setOnClickListener {
                if (selectedStartDate > 0 && selectedEndDate > 0) {
                    if (selectedStartDate > selectedEndDate)
                        Toast.makeText(
                            requireContext(),
                            "Ngày kết thúc phải sau ngày bắt đầu",
                            Toast.LENGTH_SHORT
                        ).show()
                    else {
                        listener.onConfirmClicked(selectedStartDate, selectedEndDate)
                        dialog.dismiss()
                    }
                } else Toast.makeText(
                    requireContext(),
                    "Vui lòng chọn ngày bắt đầu và ngày kết thúc",
                    Toast.LENGTH_SHORT
                ).show()
            }
            binding.btnClose.setOnClickListener { dialog.dismiss() }

            binding.btnConfirm.addClickAnimation()
            binding.btnClose.addClickAnimation()
        }
        return dialogDatePicker
    }
}