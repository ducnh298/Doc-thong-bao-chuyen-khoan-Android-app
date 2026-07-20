package com.app.docthongbaochuyenkhoan.ui.activity

import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // Flag để tránh mở dialog khi khôi phục trạng thái "Tự chọn"
    private var isRestoringCustomRange = false
    private var currentDataList: List<Amount> = emptyList()
    private var chartDataGeneration = 0

    companion object {
        var savedSpinnerPosition = 0
        var savedCustomStartDate = -1L
        var savedCustomEndDate = -1L
        var savedCustomIsMonthly = false
        var pendingNavigateToDate: Long? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        barChart = findViewById(R.id.barChart)
        barChart.setDoubleTapToZoomEnabled(false)

        lifecycleScope.launch {
            viewModel.dailyAmounts.collect { dailyAmounts ->
                if (dailyAmounts.isNotEmpty()) initChartData(dailyAmounts)
            }
        }

        lifecycleScope.launch {
            viewModel.monthlyAmounts.collect { monthlyAmounts ->
                if (monthlyAmounts.isNotEmpty()) initChartData(monthlyAmounts)
            }
        }

        lifecycleScope.launch {
            viewModel.statusMessage.collect { s ->
                withContext(Dispatchers.Main) {
                    binding.barChart.let { chart ->
                        chart.setNoDataText(s)
                        chart.setNoDataTextColor(resources.getColor(R.color.text_color_dialog))

                        val p: Paint = chart.getPaint(Chart.PAINT_INFO)
                        p.textSize = 40F
                        p.isFakeBoldText = true

                        chart.invalidate()
                    }
                }
            }
        }

        val spinnerTime = binding.spinnerTime
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
                savedSpinnerPosition = position
                updateResetButtonVisibility(position)
                loadDataBySelectedOptionPosition(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Khôi phục trạng thái đã lưu
        val pos = savedSpinnerPosition
        if (pos == 6 && savedCustomStartDate != -1L) {
            isRestoringCustomRange = true
        }
        if (pos != 0) {
            spinnerTime.setSelection(pos)
        }
        // pos == 0: adapter tự fire onItemSelected(0) → loadDataBySelectedOptionPosition(0) → 7 ngày

        binding.tvDateRange.setOnClickListener {
            openDatePickerStatisticDialog()
        }
        binding.btnChooseDateRange.setOnClickListener {
            openDatePickerStatisticDialog()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnReset.setOnClickListener {
            savedSpinnerPosition = 0
            savedCustomStartDate = -1L
            savedCustomEndDate = -1L
            savedCustomIsMonthly = false
            binding.spinnerTime.setSelection(0)
            // updateResetButtonVisibility sẽ được gọi từ spinner listener
        }

        binding.btnChooseDateRange.addClickAnimation()
        binding.btnBack.addClickAnimation()
        binding.btnReset.addClickAnimation()
    }

    private val viewModel: StatisticsViewModel by viewModels<StatisticsViewModel> {
        StatisticsViewModelFactory(AppDatabase.getDatabase(this).transactionDao())
    }

    private fun updateResetButtonVisibility(position: Int) {
        binding.btnReset.visibility = if (position != 0) View.VISIBLE else View.GONE
    }

    private fun initChartData(dataList: List<Amount>) {
        currentDataList = dataList
        val gen = ++chartDataGeneration
        barChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val index = e?.x?.toInt() ?: return
                val amount = currentDataList.getOrNull(index) ?: return
                val timestamp = when (amount) {
                    is DailyAmount -> DateUtils.parseDateToStartOfDay(amount.label)
                    is MonthlyAmount -> DateUtils.parseMonthToFirstDayStartOfDay(amount.label)
                    else -> return
                }
                if (timestamp > 0) {
                    pendingNavigateToDate = timestamp
                    finish()
                }
            }

            override fun onNothingSelected() {}
        })

        try {
            val size = dataList.size
            val maxVisible = minOf(size, 12)
            val isScrollable = size > maxVisible
            val scrollMax = size - maxVisible
            val barTextSize = if (maxVisible <= 7) 12f else 10f

            val entriesSent = mutableListOf<BarEntry>()
            val entriesReceived = mutableListOf<BarEntry>()
            val labels = mutableListOf<String>()
            var totalSent = 0L
            var totalReceived = 0L

            dataList.forEachIndexed { index, item ->
                labels.add(
                    item.label.substring(0, 2) +
                    (if (item is DailyAmount && maxVisible <= 7) " " + DateUtils.getDayOfWeek(item.label)
                    else if (item is MonthlyAmount && maxVisible <= 6) " " + DateUtils.getMonthOfYear(item.label)
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

            sentDataSet = BarDataSet(listOf<BarEntry>(), "Tiền chuyển").apply {
                color = resources.getColor(R.color.text_color_amount_negative)
                valueTextSize = barTextSize
                valueTextColor = color
            }

            receivedDataSet = BarDataSet(listOf<BarEntry>(), "Tiền nhận").apply {
                color = resources.getColor(R.color.text_color_amount_positive)
                valueTextSize = barTextSize
                valueTextColor = color
            }

            sentDataSet.values = entriesSent
            receivedDataSet.values = entriesReceived

            // Sent: ẩn "0", chỉ hiện khi > 0
            sentDataSet.setValueFormatter(object : ValueFormatter() {
                override fun getBarLabel(barEntry: BarEntry): String {
                    return if (barEntry.y == 0f) "" else valueFormatter.getFormattedValue(barEntry.y)
                }
            })

            // Received: ẩn "0" khi chỉ mình nó = 0; hiện "0" khi cả 2 đều = 0
            receivedDataSet.setValueFormatter(object : ValueFormatter() {
                override fun getBarLabel(barEntry: BarEntry): String {
                    if (barEntry.y > 0f) return valueFormatter.getFormattedValue(barEntry.y)
                    // x sau groupBars() = index + offset nhỏ → toInt() cho ra index gốc
                    val idx = barEntry.x.toInt()
                    val bothZero = dataList.getOrNull(idx)?.let { it.received == 0L && it.sent == 0L } ?: false
                    return if (bothZero) "0" else ""
                }
            })

            val barData = BarData(receivedDataSet, sentDataSet).apply {
                barWidth = 0.3f
            }

            barChart.apply {
                data = barData
                setScaleEnabled(false)
                isDragEnabled = isScrollable

                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(labels)
                    granularity = 1f
                    axisMinimum = 0f
                    position = XAxis.XAxisPosition.BOTTOM
                    axisMaximum = labels.size.toFloat()
                    labelCount = maxVisible
                    setCenterAxisLabels(true)
                    setAvoidFirstLastClipping(true)
                    textSize = barTextSize
                    textColor = resources.getColor(R.color.text_color)
                }

                axisLeft.valueFormatter = valueFormatter
                axisLeft.textColor = resources.getColor(R.color.text_color)
                axisRight.isEnabled = false
                description.isEnabled = false
                extraBottomOffset = 20f

                setFitBars(true)
                groupBars(0f, 0.3f, 0.05f)
                invalidate()

                legend.textSize = 14f
                legend.textColor = resources.getColor(R.color.text_color)
                legend.xEntrySpace = 30f
                legend.formSize = 14f
                legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            }

            // SeekBar
            val seekBar = binding.seekBarChart
            if (isScrollable) {
                seekBar.max = scrollMax
                seekBar.progress = scrollMax   // Bắt đầu ở cuối (ngày gần nhất)
                seekBar.visibility = View.VISIBLE

                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        if (fromUser) barChart.moveViewToX(progress.toFloat())
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })

                barChart.onChartGestureListener = object : OnChartGestureListener {
                    override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {
                        seekBar.progress = barChart.lowestVisibleX.roundToInt().coerceIn(0, scrollMax)
                    }
                    override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
                    override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
                    override fun onChartLongPressed(me: MotionEvent?) {}
                    override fun onChartDoubleTapped(me: MotionEvent?) {}
                    override fun onChartSingleTapped(me: MotionEvent?) {}
                    override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
                    override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}
                }

                // Dùng post{} để đảm bảo chart đã được layout trước khi set viewport
                // gen guard: bỏ qua nếu đây là post cũ (user đã chọn thời gian khác)
                barChart.post {
                    if (gen != chartDataGeneration) return@post
                    barChart.setVisibleXRangeMaximum(maxVisible.toFloat())
                    barChart.moveViewToX(size.toFloat())
                    seekBar.progress = barChart.lowestVisibleX.roundToInt().coerceIn(0, scrollMax)
                }
            } else {
                seekBar.visibility = View.GONE
                barChart.onChartGestureListener = null
                // Reset viewport về trạng thái ban đầu (hiện tất cả dữ liệu)
                barChart.post {
                    if (gen != chartDataGeneration) return@post
                    barChart.fitScreen()
                }
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
            if (fragment.isVisible) {
                fragment.dismiss()
                fragment.show(supportFragmentManager, "DatePickerDialogStatisticFragment")
            }
        } else {
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
                        "${if (absValue % 1000000 / 100000 > 0) ("." + (absValue % 1000000 / 100000).toInt()) else ""}M"
            } else if (absValue >= 1000) {
                "${(value / 1000).toInt()}K"
            } else {
                value.toInt().toString()
            }
        }
    }

    override fun onConfirmClicked(startDate: Long, endDate: Long, isStatisticByMonth: Boolean) {
        binding.spinnerTime.setSelection(6)
        savedCustomStartDate = startDate
        savedCustomEndDate = endDate
        savedCustomIsMonthly = isStatisticByMonth

        if (isStatisticByMonth && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            viewModel.loadTransactionAmountsFromDayToDayByMonths(startDate, endDate)
        else
            viewModel.loadTransactionAmountsFromDayToDay(startDate, endDate)
    }

    fun loadDataBySelectedOptionPosition(position: Int) {
        val selectedItem = binding.spinnerTime.getItemAtPosition(position).toString()
        when (selectedItem) {
            statisticsRangeOfDayList[0] -> viewModel.loadTransactionAmountsByDays(Calendar.getInstance(), 7)
            statisticsRangeOfDayList[1] -> viewModel.loadTransactionAmountsByDays(Calendar.getInstance(), 14)
            statisticsRangeOfDayList[2] -> viewModel.loadTransactionAmountsByDays(Calendar.getInstance(), 30)
            statisticsRangeOfDayList[3] -> viewModel.loadTransactionAmountsByDays(Calendar.getInstance(), 90)
            statisticsRangeOfDayList[4] -> viewModel.loadTransactionAmountsByMonths(Calendar.getInstance(), 6)
            statisticsRangeOfDayList[5] -> viewModel.loadTransactionAmountsByMonths(Calendar.getInstance(), 12)
            statisticsRangeOfDayList[6] -> {
                if (isRestoringCustomRange) {
                    isRestoringCustomRange = false
                    val start = savedCustomStartDate
                    val end = savedCustomEndDate
                    if (savedCustomIsMonthly && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        viewModel.loadTransactionAmountsFromDayToDayByMonths(start, end)
                    else
                        viewModel.loadTransactionAmountsFromDayToDay(start, end)
                } else {
                    openDatePickerStatisticDialog()
                }
            }
            else -> viewModel.loadTransactionAmountsByDays(Calendar.getInstance(), 7)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        binding.tvDateRange.setOnClickListener { null }
        binding.btnChooseDateRange.setOnClickListener { null }
        binding.btnBack.setOnClickListener { null }
        binding.btnReset.setOnClickListener { null }
    }
}
