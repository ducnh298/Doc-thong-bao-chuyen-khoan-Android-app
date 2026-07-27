package com.app.docthongbaochuyenkhoan.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.controller.SharedPreferencesManager
import com.app.docthongbaochuyenkhoan.databinding.DialogBankFilterBinding
import com.app.docthongbaochuyenkhoan.model.Bank
import com.app.docthongbaochuyenkhoan.ui.adapter.BankFilterAdapter
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.addClickAnimation

class BankFilterDialogFragment : DialogFragment() {

    companion object {
        fun newInstance() = BankFilterDialogFragment()

        private val WALLETS = setOf(Bank.MOMO, Bank.VIETTELMONEY, Bank.ZALOPAY)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogBankFilterBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
        builder.setView(binding.root)

        val allBanks = Bank.entries.filter { it != Bank.UNKNOWN }
        val banks = allBanks.filter { it !in WALLETS }
        val wallets = allBanks.filter { it in WALLETS }

        val bankAdapter = BankFilterAdapter(banks)
        val walletAdapter = BankFilterAdapter(wallets)

        binding.rvBanks.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvBanks.adapter = bankAdapter
        binding.rvWallets.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvWallets.adapter = walletAdapter

        var suppressSwitchAllEvent = false

        fun syncSwitchAll() {
            suppressSwitchAllEvent = true
            binding.switchAll.isChecked = allBanks.all { SharedPreferencesManager.isBankEnabled(it) }
            suppressSwitchAllEvent = false
        }

        syncSwitchAll()

        binding.switchAll.setOnCheckedChangeListener { _, isChecked ->
            if (suppressSwitchAllEvent) return@setOnCheckedChangeListener
            bankAdapter.setAllEnabled(isChecked)
            walletAdapter.setAllEnabled(isChecked)
        }

        bankAdapter.onBankToggled = { syncSwitchAll() }
        walletAdapter.onBankToggled = { syncSwitchAll() }

        val dialog = builder.create()
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnClose.addClickAnimation()

        return dialog
    }
}
