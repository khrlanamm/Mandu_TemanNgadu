package com.khrlanamm.mandu.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.khrlanamm.mandu.R
import com.khrlanamm.mandu.data.AdminUID
import com.khrlanamm.mandu.ui.history.HistoryActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val CHANNEL_ID = "mandu_channel"
        private const val CHANNEL_NAME = "Mandu Notifications"

        fun saveTokenIfAdmin() {
            val userId = Firebase.auth.currentUser?.uid ?: return

            CoroutineScope(Dispatchers.IO).launch {
                if (AdminUID.isAdmin(userId)) {
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                            return@addOnCompleteListener
                        }
                        val token = task.result
                        Log.d(TAG, "FCM Token for admin $userId: $token")
                        val adminDocRef = Firebase.firestore.collection("admins").document(userId)
                        adminDocRef.update("fcmToken", token)
                            .addOnSuccessListener { Log.d(TAG, "FCM token successfully updated for admin.") }
                            .addOnFailureListener { e -> Log.w(TAG, "Error updating FCM token", e) }
                    }
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        remoteMessage.notification?.let {
            val title = it.title
            val body = it.body
            val imageUrl = it.imageUrl?.toString() // Ambil URL gambar dari notifikasi

            // Jalankan di Coroutine agar bisa melakukan network request untuk mengunduh gambar.
            CoroutineScope(Dispatchers.Main).launch {
                sendNotification(title, body, imageUrl)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        saveTokenIfAdmin()
    }

    private suspend fun sendNotification(title: String?, messageBody: String?, imageUrl: String?) {
        val intent = Intent(this, HistoryActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val bitmap = if (imageUrl != null) getBitmapFromUrl(imageUrl) else null

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications) // Pastikan nama drawable ini benar
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        if (bitmap != null) {
            notificationBuilder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?) // <-- FIX DI SINI
            ).setLargeIcon(bitmap)
        } else {
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private suspend fun getBitmapFromUrl(src: String): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = URL(src)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input: InputStream = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image for notification", e)
            null
        }
    }
}
