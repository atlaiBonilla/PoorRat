package com.poorrat.gotcha.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.poorrat.gotcha.data.local.entity.SupplementEntity
import com.poorrat.gotcha.receiver.SupplementAlarmReceiver
import java.util.Calendar

class SupplementAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(supplement: SupplementEntity) {
        if (!supplement.alarmEnabled) {
            cancel(supplement.id)
            return
        }

        val intent = Intent(context, SupplementAlarmReceiver::class.java).apply {
            putExtra(SupplementAlarmReceiver.EXTRA_SUPPLEMENT_ID, supplement.id)
            putExtra(SupplementAlarmReceiver.EXTRA_SUPPLEMENT_NAME, supplement.name)
            putExtra(SupplementAlarmReceiver.EXTRA_SUPPLEMENT_DOSAGE, supplement.dosage)
            putExtra(SupplementAlarmReceiver.EXTRA_SUPPLEMENT_MOMENT, supplement.intakeMoment)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            supplement.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, supplement.alarmHour)
            set(Calendar.MINUTE, supplement.alarmMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Graceful fallback if exact alarms are restricted
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancel(supplementId: Long) {
        val intent = Intent(context, SupplementAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            supplementId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
