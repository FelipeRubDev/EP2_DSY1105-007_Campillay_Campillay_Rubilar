package com.mpaznotifica.app.viewmodel

import androidx.lifecycle.ViewModel
import com.mpaznotifica.app.data.MockData
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.Apoderado
import com.mpaznotifica.app.model.CanalDespacho
import com.mpaznotifica.app.model.Comunicacion
import com.mpaznotifica.app.model.EnvioDestinatario
import com.mpaznotifica.app.model.ErroresComunicado
import com.mpaznotifica.app.model.ErroresLogin
import com.mpaznotifica.app.model.Perfil
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import com.mpaznotifica.app.data.local.ComunicacionEntity
import com.mpaznotifica.app.data.local.EnvioDestinatarioEntity
import com.mpaznotifica.app.data.remote.Despachador
import com.mpaznotifica.app.data.remote.DespachadorSimulado
import com.mpaznotifica.app.data.repository.ComunicacionRepository
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.model.ResolvedorDestinatarios
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
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
 * Concentra el estado de sesión, la validación del formulario, el armado del comunicado y
 * la observación de los datos que vienen de Room.
 */
class MpazNotificaViewModel(
    private val despachador: Despachador = DespachadorSimulado()
) : ViewModel() {

    // =====================================================================
    // Módulo "Autenticación y perfiles"
    // =====================================================================

    private val _usuarioActivo = MutableStateFlow<Usuario?>(null)
    val usuarioActivo: StateFlow<Usuario?> = _usuarioActivo.asStateFlow()

    private val _erroresLogin = MutableStateFlow(ErroresLogin())
    val erroresLogin: StateFlow<ErroresLogin> = _erroresLogin.asStateFlow()

    /**
     * Inicio de sesión diferenciado. Valida por campo y luego compara contra los usuarios
     * de prueba. No usa backend: el caso lo permite mientras el CITT no lo confirme.
     */
    fun login(usuario: String, clave: String): Boolean {
        val correo = usuario.trim()

        val errorUsuario = when {
            correo.isBlank() -> "Ingresa tu correo institucional"
            !correo.contains("@") -> "El correo no es válido"
            else -> null
        }
        val errorClave = when {
            clave.isBlank() -> "Ingresa tu clave"
            clave.length < 6 -> "La clave debe tener al menos 6 caracteres"
            else -> null
        }

        if (errorUsuario != null || errorClave != null) {
            _erroresLogin.value = ErroresLogin(usuario = errorUsuario, clave = errorClave)
            return false
        }

        val encontrado = MockData.usuarios.find {
            it.usuario.equals(correo, ignoreCase = true) && it.clave == clave
        }
        if (encontrado == null) {
            _erroresLogin.value = ErroresLogin(general = "Correo o clave incorrectos")
            return false
        }

        _usuarioActivo.value = encontrado
        _erroresLogin.value = ErroresLogin()
        return true
    }

    fun logout() {
        _usuarioActivo.value = null
        _erroresLogin.value = ErroresLogin()
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
     * Alcances que el perfil puede usar. El Profesor/a Jefe solo emite a su curso.
     */
    fun alcancesPermitidos(usuario: Usuario?): List<AlcanceEnvio> = when (usuario?.perfil) {
        null -> emptyList()
        Perfil.INSPECTORIA_GENERAL -> AlcanceEnvio.entries.toList()
        Perfil.PROFESOR_JEFE -> listOf(AlcanceEnvio.CURSO)
    }

    /**
     * Tipos que el perfil puede emitir. El Profesor/a Jefe no puede enviar circulares
     * institucionales; el resto de los tipos sigue disponible.
     */
    fun tiposPermitidos(usuario: Usuario?): List<TipoComunicacion> = when (usuario?.perfil) {
        null -> emptyList()
        Perfil.INSPECTORIA_GENERAL -> TipoComunicacion.entries.toList()
        Perfil.PROFESOR_JEFE -> TipoComunicacion.entries.filter { it != TipoComunicacion.INSTITUCIONAL }
    }

    /**
     * Opciones de destino según el alcance. El Profesor/a Jefe solo ve su propio curso.
     */
    fun destinosPara(alcance: AlcanceEnvio?, usuario: Usuario?): List<String> = when (alcance) {
        AlcanceEnvio.CURSO -> when (usuario?.perfil) {
            Perfil.PROFESOR_JEFE -> listOfNotNull(usuario.cursoAsignado)
            else -> cursos
        }
        AlcanceEnvio.TALLER -> when (usuario?.perfil) {
            Perfil.PROFESOR_JEFE -> emptyList()
            else -> talleres.map { it.nombre }
        }
        AlcanceEnvio.APODERADO_INDIVIDUAL -> when (usuario?.perfil) {
            Perfil.PROFESOR_JEFE -> emptyList()
            else -> _apoderados.value.map { it.nombreCompleto }
        }
        AlcanceEnvio.COLEGIO_COMPLETO, null -> emptyList()
    }

    /**
     * Valida el formulario y devuelve un mensaje por campo. Además de los obligatorios,
     * comprueba que el tipo y el alcance estén permitidos para el perfil activo.
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

    private val _estadoEnvio = MutableStateFlow<EstadoEnvioUi>(EstadoEnvioUi.Inactivo)
    val estadoEnvio: StateFlow<EstadoEnvioUi> = _estadoEnvio.asStateFlow()

    private val _eventoNotificacion = MutableSharedFlow<EventoEnvio>(extraBufferCapacity = 1)
    val eventoNotificacion: SharedFlow<EventoEnvio> = _eventoNotificacion.asSharedFlow()

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
                        adjuntoPdfUri = entity.adjuntoPdfUri,
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

    // Estado de los envíos por destinatario, leído desde Room.
    val envios: StateFlow<List<EnvioDestinatario>> by lazy {
        if (::repository.isInitialized) {
            repository.todosLosEnvios.map { lista ->
                lista.map { entity ->
                    EnvioDestinatario(
                        id = entity.id,
                        comunicacionId = entity.comunicacionId,
                        apoderadoId = entity.apoderadoId,
                        canal = try { CanalDespacho.valueOf(entity.canal) } catch (e: Exception) { CanalDespacho.EMAIL },
                        estado = try { EstadoEnvio.valueOf(entity.estado) } catch (e: Exception) { EstadoEnvio.FALLIDO }
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
        } else {
            MutableStateFlow(emptyList())
        }
    }

    fun limpiarEstadoEnvio() {
        _estadoEnvio.value = EstadoEnvioUi.Inactivo
    }

    /**
     * Registra el comunicado y simula el despacho:
     *  - resuelve los apoderados según el alcance;
     *  - envía por el canal preferente de cada uno;
     *  - guarda el comunicado y un estado por destinatario en una transacción.
     * Si no hay destinatarios, no se guarda nada.
     */
    fun enviarComunicado(
        asunto: String,
        cuerpo: String,
        tipo: TipoComunicacion,
        alcance: AlcanceEnvio,
        destino: String?,
        adjuntoPdfNombre: String?,
        requiereFirmaFisica: Boolean,
        adjuntoPdfUri: String? = null
    ) {
        val autorActual = _usuarioActivo.value ?: return
        if (!::repository.isInitialized) {
            _estadoEnvio.value = EstadoEnvioUi.Error("La base de datos no está disponible")
            return
        }

        val destinatarios = ResolvedorDestinatarios.apoderadosPara(
            alcance = alcance,
            destino = destino,
            apoderados = _apoderados.value,
            estudiantes = MockData.estudiantes,
            talleres = talleres
        )
        if (destinatarios.isEmpty()) {
            _estadoEnvio.value = EstadoEnvioUi.Error("No hay apoderados para ese destino")
            return
        }

        val fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val comunicadoId = UUID.randomUUID().toString()

        val nuevoComunicadoEntity = ComunicacionEntity(
            id = comunicadoId,
            asunto = asunto,
            cuerpo = cuerpo,
            tipo = tipo.name,
            alcance = alcance.name,
            destino = destino,
            fechaEnvio = fechaActual,
            autorId = autorActual.id,
            adjuntoPdfNombre = adjuntoPdfNombre,
            adjuntoPdfUri = adjuntoPdfUri
        )

        _estadoEnvio.value = EstadoEnvioUi.Enviando

        viewModelScope.launch {
            val enviosEntities = destinatarios.map { apoderado ->
                val estado = runCatching {
                    despachador.enviar(apoderado, asunto)
                }.getOrDefault(EstadoEnvio.FALLIDO)

                EnvioDestinatarioEntity(
                    id = UUID.randomUUID().toString(),
                    comunicacionId = comunicadoId,
                    apoderadoId = apoderado.id,
                    canal = apoderado.canalPreferente.name,
                    estado = estado.name
                )
            }

            try {
                repository.registrarEnvio(nuevoComunicadoEntity, enviosEntities)

                val despachados = enviosEntities.count { it.estado == EstadoEnvio.DESPACHADO.name }
                val fallidos = enviosEntities.size - despachados
                _estadoEnvio.value = EstadoEnvioUi.Exito(despachados, fallidos)
                _eventoNotificacion.emit(
                    EventoEnvio(
                        asunto = asunto,
                        despachados = despachados,
                        fallidos = fallidos,
                        requiereFirmaFisica = requiereFirmaFisica
                    )
                )
            } catch (e: Exception) {
                _estadoEnvio.value = EstadoEnvioUi.Error("No se pudo guardar el comunicado")
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
        envios.value.filter { it.comunicacionId == comunicacionId }

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
