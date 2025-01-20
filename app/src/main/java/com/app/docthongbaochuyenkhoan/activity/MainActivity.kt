package com.app.docthongbaochuyenkhoan.activity

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.adapter.TransactionAdapter
import com.app.docthongbaochuyenkhoan.controller.SharedPreferencesManager
import com.app.docthongbaochuyenkhoan.databinding.ActivityMainBinding
import com.app.docthongbaochuyenkhoan.dialog.DatePickerDialogFragment
import com.app.docthongbaochuyenkhoan.dialog.RequestPermissionsDialogFragment
import com.app.docthongbaochuyenkhoan.dialog.SettingDialogFragment
import com.app.docthongbaochuyenkhoan.flow.TransactionFlowManager
import com.app.docthongbaochuyenkhoan.model.Transaction
import com.app.docthongbaochuyenkhoan.model.database.AppDatabase
import com.app.docthongbaochuyenkhoan.model.database.TransactionDao
import com.app.docthongbaochuyenkhoan.service.MyNotificationListenerService
import com.app.docthongbaochuyenkhoan.utils.AppUtils
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.addClickAnimation
import com.app.docthongbaochuyenkhoan.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), SettingDialogFragment.SettingDialogListener,
    DatePickerDialogFragment.DatePickerDialogListener {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var binding: ActivityMainBinding
    private lateinit var dialogRequestPermissions: RequestPermissionsDialogFragment
    private lateinit var transactionDao: TransactionDao
    private lateinit var transactionAdapter: TransactionAdapter
    private var transactionList = mutableListOf<Transaction>()
    private var today = 0L
    private var selectedDay = 0L
    private lateinit var ringtonePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_blue)

        if (!SharedPreferencesManager.isInitialized())
            SharedPreferencesManager.init(this)

        AppCompatDelegate.setDefaultNightMode(
            if (SharedPreferencesManager.isNightModeEnabled()) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        transactionDao = AppDatabase.getDatabase(this).transactionDao()

        today = DateUtils.getStartTimeOfToday()

        selectedDay = savedInstanceState?.getLong("selectedDay", today) ?: today

        initNotificationSwitch()
        initTVRequestNotificationAccessPermission()
        initRecyclerView()
        initLayoutChooseDate()

        if (!checkNotificationAccessEnabled())
            openDialogRequestPermissions(true)
        else {
            coroutineScope.launch {
                TransactionFlowManager.transactionFlow.collectLatest { transaction ->
                    addNewTransaction(transaction)
                }
            }
        }

        ringtonePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data = result.data
                    val selectedRingtoneUri =
                        data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

                    Log.d("TAG", "Selected Ringtone URI: $selectedRingtoneUri")

                    // Save the ringtone URI to SharedPreferences
                    if (selectedRingtoneUri != null)
                        SharedPreferencesManager.saveNotificationSound(selectedRingtoneUri.toString())
                }
            }
    }

    override fun onResume() {
        super.onResume()

        if (selectedDay == today && this::transactionAdapter.isInitialized)
            getTransactionFromDateAndDisplay()

        binding.recyclerView.scrollToPosition(0)
        binding.tvRequestNotificationAccessPermission.visibility =
            if (checkNotificationAccessEnabled()) View.GONE else View.VISIBLE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save selectedDay value into Bundle
        outState.putLong("selectedDay", selectedDay)
    }

    private fun initNotificationSwitch() {
        MyNotificationListenerService.isNotificationListenerEnabled =
            SharedPreferencesManager.isNotificationListenerEnabled()

        binding.switchNotification.let {
            it.isChecked = MyNotificationListenerService.isNotificationListenerEnabled
            it.setOnCheckedChangeListener { _, isChecked ->
                MyNotificationListenerService.isNotificationListenerEnabled = isChecked
                SharedPreferencesManager.saveNotificationListenerEnabled(isChecked)

                makeToast(
                    if (isChecked) "Đã bật tính năng đọc thông báo chuyển khoản" else "Đã tắt tính năng đọc thông báo chuyển khoản",
                    false
                )
            }
        }

        binding.btnSetting.setOnClickListener {
            openSettingDialog()
        }
        binding.btnSetting.addClickAnimation()
    }

    private fun initTVRequestNotificationAccessPermission() {
        binding.tvRequestNotificationAccessPermission.setOnClickListener {
            requestNotificationAccess()
        }
    }

    private fun initRecyclerView() {
        transactionAdapter = TransactionAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = transactionAdapter
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            coroutineScope.launch {
                getTransactionFromDateAndDisplay()
                withContext(Dispatchers.Main) {
                    binding.swipeRefreshLayout.isRefreshing =
                        false // Turn off refresh when completed
                }
            }
        }

        getTransactionFromDateAndDisplay()
    }

    private fun initLayoutChooseDate() {
        binding.tvDate.let {
            it.text = DateUtils.formatDate(today)
            it.setOnClickListener { openDatePickerDialog() }
        }

        binding.btnNext.let {
            it.isEnabled = false
            it.setOnClickListener {
                selectedDay += TimeUnit.DAYS.toMillis(1)
                binding.tvDate.text = DateUtils.formatDate(selectedDay)
                getTransactionFromDateAndDisplay()

                updateLayoutChooseDateButtonVisibility()
            }
        }

        binding.btnPrev.setOnClickListener {
            selectedDay -= TimeUnit.DAYS.toMillis(1)
            binding.tvDate.text = DateUtils.formatDate(selectedDay)
            getTransactionFromDateAndDisplay()

            updateLayoutChooseDateButtonVisibility()
        }

        binding.btnNext.addClickAnimation()
        binding.btnPrev.addClickAnimation()
    }

    private fun updateLayoutChooseDateButtonVisibility() {
        if (selectedDay == today)
            binding.btnNext.isEnabled = false
        else if (selectedDay < today)
            binding.btnNext.isEnabled = true
    }

    private fun getTransactionFromDateAndDisplay() {
        coroutineScope.launch {

            val transactions = transactionDao.getTransactionsForToday(
                selectedDay,                                      // Start of the day
                selectedDay + TimeUnit.DAYS.toMillis(1)    // End of the day
            )

            transactionList = transactions.toMutableList()

            withContext(Dispatchers.Main) {
                transactionAdapter.submitList(transactions)
                updateTotalAmountTaskBar()
            }
        }
    }

    private fun addNewTransaction(newTransaction: Transaction) {
        coroutineScope.launch {
            if (selectedDay == today) {
                transactionList.add(0, newTransaction)
                val newList = ArrayList(transactionList)

                withContext(Dispatchers.Main) {
                    transactionAdapter.submitList(newList) {
                        binding.recyclerView.post {
                            binding.recyclerView.scrollToPosition(0)
                        }
                    }
                    updateTotalAmountTaskBar()
                }
            }
        }
    }

    private fun updateTotalAmountTaskBar() {
        var totalAmountReceived = 0L
        var totalAmountSent = 0L

        for (transaction in transactionList) {
            if (transaction.amount > 0)
                totalAmountReceived += transaction.amount
            else totalAmountSent += transaction.amount
        }

        binding.tvTotalAmountReceived.text =
            AppUtils.formatCurrency(totalAmountReceived)
        binding.tvTotalAmountSent.text =
            AppUtils.formatCurrency(totalAmountSent)
    }

    private fun openDialogRequestPermissions(autoClose: Boolean) {
        if (!this::dialogRequestPermissions.isInitialized)
            dialogRequestPermissions = RequestPermissionsDialogFragment.newInstance(autoClose)
        dialogRequestPermissions.show(supportFragmentManager, "RequestPermissionsDialogFragment")
    }

    private fun requestNotificationAccess() {
        makeToast("Tìm và chọn ứng dụng \"Đọc thông báo chuyển khoản\"", true)
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        startActivity(intent)
    }

    private fun checkNotificationAccessEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners != null && enabledListeners.contains(packageName)
    }

    private fun openDatePickerDialog() {
        val fragment = supportFragmentManager.findFragmentByTag("DatePickerDialogFragment")

        if (fragment != null && fragment is DatePickerDialogFragment) {
            // If fragment has been added
            if (fragment.isVisible) {
                fragment.dismiss() // Make sure the fragment is deleted before displaying it again
                fragment.show(supportFragmentManager, "DatePickerDialogFragment")
            }
        } else {
            // If the fragment does not exist, create a new one and display it
            val dialog = DatePickerDialogFragment.newInstance(today, selectedDay, this)
            dialog.show(supportFragmentManager, "DatePickerDialogFragment")
        }
    }

    override fun onDateChanged(
        dialog: AlertDialog
    ): DatePicker.OnDateChangedListener {
        return DatePicker.OnDateChangedListener { _, year, month, day ->
            if (dialog.isShowing) {
                val calendar = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // Update selectedDay value
                selectedDay = calendar.timeInMillis

                // Update UI
                binding.tvDate.text = DateUtils.formatDate(selectedDay)
                updateLayoutChooseDateButtonVisibility()
                getTransactionFromDateAndDisplay()

                dialog.dismiss()
            }
        }
    }

    private fun openSettingDialog() {
        val fragment = supportFragmentManager.findFragmentByTag("SettingDialogFragment")

        if (fragment != null && fragment is SettingDialogFragment) {
            // If fragment has been added
            if (fragment.isVisible) {
                fragment.dismiss() // Make sure the fragment is deleted before displaying it again
                fragment.show(supportFragmentManager, "SettingDialogFragment")
            }
        } else {
            // If the fragment does not exist, create a new one and display it
            val dialogSetting = SettingDialogFragment.newInstance(this)
            dialogSetting.show(supportFragmentManager, "SettingDialogFragment")
        }
    }

    override fun onTvNotificationSoundClicked(): View.OnClickListener {
        return View.OnClickListener { openNotificationSoundPicker() }
    }

    private fun openNotificationSoundPicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
        }
        ringtonePickerLauncher.launch(intent)
    }

    private fun makeToast(msg: String, isLongToast: Boolean) {
        Toast.makeText(this, msg, if (isLongToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
            .show()
    }
}