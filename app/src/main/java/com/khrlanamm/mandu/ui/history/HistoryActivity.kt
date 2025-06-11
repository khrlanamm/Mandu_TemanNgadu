package com.khrlanamm.mandu.ui.history

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import com.khrlanamm.mandu.R
import com.khrlanamm.mandu.databinding.ActivityHistoryBinding
import com.khrlanamm.mandu.ui.history.data.HistoryRepository

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyAdapter: HistoryAdapter

    private val viewModel: HistoryViewModel by viewModels {
        ViewModelFactory(HistoryRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFilterDropdown()
        observeViewModel()

        // Memuat data saat activity dibuat
        if (savedInstanceState == null) {
            viewModel.loadReports()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.action_history)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun setupFilterDropdown() {
        val filterOptions = listOf("Semua Laporan", "Terlapor", "Ditangani")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, filterOptions)
        binding.actvFilter.setAdapter(adapter)

        // Listener untuk saat item filter dipilih
        binding.actvFilter.setOnItemClickListener { _, _, position, _ ->
            val selectedFilter = filterOptions[position]
            viewModel.filterReports(selectedFilter)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.toastMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.onToastShown() // Reset pesan agar tidak ditampilkan lagi
            }
        }

        viewModel.filteredReports.observe(this) { reports ->
            historyAdapter.submitList(reports)
        }

        viewModel.reportedStats.observe(this) { stats ->
            binding.tvStatsReported.text = "Terlapor : ${stats.count} (${stats.percentage}%)"
            binding.progressReported.progress = stats.percentage
        }

        viewModel.handledStats.observe(this) { stats ->
            binding.tvStatsHandled.text = "Ditangani : ${stats.count} (${stats.percentage}%)"
            binding.progressHandled.progress = stats.percentage
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
