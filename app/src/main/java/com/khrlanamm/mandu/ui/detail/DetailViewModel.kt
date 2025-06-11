package com.khrlanamm.mandu.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DetailViewModel(private val repository: DetailRepository) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _operationResult = MutableLiveData<Event<Result<String>>>()
    val operationResult: LiveData<Event<Result<String>>> = _operationResult

    fun updateStatus(reportId: String, newStatus: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.updateReportStatus(reportId, newStatus)
            if (result.isSuccess) {
                _operationResult.value = Event(Result.success("Update berhasil"))
            } else {
                _operationResult.value = Event(Result.failure(result.exceptionOrNull() ?: Exception("Gagal update status")))
            }
            _isLoading.value = false
        }
    }

    fun deleteReport(reportId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.deleteReport(reportId)
            if (result.isSuccess) {
                _operationResult.value = Event(Result.success("Delete berhasil"))
            } else {
                _operationResult.value = Event(Result.failure(result.exceptionOrNull() ?: Exception("Gagal menghapus laporan")))
            }
            _isLoading.value = false
        }
    }

    fun formatPhoneNumber(number: String): String {
        return when {
            number.startsWith("0") -> "+62" + number.substring(1)
            number.startsWith("62") -> "+$number"
            !number.startsWith("+") -> "+$number" // Jika tidak ada awalan, asumsikan nomor Indonesia
            else -> number
        }
    }

    fun formatTimestampToWIB(timestamp: Timestamp): String {
        return try {
            val sdf = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))
            sdf.timeZone = TimeZone.getTimeZone("Asia/Jakarta") // Set ke WIB (GMT+7)
            val formattedDate = sdf.format(timestamp.toDate())
            "$formattedDate WIB"
        } catch (e: Exception) {
            "Invalid Date"
        }
    }
}

open class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }
}
