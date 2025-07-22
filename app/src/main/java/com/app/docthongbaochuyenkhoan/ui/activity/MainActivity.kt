package com.app.docthongbaochuyenkhoan.ui.activity

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.docthongbaochuyenkhoan.MyCustomApplication
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.controller.SharedPreferencesManager
import com.app.docthongbaochuyenkhoan.databinding.ActivityMainBinding
import com.app.docthongbaochuyenkhoan.databinding.DialogTtsHelperBinding
import com.app.docthongbaochuyenkhoan.flow.TransactionFlowManager
import com.app.docthongbaochuyenkhoan.model.Transaction
import com.app.docthongbaochuyenkhoan.model.database.AppDatabase
import com.app.docthongbaochuyenkhoan.model.database.TransactionDao
import com.app.docthongbaochuyenkhoan.ui.adapter.TransactionAdapter
import com.app.docthongbaochuyenkhoan.ui.dialog.DatePickerDialogFragment
import com.app.docthongbaochuyenkhoan.ui.dialog.RequestPermissionsDialogFragment
import com.app.docthongbaochuyenkhoan.ui.dialog.SettingDialogFragment
import com.app.docthongbaochuyenkhoan.utils.AppUtils
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.addClickAnimation
import com.app.docthongbaochuyenkhoan.utils.DateUtils
import com.app.docthongbaochuyenkhoan.utils.MediaPlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), SettingDialogFragment.SettingDialogListener,
    DatePickerDialogFragment.DatePickerDialogListener, TransactionAdapter.AdapterListener,
    TextToSpeech.OnInitListener {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private lateinit var binding: ActivityMainBinding
    private lateinit var taskbarManager: TaskbarManager
    private lateinit var dialogRequestPermissions: RequestPermissionsDialogFragment
    private lateinit var transactionDao: TransactionDao
    private lateinit var transactionAdapter: TransactionAdapter
    private var transactionList = mutableListOf<Transaction>()
    private var today = 0L
    private var selectedDay = 0L
    private lateinit var ringtonePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_taskbar)

        if (!SharedPreferencesManager.isInitialized()) SharedPreferencesManager.init(this)

        if (MyCustomApplication.isSamsungDevice() && !SharedPreferencesManager.getNotShowAgainDialogSettingHelper())
            openDialogTTSHelper()

        AppCompatDelegate.setDefaultNightMode(
            if (SharedPreferencesManager.isNightModeEnabled()) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        taskbarManager = TaskbarManager(this)

        transactionDao = AppDatabase.getDatabase(this).transactionDao()
        textToSpeech = TextToSpeech(this, this)

        today = DateUtils.getStartTimeOfToday()

        selectedDay = savedInstanceState?.getLong("selectedDay", today) ?: today

        initTVRequestNotificationAccessPermission()
        initRecyclerView()
        initLayoutChooseDate()
        initLayoutTotalTransactions()

        if (!checkNotificationAccessEnabled()) openDialogRequestPermissions(true)
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
                    if (selectedRingtoneUri != null) SharedPreferencesManager.saveNotificationSound(
                        selectedRingtoneUri.toString()
                    )
                }
            }
    }

    override fun onResume() {
        super.onResume()

        binding.recyclerView.scrollToPosition(0)
        binding.tvRequestNotificationAccessPermission.visibility =
            if (checkNotificationAccessEnabled()) View.GONE else View.VISIBLE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Save selectedDay value into Bundle
        outState.putLong("selectedDay", selectedDay)

        super.onSaveInstanceState(outState)
    }

    private fun initTVRequestNotificationAccessPermission() {
        binding.tvRequestNotificationAccessPermission.setOnClickListener {
            requestNotificationAccess()
        }
    }

    private fun initRecyclerView() {
        transactionAdapter = TransactionAdapter(this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = transactionAdapter
        }

        binding.recyclerView.setHasFixedSize(true)

        binding.recyclerView.itemAnimator = object : DefaultItemAnimator() {
            override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
                holder?.itemView?.alpha = 0f
                holder?.itemView?.animate()?.alpha(1f)?.setDuration(300)?.start()
                return super.animateAdd(holder)
            }
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
            it.text = DateUtils.formatDate(if (selectedDay > 0) selectedDay else today)
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

        updateLayoutChooseDateButtonVisibility()
    }

    private fun initLayoutTotalTransactions() {
        binding.btnStatistic.setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            startActivity(intent)
        }
        binding.btnStatistic.addClickAnimation()
    }

    private fun updateLayoutChooseDateButtonVisibility() {
        if (selectedDay == today) binding.btnNext.isEnabled = false
        else if (selectedDay < today) binding.btnNext.isEnabled = true
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
                animateTransitionRecyclerView()
                updateTotalAmountTaskBar()

                if (transactions.isEmpty()) {
                    binding.tvNotifyNoTransaction.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.tvNotifyNoTransaction.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
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

    private fun animateTransitionRecyclerView() {
        val animation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_slide)
        binding.recyclerView.layoutAnimation = animation
    }

    private fun updateTotalAmountTaskBar() {
        var totalAmountReceived = 0L
        var totalAmountSent = 0L

        for (transaction in transactionList) {
            if (transaction.amount > 0) totalAmountReceived += transaction.amount
            else totalAmountSent += transaction.amount
        }

        binding.tvTotalTransactions.text = "Tổng giao dịch trong ngày : " + transactionList.size
        binding.tvTotalAmountReceived.text = AppUtils.formatCurrency(totalAmountReceived)
        binding.tvTotalAmountSent.text = AppUtils.formatCurrency(totalAmountSent)
    }

    private fun openDialogRequestPermissions(autoClose: Boolean) {
        if (!this::dialogRequestPermissions.isInitialized) dialogRequestPermissions =
            RequestPermissionsDialogFragment.newInstance(autoClose)
        dialogRequestPermissions.show(supportFragmentManager, "RequestPermissionsDialogFragment")
    }

    private fun requestNotificationAccess() {
        makeToast("Tìm và chọn ứng dụng \"Đọc thông báo chuyển khoản\"", true)
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        startActivity(intent)
    }

    private fun checkNotificationAccessEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
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

    override fun onBtnSpeakClicked(transaction: Transaction): View.OnClickListener {
        return View.OnClickListener {
            val notification = StringBuilder(transaction.bank.speakName)

            if (transaction.amount > 0) {
                val notificationContent = SharedPreferencesManager.getNotificationContentReceived()
                notification.append(" $notificationContent")
                notification.append(" ${AppUtils.formatCurrency(transaction.amount)}")
            } else {
                val notificationContent = SharedPreferencesManager.getNotificationContentSent()
                notification.append(" $notificationContent")
                notification.append(" ${AppUtils.formatCurrency(-transaction.amount)}")
            }

            if (!textToSpeech.isSpeaking) {
                MediaPlayerUtils.playMedia(this, null)
                textToSpeech.speak(notification.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    private fun openDialogTTSHelper() {
        val builder = AlertDialog.Builder(this, R.style.CustomDialogTheme)
        val dialogBinding = DialogTtsHelperBinding.inflate(layoutInflater)
        builder.setView(dialogBinding.root)

        val dialog = builder.create()
        dialog.let {
            dialog.window?.setGravity(Gravity.CENTER)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation
        }

        dialogBinding.btnNotShowAgain.isChecked =
            SharedPreferencesManager.getNotShowAgainDialogSettingHelper()
        dialogBinding.btnNotShowAgain.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferencesManager.saveNotShowAgainDialogSettingHelper(isChecked)
        }

        dialogBinding.iBtnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.iBtnClose.addClickAnimation()
        dialogBinding.btnClose.addClickAnimation()
        dialog.show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale("vi")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.tvDate.setOnClickListener(null)
        binding.btnNext.setOnClickListener(null)
        binding.btnPrev.setOnClickListener(null)

        taskbarManager.release()
    }

    private fun makeToast(msg: String, isLongToast: Boolean) {
        Toast.makeText(this, msg, if (isLongToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
}