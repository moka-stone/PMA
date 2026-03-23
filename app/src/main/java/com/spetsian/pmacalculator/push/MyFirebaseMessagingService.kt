package com.spetsian.pmacalculator.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.spetsian.pmacalculator.R
import com.spetsian.pmacalculator.passkey.PassKeyGateActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(
            TAG,
            "onMessageReceived: data=${remoteMessage.data} notification=${remoteMessage.notification}"
        )

        val title =
            remoteMessage.data["title"]
                ?: remoteMessage.notification?.title
                ?: getString(R.string.push_default_title)

        val body =
            remoteMessage.data["body"]
                ?: remoteMessage.notification?.body
                ?: getString(R.string.push_default_body)

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        Log.d(TAG, "showNotification title='$title' body='$body'")

        val notificationManager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, PassKeyGateActivity::class.java).apply {
            // Если откроют из уведомления, стартуем gate и пройдёт pass key.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            pendingFlags
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "FCM"
        private const val CHANNEL_ID = "calc_updates_channel"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_NAME = "Calculator updates"
    }
}

