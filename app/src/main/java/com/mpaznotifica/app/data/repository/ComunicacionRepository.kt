package com.mpaznotifica.app.data.repository

import androidx.room.withTransaction
import com.mpaznotifica.app.data.local.AppDatabase
import com.mpaznotifica.app.data.local.ComunicacionEntity
import com.mpaznotifica.app.data.local.EnvioDestinatarioEntity
import kotlinx.coroutines.flow.Flow

class ComunicacionRepository(private val database: AppDatabase) {

    private val comunicacionDao = database.comunicacionDao()
    private val envioDao = database.envioDestinatarioDao()

    val todosLosComunicados: Flow<List<ComunicacionEntity>> =
        comunicacionDao.obtenerTodos()

    val todosLosEnvios: Flow<List<EnvioDestinatarioEntity>> =
        envioDao.obtenerTodos()

    /**
     * El comunicado y sus envíos se escriben en una sola transacción: si algo falla,
     * no queda un comunicado sin destinatarios.
     */
    suspend fun registrarEnvio(
        comunicado: ComunicacionEntity,
        envios: List<EnvioDestinatarioEntity>
    ) {
        database.withTransaction {
            comunicacionDao.insertarComunicado(comunicado)
            if (envios.isNotEmpty()) envioDao.insertarEnvios(envios)
        }
    }

    suspend fun guardarComunicado(comunicado: ComunicacionEntity) {
        comunicacionDao.insertarComunicado(comunicado)
    }
}
