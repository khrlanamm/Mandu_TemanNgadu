package com.khrlanamm.mandu.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khrlanamm.mandu.ui.history.data.HistoryRepository
import com.khrlanamm.mandu.ui.history.data.Report
import kotlinx.coroutines.launch

// data class Stats tidak perlu diubah
data class Stats(val count: Int = 0, val percentage: Float = 0f)

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    // _allReports tetap menyimpan data mentah dari Firestore
    private val _allReports = MutableLiveData<List<Report>>()

    private val _filteredReports = MutableLiveData<List<Report>>()
    val filteredReports: LiveData<List<Report>> = _filteredReports

    private val _reportedStats = MutableLiveData<Stats>()
    val reportedStats: LiveData<Stats> = _reportedStats

    private val _handledStats = MutableLiveData<Stats>()
    val handledStats: LiveData<Stats> = _handledStats

    // Variabel untuk menyimpan status filter saat ini
    private var currentFilter: String = "Semua Laporan"

    /**
     * Memuat data dari repository.
     * Fungsi ini sekarang akan langsung menerapkan filter pada data yang baru diterima.
     */
    fun loadReports() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = repository.getHistoryReports()
                result.onSuccess { freshReports ->
                    if (freshReports.isEmpty()) {
                        _toastMessage.postValue("Belum ada Laporan")
                    }
                    _allReports.postValue(freshReports) // Update daftar lengkap
                    calculateStats(freshReports) // Hitung statistik dari data baru

                    // LANGSUNG TERAPKAN FILTER PADA DATA BARU, JANGAN MENUNGGU
                    val filteredList = applyFilter(freshReports, currentFilter)
                    _filteredReports.postValue(filteredList)

                }.onFailure {
                    _toastMessage.postValue("Gagal memuat data: ${it.message}")
                }
            } finally {
                // Pastikan loading selalu dihentikan
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Fungsi yang dipanggil saat pengguna mengubah pilihan filter di dropdown.
     */
    fun filterReports(statusFilter: String) {
        // Simpan status filter yang baru dipilih
        this.currentFilter = statusFilter

        val allCurrentReports = _allReports.value ?: emptyList()
        val filteredList = applyFilter(allCurrentReports, statusFilter)
        _filteredReports.value = filteredList

        if (allCurrentReports.isNotEmpty() && filteredList.isEmpty()) {
            _toastMessage.value = "Tidak ada Riwayat Laporan dengan status ${statusFilter.uppercase()}"
        }
    }

    /**
     * Helper function untuk memusatkan logika filter.
     * @param reports Daftar laporan yang akan difilter.
     * @param filter Kriteria filter ("Semua Laporan", "Terlapor", "Ditangani").
     * @return Daftar laporan yang sudah difilter.
     */
    private fun applyFilter(reports: List<Report>, filter: String): List<Report> {
        return when (filter) {
            "Terlapor" -> reports.filter { it.status.equals("terlapor", ignoreCase = true) }
            "Ditangani" -> reports.filter { it.status.equals("ditangani", ignoreCase = true) }
            else -> reports // "Semua Laporan"
        }
    }

    // Fungsi calculateStats dan onToastShown tidak perlu diubah
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
