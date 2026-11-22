package com.mpaznotifica.app.viewmodel

/** Estado del envío que observa el formulario para no mostrar el éxito antes de guardar. */
sealed interface EstadoEnvioUi {
    data object Inactivo : EstadoEnvioUi
    data object Enviando : EstadoEnvioUi
    data class Exito(val despachados: Int, val fallidos: Int) : EstadoEnvioUi
    data class Error(val mensaje: String) : EstadoEnvioUi
}

/** Evento de un solo uso para disparar la notificación local desde la pantalla. */
data class EventoEnvio(
    val asunto: String,
    val despachados: Int,
    val fallidos: Int,
    val requiereFirmaFisica: Boolean
)
