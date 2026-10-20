package com.mpaznotifica.app.ui.screen.comunicados

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mpaznotifica.app.model.AlcanceEnvio
import com.mpaznotifica.app.model.ErroresComunicado
import com.mpaznotifica.app.model.TipoComunicacion
import com.mpaznotifica.app.ui.components.BarraSuperior
import com.mpaznotifica.app.ui.components.SelectorDesplegable
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

@Composable
fun RedactarComunicadoScreen(navController: NavHostController, viewModel: MpazNotificaViewModel) {
    val usuario by viewModel.usuarioActivo.collectAsState()

    var asunto by remember { mutableStateOf("") }
    var cuerpo by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf<TipoComunicacion?>(null) }
    var alcance by remember { mutableStateOf<AlcanceEnvio?>(null) }
    var destino by remember { mutableStateOf<String?>(null) }
    var adjunto by remember { mutableStateOf<String?>(null) }
    var requiereFirma by remember { mutableStateOf(false) }
    var errores by remember { mutableStateOf(ErroresComunicado()) }
    var confirmando by remember { mutableStateOf(false) }
    var enviado by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val selectorPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            var nombre: String? = null
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) nombre = cursor.getString(idx)
            }
            adjunto = nombre ?: uri.lastPathSegment ?: "documento.pdf"
        }
    }

    Scaffold(topBar = { BarraSuperior("Redactar comunicado", navController) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = asunto,
                onValueChange = { asunto = it },
                label = { Text("Asunto") },
                singleLine = true,
                isError = errores.asunto != null,
                supportingText = if (errores.asunto != null) {
                    { Text(errores.asunto.orEmpty()) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = cuerpo,
                onValueChange = { cuerpo = it },
                label = { Text("Cuerpo del mensaje") },
                minLines = 4,
                isError = errores.cuerpo != null,
                supportingText = if (errores.cuerpo != null) {
                    { Text(errores.cuerpo.orEmpty()) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )

            SelectorDesplegable(
                etiqueta = "Tipo de comunicación",
                opciones = viewModel.tiposPermitidos(usuario),
                seleccion = tipo,
                textoDe = { it.etiqueta },
                onSeleccion = { tipo = it },
                error = errores.tipo
            )
            SelectorDesplegable(
                etiqueta = "Alcance (destinatarios)",
                opciones = viewModel.alcancesPermitidos(usuario),
                seleccion = alcance,
                textoDe = { it.etiqueta },
                onSeleccion = {
                    alcance = it
                    destino = null
                },
                error = errores.alcance
            )
            if (alcance != null && alcance != AlcanceEnvio.COLEGIO_COMPLETO) {
                SelectorDesplegable(
                    etiqueta = "Destino",
                    opciones = viewModel.destinosPara(alcance, usuario),
                    seleccion = destino,
                    textoDe = { it },
                    onSeleccion = { destino = it },
                    error = errores.destino
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { selectorPdf.launch(arrayOf("application/pdf")) }) {
                    Text(if (adjunto == null) "Adjuntar PDF (opcional)" else "Cambiar PDF")
                }
                if (adjunto != null) {
                    TextButton(onClick = { adjunto = null }) { Text("Quitar") }
                }
            }
            adjunto?.let { Text("Adjunto: $it", style = MaterialTheme.typography.bodySmall) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = requiereFirma, onCheckedChange = { requiereFirma = it })
                Text("Es una autorización que requiere firma física del apoderado")
            }

            AnimatedVisibility(
                visible = requiereFirma,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Rol solo informativo: la app notifica y avisa que la autorización debe firmarse en papel. " +
                                "No se capturan firmas en la app.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                onClick = {
                    errores = viewModel.validarComunicado(asunto, cuerpo, tipo, alcance, destino)
                    if (!errores.hayErrores && tipo != null && alcance != null) confirmando = true
                }
            ) { Text("Enviar") }
        }
    }

    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            title = { Text("¿Enviar comunicado?") },
            text = { Text("Una vez enviado, el comunicado queda registrado de forma inmutable en la base de datos.") },
            confirmButton = {
                TextButton(onClick = {
                    val tipoElegido = tipo
                    val alcanceElegido = alcance
                    if (tipoElegido != null && alcanceElegido != null) {
                        viewModel.enviarComunicado(
                            asunto = asunto.trim(),
                            cuerpo = cuerpo.trim(),
                            tipo = tipoElegido,
                            alcance = alcanceElegido,
                            destino = destino,
                            adjuntoPdfNombre = adjunto,
                            requiereFirmaFisica = requiereFirma
                        )
                    }
                    confirmando = false
                    enviado = true
                }) { Text("Enviar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmando = false }) { Text("Cancelar") }
            }
        )
    }

    if (enviado) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Comunicado enviado") },
            text = { Text("El comunicado fue registrado exitosamente en la base de datos local (Room).") },
            confirmButton = {
                TextButton(onClick = {
                    enviado = false
                    navController.popBackStack()
                }) { Text("Aceptar") }
            }
        )
    }
}