package com.example.controlpeso

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tipo = intent.getStringExtra("tipo_plan") ?: "Peso"
        
        val title = if (tipo == "Peso") "Actualización de Peso" else "Recordatorio de ControlPeso"
        val message = if (tipo == "Peso") "¡Es momento de registrar tu nuevo peso de la semana!" else "¡Es hora de seguir tu $tipo!"
        
        showNotification(context, title, message)
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val channelId = "health_alerts"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alertas de Salud", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(tipoNotificacion(title), notification)
    }

    private fun tipoNotificacion(title: String): Int {
        return if (title.contains("Peso")) 2 else 1
    }
}
