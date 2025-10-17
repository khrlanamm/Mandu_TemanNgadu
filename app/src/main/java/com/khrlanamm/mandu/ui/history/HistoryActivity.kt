package com.khrlanamm.mandu.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.khrlanamm.mandu.R
import com.khrlanamm.mandu.databinding.ActivityHistoryBinding
import com.khrlanamm.mandu.ui.detail.DetailActivity
import com.khrlanamm.mandu.ui.history.data.HistoryRepository
import com.khrlanamm.mandu.ui.history.data.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyAdapter: HistoryAdapter

    private val viewModel: HistoryViewModel by viewModels {
        ViewModelFactory(HistoryRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            (binding.toolbar.parent as? View)?.setPadding(insets.left, insets.top, insets.right, 0)
            binding.swipeRefreshLayout.setPadding(0, 0, 0, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        setupToolbar()
        setupRecyclerView()
        setupDatePicker()
        setupFilterDropdown()
        setupSwipeToRefresh()
        observeViewModel()
    }

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

    private fun setupDatePicker() {
        binding.tietDateRange.setOnClickListener {
            // Mengambil nilai tanggal saat ini dari ViewModel untuk preselekasi
            val currentStartDate = viewModel.startDate.value
            val currentEndDate = viewModel.endDate.value
            val selection = if (currentStartDate != null && currentEndDate != null) {
                Pair(currentStartDate, currentEndDate)
            } else {
                // Default ke hari ini jika tidak ada yang dipilih
                Pair(
                    MaterialDatePicker.todayInUtcMilliseconds(),
                    MaterialDatePicker.todayInUtcMilliseconds()
                )
            }

            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(getString(R.string.title_date_range_picker))
                .setSelection(selection)
                .build()

            picker.show(supportFragmentManager, "DATE_RANGE_PICKER")

            // Listener untuk tombol "OK"
            picker.addOnPositiveButtonClickListener { dateSelection ->
                val startDate = dateSelection.first
                val endDate = dateSelection.second
                viewModel.setDateRange(startDate, endDate) // Kirim ke ViewModel

                // Format tanggal untuk ditampilkan di UI
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val formattedDate = "${sdf.format(Date(startDate))} - ${sdf.format(Date(endDate))}"
                binding.tietDateRange.setText(formattedDate)
                binding.btnClearDateFilter.visibility = View.VISIBLE
            }
        }

        // Listener untuk tombol hapus filter tanggal
        binding.btnClearDateFilter.setOnClickListener {
            viewModel.setDateRange(null, null) // Hapus filter di ViewModel
            binding.tietDateRange.setText(R.string.all_time)
            it.visibility = View.GONE
        }
    }

    private fun setupFilterDropdown() {
        val filterOptions = resources.getStringArray(R.array.history_filter_options).toList()
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
            // Hanya tampilkan progress bar utama jika daftar kosong
            if (isLoading && historyAdapter.currentList.isEmpty()) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }
            // Swipe refresh indicator diatur secara terpisah
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
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
            binding.tvStatsReported.text =
                getString(R.string.reported_stats, stats.count, formattedPercentage)
            binding.progressReported.progress = stats.percentage.toInt()
        }

        viewModel.handledStats.observe(this) { stats ->
            val formattedPercentage = String.format(Locale.US, "%.1f", stats.percentage)
            binding.tvStatsHandled.text =
                getString(R.string.handled_stats, stats.count, formattedPercentage)
            binding.progressHandled.progress = stats.percentage.toInt()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
