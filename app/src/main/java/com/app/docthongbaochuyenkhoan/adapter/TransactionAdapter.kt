package com.app.docthongbaochuyenkhoan.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.docthongbaochuyenkhoan.databinding.ItemTransactionBinding
import com.app.docthongbaochuyenkhoan.model.Transaction
import com.app.docthongbaochuyenkhoan.utils.AppUtils
import com.app.docthongbaochuyenkhoan.utils.DateUtils

class TransactionAdapter :
    ListAdapter<Transaction, TransactionAdapter.ViewHolder>(TransactionDiffCallback()) {

    class ViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            binding.tvTitle.text = transaction.title
            binding.tvContent.text = transaction.content
            binding.imgLogo.setImageResource(transaction.bank?.logo ?: 0)

            binding.tvAmount.text = if (transaction.amount!! > 0)
                "+" + AppUtils.formatCurrency(transaction.amount)
            else AppUtils.formatCurrency(transaction.amount)

            binding.tvDateTime.text = DateUtils.formatDateTime(transaction.timestamp)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding =
            ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // DiffUtil to compare transactions
    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.content == newItem.content  // Compare using timestamp
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem  // Compare all content
        }
    }
}