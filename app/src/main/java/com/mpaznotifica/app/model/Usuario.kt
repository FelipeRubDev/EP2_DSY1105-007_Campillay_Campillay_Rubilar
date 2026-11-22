package com.mpaznotifica.app.model

/**
 * Usuario emisor que inicia sesión. cursoAsignado solo aplica al Profesor/a Jefe (su curso de
 * jefatura). Solo datos ficticios (ver MockData.usuarios).
 */
data class Usuario(
    val id: String,
    val nombre: String,
    val usuario: String,
    val clave: String,
    val perfil: Perfil,
    val cursoAsignado: String? = null
)

/** Errores del login separados por campo, para pintarlos bajo cada texto. */
data class ErroresLogin(
    val usuario: String? = null,
    val clave: String? = null,
    val general: String? = null
) {
    val hayErrores: Boolean
        get() = usuario != null || clave != null || general != null
}
