package com.mpaznotifica.app.model

/**
 * Resuelve qué apoderados reciben un comunicado según el alcance elegido.
 * Se mantiene como objeto puro para poder probar la regla sin Android ni base de datos.
 */
object ResolvedorDestinatarios {

    fun apoderadosPara(
        alcance: AlcanceEnvio,
        destino: String?,
        apoderados: List<Apoderado>,
        estudiantes: List<Estudiante>,
        talleres: List<Taller>
    ): List<Apoderado> = when (alcance) {
        AlcanceEnvio.COLEGIO_COMPLETO -> apoderados

        AlcanceEnvio.CURSO -> {
            val alumnos = estudiantes
                .filter { it.curso == destino }
                .map { it.id }
                .toSet()
            apoderadosConPupilos(apoderados, alumnos)
        }

        AlcanceEnvio.TALLER -> {
            val idsTalleres = talleres
                .filter { it.nombre == destino }
                .map { it.id }
                .toSet()
            val alumnos = estudiantes
                .filter { estudiante -> estudiante.talleresIds.any { it in idsTalleres } }
                .map { it.id }
                .toSet()
            apoderadosConPupilos(apoderados, alumnos)
        }

        AlcanceEnvio.APODERADO_INDIVIDUAL ->
            apoderados.filter { it.nombreCompleto == destino }
    }

    private fun apoderadosConPupilos(
        apoderados: List<Apoderado>,
        idsAlumnos: Set<String>
    ): List<Apoderado> = apoderados.filter { apoderado ->
        apoderado.pupilosIds.any { it in idsAlumnos }
    }
}
