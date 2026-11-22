package com.mpaznotifica.app.notificaciones

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mpaznotifica.app.R
import com.mpaznotifica.app.viewmodel.EventoEnvio

/**
 * Notificación local del resultado del envío. Es el segundo recurso nativo del proyecto,
 * junto con el selector de archivos. No requiere internet.
 */
object NotificadorLocal {

    private const val CANAL = "envios"
    private const val ID_NOTIFICACION = 1001

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL,
                "Envíos de comunicados",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(canal)
        }
    }

    // El permiso se comprueba en puedeNotificar(); lint no sigue esa indirección.
    @SuppressLint("MissingPermission")
    fun mostrar(context: Context, evento: EventoEnvio) {
        if (!puedeNotificar(context)) return

        crearCanal(context)

        val detalle = buildString {
            append("${evento.despachados} despachados")
            if (evento.fallidos > 0) append(", ${evento.fallidos} fallidos")
            if (evento.requiereFirmaFisica) append(". Requiere firma física")
        }

        val notificacion = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.logo_app)
            .setContentTitle("Comunicado enviado")
            .setContentText(detalle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detalle))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(ID_NOTIFICACION, notificacion)
    }

    private fun puedeNotificar(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
