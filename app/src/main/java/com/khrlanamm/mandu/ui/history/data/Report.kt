package com.khrlanamm.mandu.ui.history.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class Report(
    val id: String = "",
    val deskripsi: String = "",
    val frekuensi: String = "",
    val lokasi: String = "",
    val nomorWhatsapp: String = "",
    val peran: String = "",
    val status: String = "",
    val tanggalBullying: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val urlBukti: String? = null,
    val userId: String = ""
) : Parcelable
