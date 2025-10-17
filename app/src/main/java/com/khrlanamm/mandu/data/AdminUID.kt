package com.khrlanamm.mandu.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await

object AdminUID {
    private val db: FirebaseFirestore by lazy { Firebase.firestore }

    suspend fun isAdmin(userId: String?): Boolean {
        if (userId.isNullOrEmpty()) {
            return false
        }

        return try {
            val adminDocRef = db.collection("admins").document(userId)
            val document = adminDocRef.get().await()
            document.exists()
        } catch (e: Exception) {
            Log.e("AdminUID", "Error checking admin status for user: $userId", e)
            false
        }
    }
}
