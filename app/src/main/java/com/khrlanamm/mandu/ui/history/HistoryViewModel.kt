package com.khrlanamm.mandu.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khrlanamm.mandu.ui.history.data.HistoryRepository
import com.khrlanamm.mandu.ui.history.data.Report
import kotlinx.coroutines.launch

data class Stats(val count: Int = 0, val percentage: Float = 0f)

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val _allReports = MutableLiveData<List<Report>>()

    private val _filteredReports = MutableLiveData<List<Report>>()
    val filteredReports: LiveData<List<Report>> = _filteredReports

    private val _reportedStats = MutableLiveData<Stats>()
    val reportedStats: LiveData<Stats> = _reportedStats

    private val _handledStats = MutableLiveData<Stats>()
    val handledStats: LiveData<Stats> = _handledStats

    // --- State untuk Date Filter ---
    private val _startDate = MutableLiveData<Long?>()
    val startDate: LiveData<Long?> = _startDate

    private val _endDate = MutableLiveData<Long?>()
    val endDate: LiveData<Long?> = _endDate
    // -----------------------------------------

    private var currentFilter: String = "Semua Laporan"

    fun loadReports() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.getHistoryReports()
                result.onSuccess { freshReports ->
                    if (freshReports.isEmpty()) {
                        _toastMessage.postValue("Belum ada Laporan")
                    }
                    // Urutkan laporan berdasarkan timestamp, yang terbaru di atas
                    val sortedReports = freshReports.sortedByDescending { it.timestamp }
                    _allReports.postValue(sortedReports)
                    // Langsung kirim data baru ke fungsi filter untuk mengatasi masalah initial load
                    applyAllFilters(reportsToFilter = sortedReports)
                }.onFailure {
                    _toastMessage.postValue("Gagal memuat data: ${it.message}")
                }
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Fungsi baru untuk mengatur rentang tanggal dan memicu pemfilteran ulang.
     */
    fun setDateRange(start: Long?, end: Long?) {
        _startDate.value = start
        _endDate.value = end // PERBAIKAN: Tambahkan baris ini untuk menyimpan tanggal akhir
        applyAllFilters()
    }

    /**
     * Memperbarui filter status dan memicu pemfilteran ulang.
     */
    fun filterReports(statusFilter: String) {
        this.currentFilter = statusFilter
        applyAllFilters()
    }

    /**
     * Fungsi ini sekarang bisa menerima daftar laporan secara langsung
     * untuk menghindari race condition saat pertama kali load.
     */
    private fun applyAllFilters(reportsToFilter: List<Report>? = null) {
        // Jika `reportsToFilter` diberikan (dari loadReports), gunakan itu.
        // Jika tidak (dari filter biasa), gunakan data yang sudah tersimpan di _allReports.
        val allCurrentReports = reportsToFilter ?: _allReports.value ?: emptyList()
        var baseFilteredList = allCurrentReports

        // 1. Terapkan filter rentang tanggal TERLEBIH DAHULU
        val start = _startDate.value
        val end = _endDate.value
        if (start != null && end != null) {
            // Tambahkan 1 hari (dalam milidetik) ke tanggal akhir agar inklusif
            val inclusiveEndDate = end + 86400000
            baseFilteredList = baseFilteredList.filter { report ->
                val reportTime = report.timestamp.toDate().time
                reportTime >= start && reportTime < inclusiveEndDate
            }
        }

        // 2. HITUNG ULANG STATISTIK berdasarkan daftar yang sudah difilter tanggal.
        calculateStats(baseFilteredList)

        // 3. Terapkan filter status pada daftar yang sudah difilter tanggal.
        val finalFilteredList = when (currentFilter) {
            "Terlapor" -> baseFilteredList.filter { it.status.equals("terlapor", ignoreCase = true) }
            "Ditangani" -> baseFilteredList.filter { it.status.equals("ditangani", ignoreCase = true) }
            else -> baseFilteredList // "Semua Laporan"
        }

        _filteredReports.postValue(finalFilteredList)

        if (allCurrentReports.isNotEmpty() && finalFilteredList.isEmpty()) {
            _toastMessage.postValue("Tidak ada laporan yang cocok dengan filter")
        }
    }

    private fun calculateStats(reports: List<Report>) {
        if (reports.isEmpty()) {
            _reportedStats.value = Stats(0, 0f)
            _handledStats.value = Stats(0, 0f)
            return
        }
        val total = reports.size
        val reportedCount = reports.count { it.status.equals("terlapor", ignoreCase = true) }
        val handledCount = reports.count { it.status.equals("ditangani", ignoreCase = true) }
        val reportedPercentage = if (total > 0) (reportedCount * 100f / total) else 0f
        val handledPercentage = if (total > 0) (handledCount * 100f / total) else 0f
        _reportedStats.value = Stats(reportedCount, reportedPercentage)
        _handledStats.value = Stats(handledCount, handledPercentage)
    }

    fun onToastShown() {
        _toastMessage.value = null
    }
}
