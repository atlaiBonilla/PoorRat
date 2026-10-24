package com.poorrat.gotcha.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.poorrat.gotcha.MainActivity
import com.poorrat.gotcha.R
import com.poorrat.gotcha.alarm.SupplementAlarmScheduler
import com.poorrat.gotcha.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SupplementAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val supplementId = intent.getLongExtra(EXTRA_SUPPLEMENT_ID, -1L)
        val supplementName = intent.getStringExtra(EXTRA_SUPPLEMENT_NAME) ?: "Suplemento"
        val dosage = intent.getStringExtra(EXTRA_SUPPLEMENT_DOSAGE) ?: ""
        val moment = intent.getStringExtra(EXTRA_SUPPLEMENT_MOMENT) ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de Suplementos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de tomas diarias de suplementación PoorRat"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            supplementId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtext = if (dosage.isNotBlank() && moment.isNotBlank()) {
            "$dosage · $moment"
        } else dosage.ifBlank { moment }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("PoorRat · $supplementName")
            .setContentText("Es hora de tu toma: $subtext")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        notificationManager.notify(supplementId.toInt(), notification)

        // Reschedule for next day if supplement still has alarmEnabled
        if (supplementId != -1L) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val supplement = db.supplementDao().getSupplementByIdSync(supplementId)
                    if (supplement != null && supplement.alarmEnabled) {
                        SupplementAlarmScheduler(context).schedule(supplement)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "poorrat_supplements_channel"
        const val EXTRA_SUPPLEMENT_ID = "extra_supplement_id"
        const val EXTRA_SUPPLEMENT_NAME = "extra_supplement_name"
        const val EXTRA_SUPPLEMENT_DOSAGE = "extra_supplement_dosage"
        const val EXTRA_SUPPLEMENT_MOMENT = "extra_supplement_moment"
    }
}
