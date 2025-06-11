package com.khrlanamm.mandu.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khrlanamm.mandu.ui.history.data.HistoryRepository
import com.khrlanamm.mandu.ui.history.data.Report
import kotlinx.coroutines.launch

// Mengubah tipe data 'percentage' menjadi Float untuk presisi desimal
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

    // Fungsi untuk memuat data awal
    fun loadReports() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.getHistoryReports()
            result.onSuccess { reports ->
                if (reports.isEmpty()) {
                    _toastMessage.value = "Belum ada Laporan"
                }
                _allReports.value = reports
                calculateStats(reports)
                filterReports("Semua Laporan") // Tampilkan semua laporan secara default
            }.onFailure {
                _toastMessage.value = "Gagal memuat data: ${it.message}"
            }
            _isLoading.value = false
        }
    }

    // Fungsi untuk memfilter daftar laporan
    fun filterReports(statusFilter: String) {
        val all = _allReports.value ?: emptyList()
        val filteredList = when (statusFilter) {
            "Terlapor" -> all.filter { it.status.equals("terlapor", ignoreCase = true) }
            "Ditangani" -> all.filter { it.status.equals("ditangani", ignoreCase = true) }
            else -> all // "Semua Laporan"
        }

        _filteredReports.value = filteredList

        if (all.isNotEmpty() && filteredList.isEmpty()) {
            _toastMessage.value = "Tidak ada Riwayat Laporan dengan status ${statusFilter.uppercase()}"
        }
    }

    // Fungsi untuk menghitung statistik dengan presisi float
    private fun calculateStats(reports: List<Report>) {
        if (reports.isEmpty()) {
            _reportedStats.value = Stats(0, 0f)
            _handledStats.value = Stats(0, 0f)
            return
        }

        val total = reports.size
        val reportedCount = reports.count { it.status.equals("terlapor", ignoreCase = true) }
        val handledCount = reports.count { it.status.equals("ditangani", ignoreCase = true) }

        // Kalkulasi diubah menjadi float untuk mendapatkan nilai desimal
        val reportedPercentage = if (total > 0) (reportedCount * 100f / total) else 0f
        val handledPercentage = if (total > 0) (handledCount * 100f / total) else 0f

        _reportedStats.value = Stats(reportedCount, reportedPercentage)
        _handledStats.value = Stats(handledCount, handledPercentage)
    }

    // Fungsi untuk membersihkan pesan toast setelah ditampilkan
    fun onToastShown() {
        _toastMessage.value = null
    }
}
