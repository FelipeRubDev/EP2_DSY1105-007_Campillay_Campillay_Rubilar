package com.mpaznotifica.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ComunicacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarComunicado(comunicado: ComunicacionEntity)

    @Query("SELECT * FROM comunicados ORDER BY fechaEnvio DESC")
    fun obtenerTodos(): Flow<List<ComunicacionEntity>>
}