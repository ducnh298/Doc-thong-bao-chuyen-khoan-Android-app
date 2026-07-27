package com.app.docthongbaochuyenkhoan.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.docthongbaochuyenkhoan.controller.SharedPreferencesManager
import com.app.docthongbaochuyenkhoan.databinding.ItemBankFilterBinding
import com.app.docthongbaochuyenkhoan.model.Bank

class BankFilterAdapter(
    private val banks: List<Bank>
) : RecyclerView.Adapter<BankFilterAdapter.ViewHolder>() {

    var onBankToggled: (() -> Unit)? = null

    inner class ViewHolder(val binding: ItemBankFilterBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBankFilterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bank = banks[position]
        val b = holder.binding

        b.ivLogo.setImageResource(bank.logo)
        b.tvName.text = bank.speakName

        b.switchEnabled.setOnCheckedChangeListener(null)
        b.switchEnabled.isChecked = SharedPreferencesManager.isBankEnabled(bank)
        b.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferencesManager.saveBankEnabled(bank, isChecked)
            onBankToggled?.invoke()
        }

        b.root.setOnClickListener { b.switchEnabled.toggle() }
    }

    override fun getItemCount() = banks.size

    fun setAllEnabled(isEnabled: Boolean) {
        banks.forEach { SharedPreferencesManager.saveBankEnabled(it, isEnabled) }
        notifyItemRangeChanged(0, banks.size)
    }

    fun areAllEnabled(): Boolean = banks.all { SharedPreferencesManager.isBankEnabled(it) }
}
