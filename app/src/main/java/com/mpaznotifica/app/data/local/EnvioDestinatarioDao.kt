package com.mpaznotifica.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvioDestinatarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEnvios(envios: List<EnvioDestinatarioEntity>)

    @Query("SELECT * FROM envios")
    fun obtenerTodos(): Flow<List<EnvioDestinatarioEntity>>
}
