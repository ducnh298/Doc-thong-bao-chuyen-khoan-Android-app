package com.app.docthongbaochuyenkhoan.ui.activity

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.databinding.ActivityStatisticsBinding
import com.app.docthongbaochuyenkhoan.model.Amount
import com.app.docthongbaochuyenkhoan.model.DailyAmount
import com.app.docthongbaochuyenkhoan.model.MonthlyAmount
import com.app.docthongbaochuyenkhoan.model.database.AppDatabase
import com.app.docthongbaochuyenkhoan.ui.dialog.DatePickerDialogStatisticFragment
import com.app.docthongbaochuyenkhoan.utils.AppUtils
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.addClickAnimation
import com.app.docthongbaochuyenkhoan.utils.DateUtils
import com.app.docthongbaochuyenkhoan.viewModel.StatisticsViewModel
import com.app.docthongbaochuyenkhoan.viewModel.StatisticsViewModelFactory
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs


class StatisticsActivity : AppCompatActivity(),
    DatePickerDialogStatisticFragment.DatePickerDialogStatisticListener {
    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var barChart: BarChart
    private lateinit var sentDataSet: BarDataSet
    private lateinit var receivedDataSet: BarDataSet
    private val statisticsRangeOfDayList =
        listOf("7 ngày", "14 ngày", "1 tháng", "3 tháng", "6 tháng", "1 năm", "Tự chọn")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        barChart = findViewById(R.id.barChart)
        viewModel.loadTransactionAmountsByDays(
            Calendar.getInstance(),
            7
        )    // Default get data in 7 days

        // Quan sát dữ liệu từ ViewModel và cập nhật biểu đồ
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.dailyAmounts.collect { dailyAmounts ->
                if (dailyAmounts.isNotEmpty()) initChartData(dailyAmounts)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            viewModel.monthlyAmounts.collect { monthlyAmounts ->
                if (monthlyAmounts.isNotEmpty()) initChartData(monthlyAmounts)
            }
        }

        val spinnerTime = findViewById<Spinner>(R.id.spinnerTime)
        val adapter = ArrayAdapter(
            this,
            R.layout.item_spinner_statistics_time,
            statisticsRangeOfDayList
        )
        spinnerTime.adapter = adapter
        spinnerTime.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent.getItemAtPosition(position).toString()
                when (selectedItem) {
                    statisticsRangeOfDayList[0] -> viewModel.loadTransactionAmountsByDays(
                        Calendar.getInstance(),
                        7
                    )

                    statisticsRangeOfDayList[1] -> viewModel.loadTransactionAmountsByDays(
                        Calendar.getInstance(),
                        14
                    )

                    statisticsRangeOfDayList[2] -> viewModel.loadTransactionAmountsByDays(
                        Calendar.getInstance(),
                        30
                    )

                    statisticsRangeOfDayList[3] -> viewModel.loadTransactionAmountsByDays(
                        Calendar.getInstance(),
                        90
                    )

                    statisticsRangeOfDayList[4] -> viewModel.loadTransactionAmountsByMonths(
                        Calendar.getInstance(),
                        6
                    )

                    statisticsRangeOfDayList[5] -> viewModel.loadTransactionAmountsByMonths(
                        Calendar.getInstance(),
                        12
                    )

                    statisticsRangeOfDayList[6] -> openDatePickerStatisticDialog()

                    else -> viewModel.loadTransactionAmountsByDays(Calendar.getInstance(), 7)
                }
            } // to close the onItemSelected

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        binding.tvDateRange.setOnClickListener {
            openDatePickerStatisticDialog()
        }
        binding.btnChooseDateRange.setOnClickListener {
            openDatePickerStatisticDialog()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnChooseDateRange.addClickAnimation()
        binding.btnBack.addClickAnimation()
    }

    private val viewModel: StatisticsViewModel by viewModels<StatisticsViewModel> {
        StatisticsViewModelFactory(AppDatabase.getDatabase(this).transactionDao())
    }

    private fun initChartData(dataList: List<Amount>) {
        try {
            val size = dataList.size
            val entriesSent = mutableListOf<BarEntry>()
            val entriesReceived = mutableListOf<BarEntry>()
            val labels = mutableListOf<String>()
            var totalSent = 0L
            var totalReceived = 0L

            dataList.forEachIndexed { index, item ->
                labels.add(
                    item.label.substring(
                        0,
                        2
                    ) + (if (item is DailyAmount && size <= 7) " " + DateUtils.getDayOfWeek(item.label)
                    else if (item is MonthlyAmount && size <= 6) " " + DateUtils.getMonthOfYear(item.label)
                    else "")
                )

                entriesSent.add(BarEntry(index.toFloat(), item.sent.toFloat()))
                entriesReceived.add(BarEntry(index.toFloat(), item.received.toFloat()))

                totalSent += item.sent
                totalReceived += item.received
            }

            runOnUiThread {
                updateTvDateRange(dataList.first().label, dataList.last().label)
                updateTotalAmount(totalSent, totalReceived)
            }

            sentDataSet = BarDataSet(listOf<BarEntry>(), "Tiền gửi").apply {
                color = resources.getColor(R.color.text_color_amount_negative)
                valueTextSize =
                    if (size <= 7) 12f else if (size <= 14) 10f else if (size <= 30) 8f else 6f
                valueTextColor = color
            }

            receivedDataSet = BarDataSet(listOf<BarEntry>(), "Tiền nhận").apply {
                color = resources.getColor(R.color.text_color_amount_positive)
                valueTextSize =
                    if (size <= 7) 12f else if (size <= 14) 10f else if (size <= 30) 8f else 6f
                valueTextColor = color
            }

            sentDataSet.values = entriesSent
            receivedDataSet.values = entriesReceived

            val barData = BarData(sentDataSet, receivedDataSet).apply {
                barWidth = 0.3f // Điều chỉnh độ rộng cột
                setValueFormatter(valueFormatter)
            }

            barChart.apply {
                data = barData
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(labels) // Hiển thị nhãn trên trục X
                    granularity = 1f
                    axisMinimum = 0f
                    position = XAxis.XAxisPosition.BOTTOM
                    axisMaximum = labels.size.toFloat()
                    barChart.xAxis.labelCount = labels.size
                    setCenterAxisLabels(true) // Căn chỉnh cột theo nhóm
                    setAvoidFirstLastClipping(true)
                    textSize =
                        if (size <= 7) 12f else if (size <= 14) 10f else if (size <= 30) 8f else 6f
                    textColor = resources.getColor(R.color.text_color)
                }

                axisLeft.valueFormatter = valueFormatter
                axisLeft.textColor = resources.getColor(R.color.text_color)
                axisRight.isEnabled = false
                description.isEnabled = false

                extraBottomOffset = 20f

                setFitBars(true)
                barChart.groupBars(0f, 0.3f, 0.05f) // Nhóm các cột gần nhau
                invalidate()

                legend.textSize = 14f
                legend.textColor = resources.getColor(R.color.text_color)
                legend.xEntrySpace = 30f   // Tăng khoảng cách ngang giữa các mục
                legend.formSize = 14f      // Kích thước ô màu
                legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            }
        } catch (e: Exception) {
            Log.e("StatisticsActivity", "Error: ${e.message}")
            Toast.makeText(this, "Lỗi đọc dữ liệu thống kê, vui lòng thử lại.", Toast.LENGTH_SHORT)
                .show()
            finish()
        }
    }

    private fun openDatePickerStatisticDialog() {
        val fragment = supportFragmentManager.findFragmentByTag("DatePickerDialogStatisticFragment")

        if (fragment != null && fragment is DatePickerDialogStatisticFragment) {
            // If fragment has been added
            if (fragment.isVisible) {
                fragment.dismiss() // Make sure the fragment is deleted before displaying it again
                fragment.show(supportFragmentManager, "DatePickerDialogStatisticFragment")
            }
        } else {
            // If the fragment does not exist, create a new one and display it
            val dialog = DatePickerDialogStatisticFragment.newInstance(this)
            dialog.show(supportFragmentManager, "DatePickerDialogStatisticFragment")
        }
    }

    private fun updateTvDateRange(startDate: String, endDate: String) {
        findViewById<TextView>(R.id.tvDateRange).text = "$startDate - $endDate"
    }

    private fun updateTotalAmount(totalSent: Long, totalReceived: Long) {
        findViewById<TextView>(R.id.tvTotalSent).text = AppUtils.formatCurrency(totalSent)
        findViewById<TextView>(R.id.tvTotalReceived).text = AppUtils.formatCurrency(totalReceived)
    }

    private val valueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val absValue = abs(value)
            return if (absValue >= 1000000) {
                "${(value / 1000000).toInt()}" +
                        "${if (absValue % 1000000 / 100000 > 0) ("." + (absValue % 1000000 / 100000).toInt()) else ""}M" // Đổi sang đơn vị triệu
            } else if (absValue >= 1000) {
                "${(value / 1000).toInt()}K" // Đổi sang đơn vị nghìn
            } else {
                value.toInt().toString() // Hiển thị số bình thường nếu nhỏ
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onConfirmClicked(startDate: Long, endDate: Long, isStatisticByMonth: Boolean) {
        binding.spinnerTime.setSelection(6)

        if (isStatisticByMonth)
            viewModel.loadTransactionAmountsFromDayToDayByMonths(startDate, endDate)
        else
            viewModel.loadTransactionAmountsFromDayToDay(startDate, endDate)
    }
}