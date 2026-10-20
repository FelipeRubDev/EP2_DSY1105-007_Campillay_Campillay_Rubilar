package com.mpaznotifica.app.data.repository

import com.mpaznotifica.app.data.local.ComunicacionDao
import com.mpaznotifica.app.data.local.ComunicacionEntity
import kotlinx.coroutines.flow.Flow

class ComunicacionRepository(private val comunicacionDao: ComunicacionDao) {

    val todosLosComunicados: Flow<List<ComunicacionEntity>> = comunicacionDao.obtenerTodos()

    suspend fun guardarComunicado(comunicado: ComunicacionEntity) {
        comunicacionDao.insertarComunicado(comunicado)
    }
}