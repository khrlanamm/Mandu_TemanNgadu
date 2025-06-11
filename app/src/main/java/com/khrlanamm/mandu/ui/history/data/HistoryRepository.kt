package com.khrlanamm.mandu.ui.history.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.khrlanamm.mandu.data.AdminUID
import kotlinx.coroutines.tasks.await

class HistoryRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val reportsCollection = db.collection("reports")

    // Fungsi untuk mendapatkan riwayat laporan
    suspend fun getHistoryReports(): Result<List<Report>> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("Pengguna tidak terautentikasi."))
            }

            // Tentukan query berdasarkan peran pengguna (admin atau user biasa)
            val query = if (AdminUID.isAdmin(currentUser.uid)) {
                // Admin: ambil semua laporan
                reportsCollection.orderBy("timestamp", Query.Direction.DESCENDING)
            } else {
                // User biasa: ambil laporan milik sendiri
                reportsCollection.whereEqualTo("userId", currentUser.uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
            }

            val snapshot = query.get().await()
            val reports = snapshot.toObjects(Report::class.java)
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
