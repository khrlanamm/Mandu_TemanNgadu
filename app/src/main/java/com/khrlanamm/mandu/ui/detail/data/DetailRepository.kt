package com.khrlanamm.mandu.ui.detail

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DetailRepository {
    private val db = FirebaseFirestore.getInstance()
    private val reportsCollection = db.collection("reports")

    suspend fun updateReportStatus(reportId: String, newStatus: String): Result<Unit> {
        return try {
            reportsCollection.document(reportId).update("status", newStatus).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReport(reportId: String): Result<Unit> {
        return try {
            reportsCollection.document(reportId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
