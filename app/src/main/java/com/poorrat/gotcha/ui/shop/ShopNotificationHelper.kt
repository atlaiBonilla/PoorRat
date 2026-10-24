package com.poorrat.gotcha.ui.shop

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.poorrat.gotcha.MainActivity
import com.poorrat.gotcha.R

object ShopNotificationHelper {
    const val CHANNEL_ID = "poorrat_shop_channel"
    private const val PREFS_NAME = "poorrat_shop_prefs"
    private const val KEY_SHOP_REMINDER_SET = "shop_reminder_set"
    private const val NOTIFICATION_ID = 9002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Avisos de Tienda PoorRat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de apertura y promociones de la tienda PoorRat"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun isReminderMarked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHOP_REMINDER_SET, false)
    }

    fun setReminderMarked(context: Context, marked: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHOP_REMINDER_SET, marked).apply()
    }

    fun triggerShopNotification(context: Context): Boolean {
        createNotificationChannel(context)
        setReminderMarked(context, true)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("PoorRat · Tienda Próximamente")
            .setContentText("Aviso registrado: Te notificaremos en cuanto la tienda abra sus puertas.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("La opción de Tienda no está disponible aún. Hemos marcado tu recordatorio y recibirás un aviso tan pronto como la tienda esté abierta.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return try {
            manager.notify(NOTIFICATION_ID, notification)
            true
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}
