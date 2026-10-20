package com.mpaznotifica.app.viewmodel

import androidx.lifecycle.ViewModel
import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.ErroresComunicado
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import com.mpaznotifica.app.data.local.ComunicacionEntity
import com.mpaznotifica.app.data.repository.ComunicacionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel único, compartido por todas las pantallas (se crea una sola vez en AppNavigation).
 *
 * ESTO ES UN ESQUELETO: las pantallas ya llaman a todas estas funciones, pero la lógica de
 * negocio del caso está vacía a propósito y marcada con TODO(equipo), citando el módulo de
 * Requerimientos_DSY1105-007 al que corresponde. Mientras no se implemente:
 *  - el login siempre rechaza (usar "Modo desarrollo" en la pantalla de login),
 *  - cualquier perfil ve todos los alcances y tipos (sin restricción por perfil),
 *  - enviar un comunicado o cambiar el canal de un apoderado no hace nada,
 *  - los filtros del historial no filtran.
 */
class MpazNotificaViewModel : ViewModel() {

    // =====================================================================
    // Módulo "Autenticación y perfiles"
    // =====================================================================

    private val _usuarioActivo = MutableStateFlow<Usuario?>(null)
    val usuarioActivo: StateFlow<Usuario?> = _usuarioActivo.asStateFlow()

    private val _errorLogin = MutableStateFlow<String?>(null)
    val errorLogin: StateFlow<String?> = _errorLogin.asStateFlow()

    /**
     * TODO(equipo): inicio de sesión diferenciado para Inspectoría General y Profesor/a Jefe.
     * Validar campos vacíos (patrón Guía 11) y luego comparar contra MockData.usuarios (o el
     * backend que confirme el CITT). Si es correcto: dejar _usuarioActivo y devolver true.
     * Si no: dejar _errorLogin con un mensaje y devolver false.
     */
    fun login(usuario: String, clave: String): Boolean {
        _errorLogin.value = "Login sin implementar — ver TODO(equipo) en MpazNotificaViewModel.login()."
        return false
    }

    /**
     * SOLO PARA DESARROLLO: entra con el usuario de prueba del perfil indicado, sin validar
     * credenciales. TODO(equipo): eliminar esta función y los botones "Modo desarrollo".
     */
    fun entrarModoDesarrollo(perfil: Perfil) {
        _usuarioActivo.value = MockData.usuarios.first { it.perfil == perfil }
        _errorLogin.value = null
    }

    fun logout() {
        _usuarioActivo.value = null
        _errorLogin.value = null
        _comunicadoSeleccionadoId.value = null
    }

    // =====================================================================
    // Datos de referencia (no es lógica de negocio)
    // =====================================================================

    val cursos: List<String> = MockData.cursos
    val talleres = MockData.talleres
    val emisores: List<Usuario> = MockData.usuarios

    private val _apoderados = MutableStateFlow(MockData.apoderados)
    val apoderados: StateFlow<List<Apoderado>> = _apoderados.asStateFlow()

    fun apoderadoPorId(id: String): Apoderado? = _apoderados.value.find { it.id == id }

    fun nombresPupilos(apoderado: Apoderado): List<String> =
        MockData.estudiantes.filter { it.id in apoderado.pupilosIds }.map { "${it.nombreReferencial} (${it.curso})" }

    // =====================================================================
    // Módulo "Redacción y envío"
    // =====================================================================

    /**
     * TODO(equipo): devolver solo los alcances que el perfil puede usar.
     * RF: "restringir las opciones de emisión según el perfil autenticado".
     * Regla de negocio: un Profesor/a Jefe no puede emitir a "colegio completo" ni a un curso
     * distinto al propio (alcance sobre talleres pendiente de confirmar con el CITT).
     * Hoy devuelve los cuatro alcances para cualquier perfil.
     */
    fun alcancesPermitidos(usuario: Usuario?): List<AlcanceEnvio> = AlcanceEnvio.entries.toList()

    /**
     * TODO(equipo): devolver solo los tipos de comunicación que el perfil puede emitir.
     * Criterio de éxito del caso: un Profesor/a Jefe no puede enviar circulares institucionales,
     * solo lo referente a su curso. Hoy devuelve todos los tipos.
     */
    fun tiposPermitidos(usuario: Usuario?): List<TipoComunicacion> = TipoComunicacion.entries.toList()

    /**
     * TODO(equipo): opciones de destino según el alcance, respetando el perfil (por ejemplo, un
     * Profesor/a Jefe solo debería ver su propio curso). Hoy muestra todas las opciones.
     */
    fun destinosPara(alcance: AlcanceEnvio?, usuario: Usuario?): List<String> = when (alcance) {
        AlcanceEnvio.CURSO -> cursos
        AlcanceEnvio.TALLER -> talleres.map { it.nombre }
        AlcanceEnvio.APODERADO_INDIVIDUAL -> _apoderados.value.map { it.nombreCompleto }
        AlcanceEnvio.COLEGIO_COMPLETO, null -> emptyList()
    }

    /**
     * TODO(equipo): validar el formulario y devolver los errores por campo: asunto y cuerpo
     * obligatorios, tipo y alcance elegidos, destino obligatorio salvo "colegio completo", y que
     * el alcance esté permitido para el perfil. Hoy devuelve "sin errores" siempre.
     */


    //CODIGO DE REQUERIMIENTO 3 A PRESENTAR.
    fun validarComunicado(
        asunto: String,
        cuerpo: String,
        tipo: TipoComunicacion?,
        alcance: AlcanceEnvio?,
        destino: String?
    ): ErroresComunicado {
        
        var errorAsunto: String? = null
        var errorCuerpo: String? = null
        var errorTipo: String? = null
        var errorAlcance: String? = null
        var errorDestino: String? = null


        // Obtenemos el usuario actual para validar sus permisos
        val usuario = _usuarioActivo.value


        // Validación del Asunto
        if (asunto.isBlank()) {
            errorAsunto = "El asunto es obligatorio"
        } else if (asunto.length > 100) {
            errorAsunto = "El asunto no puede superar los 100 caracteres"
        }


        //  Validación del Cuerpo
        if (cuerpo.isBlank()) {
            errorCuerpo = "El cuerpo del comunicado es obligatorio"
        }


        // Validación del Tipo de comunicación
        if (tipo == null) {
            errorTipo = "Selecciona un tipo de comunicación"
        } else if (!tiposPermitidos(usuario).contains(tipo)) {
            errorTipo = "Este tipo de comunicado no está permitido para tu perfil"
        }


        // Validación del Alcance
        if (alcance == null) {
            errorAlcance = "Selecciona un alcance"
        } else if (!alcancesPermitidos(usuario).contains(alcance)) {
            errorAlcance = "Este alcance no está permitido para tu perfil"
        }


        // Validación del Destino (Obligatorio EXCEPTO si es a todo el colegio)
        if (alcance != AlcanceEnvio.COLEGIO_COMPLETO && destino.isNullOrBlank()) {
            errorDestino = "El destino es obligatorio para este alcance"
        }


        // Retornamos el objeto con los errores encontrados (si todo está nulo, significa que no hay errores)
        return ErroresComunicado(
            asunto = errorAsunto,
            cuerpo = errorCuerpo,
            tipo = errorTipo,
            alcance = errorAlcance,
            destino = errorDestino
        )
    }

    private val _envios = MutableStateFlow(MockData.envios)
    private lateinit var repository: ComunicacionRepository

    // Función para inicializar la base de datos desde la UI
    fun inicializarRepository(repo: ComunicacionRepository) {
        this.repository = repo
    }

    // Obtenemos los comunicados desde Room y los convertimos al modelo de la UI en tiempo real
    val comunicados: StateFlow<List<Comunicacion>> by lazy {
        if (::repository.isInitialized) {
            repository.todosLosComunicados.map { listaEntities ->
                listaEntities.map { entity ->
                    val autorObj = MockData.usuarios.find { it.id == entity.autorId } ?: MockData.usuarios.first()

                    Comunicacion(
                        id = entity.id,
                        asunto = entity.asunto,
                        cuerpo = entity.cuerpo,
                        tipo = try { TipoComunicacion.valueOf(entity.tipo) } catch (e: Exception) { TipoComunicacion.INSTITUCIONAL },
                        alcance = try { AlcanceEnvio.valueOf(entity.alcance) } catch (e: Exception) { AlcanceEnvio.CURSO },
                        destinoReferencia = entity.destino,
                        adjuntoPdfNombre = entity.adjuntoPdfNombre,
                        requiereFirmaFisica = false,
                        autorId = entity.autorId,
                        autorNombre = autorObj.nombre,
                        fechaHora = entity.fechaEnvio
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = MockData.comunicados
            )
        } else {
            MutableStateFlow(MockData.comunicados)
        }
    }
    /**
     * TODO(equipo): registrar el comunicado y simular su despacho. RF del módulo:
     *  - guardar automáticamente autor (usuarioActivo), fecha/hora, contenido y destinatarios;
     *  - resolver los apoderados destinatarios según el alcance (curso, taller, individual o todos);
     *  - simular el envío por el canal preferente de cada apoderado mediante un servicio mock
     *    (ver data/remote/ApiPendiente.kt) y guardar un EnvioDestinatario con su estado
     *    (despachado/fallido) por cada uno;
     *  - si requiereFirmaFisica: solo informar por correo y alertar que corresponde firma en
     *    papel (no capturar firmas).
     * Regla: el registro queda inmutable una vez enviado.
     * Hoy no hace nada: el comunicado no aparece en el historial.
     */
    fun enviarComunicado(
        asunto: String,
        cuerpo: String,
        tipo: TipoComunicacion,
        alcance: AlcanceEnvio,
        destino: String?,
        adjuntoPdfNombre: String?,
        requiereFirmaFisica: Boolean
    ) {
        val autorActual = _usuarioActivo.value ?: return

        val fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val nuevoComunicadoEntity = ComunicacionEntity(
            id = UUID.randomUUID().toString(),
            asunto = asunto,
            cuerpo = cuerpo,
            tipo = tipo.name,
            alcance = alcance.name,
            destino = destino,
            fechaEnvio = fechaActual,
            autorId = autorActual.id,
            adjuntoPdfNombre = adjuntoPdfNombre
        )

        viewModelScope.launch {
            if (::repository.isInitialized) {
                repository.guardarComunicado(nuevoComunicadoEntity)
            }
        }
    }

    // =====================================================================
    // Módulo "Historial y trazabilidad"
    // =====================================================================

    private val _comunicadoSeleccionadoId = MutableStateFlow<String?>(null)
    val comunicadoSeleccionadoId: StateFlow<String?> = _comunicadoSeleccionadoId.asStateFlow()

    fun seleccionarComunicado(id: String?) {
        _comunicadoSeleccionadoId.value = id
    }

    // Se usa comunicados.value en lugar de _comunicados.value
    fun comunicadoPorId(id: String?): Comunicacion? = comunicados.value.find { it.id == id }
    fun enviosDe(comunicacionId: String): List<EnvioDestinatario> =
        _envios.value.filter { it.comunicacionId == comunicacionId }

    // Se usa comunicados.value en lugar de _comunicados.value
    fun historialFiltrado(
        tipo: TipoComunicacion?,
        emisorId: String?,
        fechaDesde: String?,
        fechaHasta: String?
    ): List<Comunicacion> = comunicados.value

    // =====================================================================
    // Canal preferente del apoderado (supuesto pendiente con el CITT)
    // =====================================================================

    /**
     * TODO(equipo): guardar el canal preferente del apoderado. Criterio de éxito: "emisión
     * simulada inmediata al canal preferente configurado por el apoderado". Hoy no hace nada.
     */
    fun actualizarCanalPreferente(apoderadoId: String, canal: CanalDespacho) {
        // Sin implementar todavía.
    }
}
