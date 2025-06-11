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

    suspend fun getHistoryReports(): Result<List<Report>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Pengguna tidak terautentikasi."))

            val query = if (AdminUID.isAdmin(currentUser.uid)) {
                reportsCollection.orderBy("timestamp", Query.Direction.DESCENDING)
            } else {
                reportsCollection.whereEqualTo("userId", currentUser.uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
            }

            val snapshot = query.get().await()

            // PERBAIKAN: Memetakan setiap dokumen dan menyalin ID-nya ke dalam objek Report
            val reports = snapshot.documents.mapNotNull { document ->
                // Mengubah dokumen menjadi objek Report, lalu menyalinnya dengan ID yang benar
                document.toObject(Report::class.java)?.copy(id = document.id)
            }
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
