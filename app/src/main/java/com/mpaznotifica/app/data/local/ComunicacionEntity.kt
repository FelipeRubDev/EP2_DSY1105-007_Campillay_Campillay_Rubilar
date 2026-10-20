package com.mpaznotifica.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comunicados")
data class ComunicacionEntity(
    @PrimaryKey
    val id: String,
    val asunto: String,
    val cuerpo: String,
    val tipo: String,
    val alcance: String,
    val destino: String?,
    val fechaEnvio: String,
    val autorId: String,
    val adjuntoPdfNombre: String? = null
)