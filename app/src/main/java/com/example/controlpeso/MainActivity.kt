package com.example.controlpeso

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.controlpeso.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupWeeklyWeightReminder()
    }

    private fun setupWeeklyWeightReminder() {
        val prefs = getSharedPreferences("ControlPesoPrefs", Context.MODE_PRIVATE)
        val isReminderSet = prefs.getBoolean("weight_reminder_set", false)

        if (!isReminderSet) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, AlarmReceiver::class.java).apply {
                putExtra("tipo_plan", "Peso")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                100, // ID único para el recordatorio de peso
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Programar para que suene cada 7 días empezando mañana
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 7)
                set(Calendar.HOUR_OF_DAY, 9) // 9:00 AM
                set(Calendar.MINUTE, 0)
            }

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
            )

            prefs.edit().putBoolean("weight_reminder_set", true).apply()
        }
    }
}
