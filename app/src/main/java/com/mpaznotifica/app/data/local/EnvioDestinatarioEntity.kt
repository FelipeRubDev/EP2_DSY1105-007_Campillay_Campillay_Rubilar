package com.mpaznotifica.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Resultado del despacho simulado a un destinatario. Se guarda un registro por
 * apoderado con el canal usado y el estado obtenido.
 */
@Entity(tableName = "envios")
data class EnvioDestinatarioEntity(
    @PrimaryKey val id: String,
    val comunicacionId: String,
    val apoderadoId: String,
    val canal: String,
    val estado: String
)
