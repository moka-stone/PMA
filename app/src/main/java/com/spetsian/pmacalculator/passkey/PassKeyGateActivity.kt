package com.spetsian.pmacalculator.passkey

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.spetsian.pmacalculator.R

/**
 * Точка входа: перенаправляет на первичную настройку PIN или экран разблокировки.
 */
class PassKeyGateActivity : AppCompatActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, R.string.notifications_permission_denied, Toast.LENGTH_SHORT).show()
            }
            // Даже если запретили уведомления — приложение должно стартовать (логика pass key не связана).
            startNextAfterPermissionCheck()
        }

    private var pinConfiguredAtStart: Boolean = false
    private var permissionCheckDone: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = PassKeyRepository(this)
        pinConfiguredAtStart = repo.isPinConfigured()

        // Подписываемся на topic независимо от разрешений — доставка будет работать в любом случае,
        // но для показа уведомлений на Android 13+ разрешение всё же требуется.
        subscribeToTopic()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted && !permissionCheckDone) {
                permissionCheckDone = true
                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission")
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        startNextAfterPermissionCheck()
    }

    private fun subscribeToTopic() {
        FirebaseMessaging.getInstance()
            .subscribeToTopic(TOPIC)
            .addOnCompleteListener {
                // Не критично для работы приложения: только для показа уведомлений
                Log.d(TAG, "subscribeToTopic($TOPIC) success=${it.isSuccessful}")
            }
    }

    private fun startNextAfterPermissionCheck() {
        if (!pinConfiguredAtStart) {
            startActivity(Intent(this, PassKeySetupActivity::class.java))
        } else {
            startActivity(Intent(this, PassKeyUnlockActivity::class.java))
        }
        finish()
    }

    companion object {
        private const val TOPIC = "calc_updates"
        private const val TAG = "FCM"
    }
}
