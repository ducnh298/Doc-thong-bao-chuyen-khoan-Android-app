package com.app.docthongbaochuyenkhoan.activity

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.databinding.ActivityStatisticsBinding
import com.app.docthongbaochuyenkhoan.dialog.SettingDialogFragment
import com.app.docthongbaochuyenkhoan.model.DailyAmount
import com.app.docthongbaochuyenkhoan.model.database.AppDatabase
import com.app.docthongbaochuyenkhoan.utils.AppUtils
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs


class StatisticsActivity : AppCompatActivity(), SettingDialogFragment.SettingDialogListener {
    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var barChart: BarChart
    private lateinit var sentDataSet: BarDataSet
    private lateinit var receivedDataSet: BarDataSet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.loadDailyAmounts(Calendar.getInstance(), 6)    // Default get data in 7 days

        // Quan sát dữ liệu từ ViewModel và cập nhật biểu đồ
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.dailyAmounts.collectLatest { dailyAmounts ->
                if (dailyAmounts.isNotEmpty()) initChartData(dailyAmounts)
            }
        }

        TaskbarManager(this)

        val spinnerTime = findViewById<Spinner>(R.id.spinnerTime)
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_time_item,
            listOf("7 ngày", "1 tháng", "3 tháng", "6 tháng")
        )
        spinnerTime.adapter = adapter
    }

    private val viewModel: StatisticsViewModel by viewModels<StatisticsViewModel> {
        StatisticsViewModelFactory(AppDatabase.getDatabase(this).transactionDao())
    }

    private fun initChartData(dailyAmounts: List<DailyAmount>) {
        barChart = findViewById(R.id.barChart)

        barChart.clear()
        val entriesSent = mutableListOf<BarEntry>()
        val entriesReceived = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        var totalSent = 0L
        var totalReceived = 0L

        dailyAmounts.forEachIndexed { index, data ->
            labels.add(
                data.day.substring(
                    0,
                    2
                ) + " " + DateUtils.getDayOfWeek(data.day)
            )

            entriesSent.add(BarEntry(index.toFloat(), data.sent.toFloat()))
            entriesReceived.add(BarEntry(index.toFloat(), data.received.toFloat()))

            totalSent += data.sent
            totalReceived += data.received
        }

        runOnUiThread {
            updateTvDateRange(dailyAmounts.first().day, dailyAmounts.last().day)
            updateTotalAmount(totalSent, totalReceived)
        }

        sentDataSet = BarDataSet(listOf<BarEntry>(), "Tiền gửi").apply {
            color = resources.getColor(R.color.light_red)
            valueTextSize = 12f
            valueTextColor = color
        }

        receivedDataSet = BarDataSet(listOf<BarEntry>(), "Tiền nhận").apply {
            color = resources.getColor(R.color.blue)
            valueTextSize = 12f
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
            }

            axisLeft.valueFormatter = valueFormatter
            axisRight.isEnabled = false
            description.isEnabled = false
            extraBottomOffset = 20f

            setFitBars(true)
            barChart.groupBars(0f, 0.3f, 0.05f) // Nhóm các cột gần nhau
            invalidate()

            legend.textSize = 14f
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        }
    }

    private fun updateTvDateRange(startDate: String, endDate: String) {
        findViewById<TextView>(R.id.tvDateRange).text = "$startDate - $endDate"
    }

    private fun updateTotalAmount(totalSent: Long, totalReceived: Long) {
        findViewById<TextView>(R.id.tvTotalSent).text =
            "Tổng tiền chuyển: " + AppUtils.formatCurrency(totalSent)
        findViewById<TextView>(R.id.tvTotalReceived).text =
            "Tổng tiền nhận: " + AppUtils.formatCurrency(totalReceived)
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

    private fun showDateRangePicker() {

    }

    override fun onTvNotificationSoundClicked(): View.OnClickListener {
        TODO("Not yet implemented")
    }
}