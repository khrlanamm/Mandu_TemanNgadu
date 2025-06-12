package com.khrlanamm.mandu.ui.history

import android.content.Intent
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
import com.khrlanamm.mandu.ui.detail.DetailActivity
import com.khrlanamm.mandu.ui.history.data.HistoryRepository
import com.khrlanamm.mandu.ui.history.data.Report
import java.util.Locale

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
        setupSwipeToRefresh()
        observeViewModel()

        // Pemanggilan data dipindahkan ke onResume()
    }

    /**
     * onResume akan dipanggil setiap kali activity ini ditampilkan.
     * Ini mencakup saat pertama kali dibuka dan saat kembali dari activity lain.
     */
    override fun onResume() {
        super.onResume()
        viewModel.loadReports()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.action_history)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter { report ->
            navigateToDetail(report)
        }
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun navigateToDetail(report: Report) {
        val intent = Intent(this, DetailActivity::class.java)
        intent.putExtra(DetailActivity.EXTRA_REPORT, report)
        startActivity(intent)
    }

    private fun setupFilterDropdown() {
        val filterOptions = listOf("Semua Laporan", "Terlapor", "Ditangani")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, filterOptions)
        binding.actvFilter.setAdapter(adapter)

        binding.actvFilter.setOnItemClickListener { _, _, position, _ ->
            val selectedFilter = filterOptions[position]
            viewModel.filterReports(selectedFilter)
        }
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadReports()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading

            if (isLoading && historyAdapter.currentList.isEmpty()) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }
        }

        viewModel.toastMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.onToastShown()
            }
        }

        viewModel.filteredReports.observe(this) { reports ->
            historyAdapter.submitList(reports)
        }

        viewModel.reportedStats.observe(this) { stats ->
            val formattedPercentage = String.format(Locale.US, "%.1f", stats.percentage)
            binding.tvStatsReported.text = "Terlapor : ${stats.count} (${formattedPercentage}%)"
            binding.progressReported.progress = stats.percentage.toInt()
        }

        viewModel.handledStats.observe(this) { stats ->
            val formattedPercentage = String.format(Locale.US, "%.1f", stats.percentage)
            binding.tvStatsHandled.text = "Ditangani : ${stats.count} (${formattedPercentage}%)"
            binding.progressHandled.progress = stats.percentage.toInt()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
