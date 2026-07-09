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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.docthongbaochuyenkhoan.MyCustomApplication
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.controller.SharedPreferencesManager
import com.app.docthongbaochuyenkhoan.databinding.ActivityMainBinding
import com.app.docthongbaochuyenkhoan.databinding.DialogTtsHelperBinding
import com.app.docthongbaochuyenkhoan.model.Transaction
import com.app.docthongbaochuyenkhoan.model.database.AppDatabase
import com.app.docthongbaochuyenkhoan.ui.adapter.TransactionAdapter
import com.app.docthongbaochuyenkhoan.ui.dialog.DatePickerDialogFragment
import com.app.docthongbaochuyenkhoan.ui.dialog.RequestPermissionsDialogFragment
import com.app.docthongbaochuyenkhoan.ui.dialog.SettingDialogFragment
import com.app.docthongbaochuyenkhoan.utils.AppUtils
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.addClickAnimation
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.getAppVersionInfo
import com.app.docthongbaochuyenkhoan.utils.DateUtils
import com.app.docthongbaochuyenkhoan.utils.MediaPlayerUtils
import com.app.docthongbaochuyenkhoan.viewModel.MainViewModel
import com.app.docthongbaochuyenkhoan.viewModel.MainViewModelFactory
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale


class MainActivity : AppCompatActivity(), SettingDialogFragment.SettingDialogListener,
    DatePickerDialogFragment.DatePickerDialogListener, TransactionAdapter.AdapterListener,
    TextToSpeech.OnInitListener, TaskbarManager.TaskBarListener {

    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val MY_REQUEST_CODE = 290800
    private val DAYS_FOR_IMMEDIATE_UPDATE = 7

    // Lắng nghe trạng thái tải xuống bản cập nhật Flexible
    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> showUpdateReadySnackbar()
            else -> {}
        }
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var taskbarManager: TaskbarManager
    private lateinit var dialogRequestPermissions: RequestPermissionsDialogFragment
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var ringtonePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var textToSpeech: TextToSpeech
    private var needShowGuideStatisticfunction: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showWhatsNewIfUpdated()

        if (MyCustomApplication.isSamsungDevice() && !SharedPreferencesManager.getNotShowAgainDialogSettingHelper())
            openDialogTTSHelper()

        needShowGuideStatisticfunction = SharedPreferencesManager.getGuideStatisticfunction()
        if (needShowGuideStatisticfunction)
            showGuideStatisticfunction()

        AppCompatDelegate.setDefaultNightMode(
            if (SharedPreferencesManager.isNightModeEnabled()) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        taskbarManager = TaskbarManager(this)
        textToSpeech = TextToSpeech(this, this)

        val dao = AppDatabase.getDatabase(this).transactionDao()
        viewModel = ViewModelProvider(this, MainViewModelFactory(dao))[MainViewModel::class.java]

        initTVRequestNotificationAccessPermission()
        initRecyclerView()
        initLayoutChooseDate()
        initLayoutTotalTransactions()
        observeViewModel()

        if (!checkNotificationAccessEnabled()) openDialogRequestPermissions(true)

        ringtonePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    if (uri != null) SharedPreferencesManager.saveNotificationSound(uri.toString())
                }
            }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.transactions.collect { transactions ->
                        transactionAdapter.submitList(transactions) {
                            binding.recyclerView.scrollToPosition(0)
                        }
                        animateTransitionRecyclerView()
                        updateTotalAmountTaskBar(transactions)
                        binding.tvNotifyNoTransaction.visibility =
                            if (transactions.isEmpty()) View.VISIBLE else View.GONE
                        binding.recyclerView.visibility =
                            if (transactions.isEmpty()) View.GONE else View.VISIBLE
                    }
                }
                launch {
                    viewModel.selectedDay.collect { day ->
                        binding.tvDate.text = DateUtils.formatDate(day)
                        binding.btnNext.isEnabled = viewModel.canGoNext
                    }
                }
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.swipeRefreshLayout.isRefreshing = loading
                    }
                }
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.resetToToday()
        checkPendingFlexibleUpdate()
        checkForAppUpdate()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppForeground()
        binding.recyclerView.scrollToPosition(0)
        binding.tvRequestNotificationAccessPermission.visibility =
            if (checkNotificationAccessEnabled()) View.GONE else View.VISIBLE
        binding.tvAppHelper.visibility =
            if (SharedPreferencesManager.isNotificationListenerEnabled()) View.GONE else View.VISIBLE
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
            setHasFixedSize(true)
            itemAnimator = object : DefaultItemAnimator() {
                override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
                    holder?.itemView?.alpha = 0f
                    holder?.itemView?.animate()?.alpha(1f)?.setDuration(300)?.start()
                    return super.animateAdd(holder)
                }
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadTransactions()
        }
    }

    private fun initLayoutChooseDate() {
        binding.tvDate.setOnClickListener { openDatePickerDialog() }
        binding.btnNext.setOnClickListener { viewModel.nextDay() }
        binding.btnPrev.setOnClickListener { viewModel.prevDay() }
        binding.btnNext.addClickAnimation()
        binding.btnPrev.addClickAnimation()
    }

    private fun initLayoutTotalTransactions() {
        binding.btnStatistic.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
            if (needShowGuideStatisticfunction) {
                needShowGuideStatisticfunction = false
                showGuideStatisticfunction()
                SharedPreferencesManager.setGuideStatisticfunction(false)
            }
        }
        binding.btnStatistic.addClickAnimation()
    }

    private fun animateTransitionRecyclerView() {
        val animation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_slide)
        binding.recyclerView.layoutAnimation = animation
    }

    private fun updateTotalAmountTaskBar(transactions: List<Transaction>) {
        var totalAmountReceived = 0L
        var totalAmountSent = 0L
        for (transaction in transactions) {
            if (transaction.amount > 0) totalAmountReceived += transaction.amount
            else totalAmountSent += transaction.amount
        }
        binding.tvTotalTransactions.text = "Tổng giao dịch trong ngày : " + transactions.size
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
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    private fun showGuideStatisticfunction() {
        binding.guideStatisticfunction.visibility =
            if (needShowGuideStatisticfunction) View.VISIBLE else View.GONE
    }

    private fun checkNotificationAccessEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledListeners != null && enabledListeners.contains(packageName)
    }

    private fun openDatePickerDialog() {
        val fragment = supportFragmentManager.findFragmentByTag("DatePickerDialogFragment")
        if (fragment != null && fragment is DatePickerDialogFragment) {
            if (fragment.isVisible) {
                fragment.dismiss()
                fragment.show(supportFragmentManager, "DatePickerDialogFragment")
            }
        } else {
            DatePickerDialogFragment
                .newInstance(viewModel.today, viewModel.selectedDay.value, this)
                .show(supportFragmentManager, "DatePickerDialogFragment")
        }
    }

    override fun onDateChanged(dialog: AlertDialog): DatePicker.OnDateChangedListener {
        return DatePicker.OnDateChangedListener { _, year, month, day ->
            if (dialog.isShowing) {
                val calendar = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                viewModel.selectDay(calendar.timeInMillis)
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
                notification.append(" ${SharedPreferencesManager.getNotificationContentReceived()}")
                notification.append(" ${AppUtils.formatCurrency(transaction.amount)}")
            } else {
                notification.append(" ${SharedPreferencesManager.getNotificationContentSent()}")
                notification.append(" ${AppUtils.formatCurrency(-transaction.amount)}")
            }
            if (!textToSpeech.isSpeaking) {
                MediaPlayerUtils.playMedia(this, null)
                textToSpeech.speak(notification.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    private fun showWhatsNewIfUpdated() {
        val (versionName, versionCode) = getAppVersionInfo()
        val lastSeen = SharedPreferencesManager.getLastSeenVersionCode()
        if (versionCode <= lastSeen) return
        SharedPreferencesManager.saveLastSeenVersionCode(versionCode)

        val message = """
            🎨 Giao diện mới: dark/light mode, cài đặt được tổ chức lại
            🗑️ Thêm tính năng xoá toàn bộ dữ liệu giao dịch
            🔧 Sửa lỗi crash, đơ màn hình khi mở cài đặt
        """.trimIndent()

        AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setTitle("Có gì mới trong v$versionName")
            .setMessage(message)
            .setPositiveButton("Đã hiểu", null)
            .show()
    }

    private fun openDialogTTSHelper() {
        val builder = AlertDialog.Builder(this, R.style.CustomDialogTheme)
        val dialogBinding = DialogTtsHelperBinding.inflate(layoutInflater)
        builder.setView(dialogBinding.root)

        val dialog = builder.create()
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        dialogBinding.btnNotShowAgain.isChecked =
            SharedPreferencesManager.getNotShowAgainDialogSettingHelper()
        dialogBinding.btnNotShowAgain.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferencesManager.saveNotShowAgainDialogSettingHelper(isChecked)
        }
        dialogBinding.tvNotShowAgain.setOnClickListener { dialogBinding.btnNotShowAgain.performClick() }
        dialogBinding.iBtnClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.iBtnClose.addClickAnimation()
        dialogBinding.btnClose.addClickAnimation()
        dialog.show()
    }

    private fun checkForAppUpdate() {
        appUpdateManager.registerListener(installStateListener)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            when (info.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    // IMMEDIATE chỉ khi app đã lỗi thời >= DAYS_FOR_IMMEDIATE_UPDATE ngày
                    val staleDays = info.clientVersionStalenessDays() ?: 0
                    val updateType = when {
                        staleDays >= DAYS_FOR_IMMEDIATE_UPDATE &&
                                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                            AppUpdateType.IMMEDIATE
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ->
                            AppUpdateType.FLEXIBLE
                        else -> return@addOnSuccessListener
                    }
                    appUpdateManager.startUpdateFlowForResult(info, updateType, this, MY_REQUEST_CODE)
                }
                // Tiếp tục IMMEDIATE đang dở dang (app bị tắt giữa chừng)
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    appUpdateManager.startUpdateFlowForResult(
                        info, AppUpdateType.IMMEDIATE, this, MY_REQUEST_CODE
                    )
                }
                else -> {}
            }
        }
    }

    private fun checkPendingFlexibleUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                showUpdateReadySnackbar()
            }
        }
    }

    private fun showUpdateReadySnackbar() {
        Snackbar.make(
            binding.root,
            "Phiên bản mới đã tải xong!",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("Cài ngay") {
            appUpdateManager.completeUpdate()
        }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MY_REQUEST_CODE && resultCode != RESULT_OK) {
            Log.e("AppUpdate", "Update flow failed! Result code: $resultCode")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale("vi")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateListener)
        textToSpeech.stop()
        textToSpeech.shutdown()
        binding.tvDate.setOnClickListener(null)
        binding.btnNext.setOnClickListener(null)
        binding.btnPrev.setOnClickListener(null)
        taskbarManager.release()
    }

    private fun makeToast(msg: String, isLongToast: Boolean) {
        Toast.makeText(this, msg, if (isLongToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    override fun onSwitchNotificationClicked(isChecked: Boolean) {
        binding.tvAppHelper.visibility =
            if (SharedPreferencesManager.isNotificationListenerEnabled()) View.GONE else View.VISIBLE
    }
}
