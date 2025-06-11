package com.khrlanamm.mandu.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.khrlanamm.mandu.R
import com.khrlanamm.mandu.databinding.ItemHistoryBinding
import com.khrlanamm.mandu.ui.history.data.Report

class HistoryAdapter : ListAdapter<Report, HistoryAdapter.HistoryViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val report = getItem(position)
        holder.bind(report)
    }

    class HistoryViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(report: Report) {
            val context = binding.root.context
            binding.apply {
                // Memuat gambar dengan Glide, jika null pakai placeholder
                Glide.with(context)
                    .load(report.urlBukti)
                    .placeholder(R.drawable.placeholder_image) // Pastikan drawable ini ada
                    .error(R.drawable.placeholder_image)
                    .into(ivReportImage)

                // Mengisi data teks
                tvDate.text = "Tanggal: ${report.tanggalBullying}"
                tvLocation.text = "Lokasi: ${report.lokasi}"
                tvDescription.text = report.deskripsi

                // Mengatur status (warna dan teks)
                when (report.status.lowercase()) {
                    "terlapor" -> {
                        tvStatus.text = "TERLAPOR"
                        tvStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_status_terlapor)
                    }
                    "ditangani" -> {
                        tvStatus.text = "DITANGANI"
                        tvStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_status_ditangani)
                    }
                    else -> tvStatus.visibility = ViewGroup.GONE
                }

                // Mengatur peran (warna dan teks)
                when (report.peran.lowercase()) {
                    "korban" -> {
                        tvRole.text = "KORBAN"
                        tvRole.background = ContextCompat.getDrawable(context, R.drawable.bg_peran_korban)
                    }
                    "saksi" -> {
                        tvRole.text = "SAKSI"
                        tvRole.background = ContextCompat.getDrawable(context, R.drawable.bg_peran_saksi)
                    }
                    else -> tvRole.visibility = ViewGroup.GONE
                }
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Report>() {
            override fun areItemsTheSame(oldItem: Report, newItem: Report): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Report, newItem: Report): Boolean {
                return oldItem == newItem
            }
        }
    }
}
