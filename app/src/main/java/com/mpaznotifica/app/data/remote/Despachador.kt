package com.mpaznotifica.app.data.remote

import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.EstadoEnvio
import kotlinx.coroutines.delay

/**
 * Despacho simulado. El caso prohíbe conectarse a servidores reales, así que el resultado
 * se decide localmente: los teléfonos terminados en 2 simulan un rebote y el resto se
 * despacha. La regla es fija para que la demostración sea repetible.
 */
interface Despachador {
    suspend fun enviar(apoderado: Apoderado, asunto: String): EstadoEnvio
}

class DespachadorSimulado(private val latenciaMs: Long = 150) : Despachador {

    override suspend fun enviar(apoderado: Apoderado, asunto: String): EstadoEnvio {
        delay(latenciaMs)
        return if (apoderado.telefono.endsWith("2")) {
            EstadoEnvio.FALLIDO
        } else {
            EstadoEnvio.DESPACHADO
        }
    }
}
