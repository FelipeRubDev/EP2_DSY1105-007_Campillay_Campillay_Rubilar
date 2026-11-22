package com.mpaznotifica.app.ui.screen.comunicados

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mpaznotifica.app.model.EstadoEnvio
import com.mpaznotifica.app.ui.components.BarraSuperior
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

/**
 * Detalle de un comunicado del historial. RF: "cada registro debe incluir autor, fecha/hora,
 * contenido, destinatarios y canal empleado" y "registrar el estado del envío por destinatario".
 *
 * Regla de negocio: el comunicado es INMUTABLE, por eso esta pantalla es solo de lectura
 * (no tiene botones de editar ni borrar).
 */
@Composable
fun DetalleComunicadoScreen(navController: NavHostController, viewModel: MpazNotificaViewModel) {
    val seleccionadoId by viewModel.comunicadoSeleccionadoId.collectAsState()
    val comunicados by viewModel.comunicados.collectAsState()
    val envios by viewModel.envios.collectAsState()
    val comunicado = comunicados.find { it.id == seleccionadoId }
    val context = LocalContext.current

    Scaffold(topBar = { BarraSuperior("Detalle del comunicado", navController) }) { padding ->
        if (comunicado == null) {
            Text("Comunicado no encontrado.", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        val enviosDelComunicado = envios.filter { it.comunicacionId == comunicado.id }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(comunicado.asunto, style = MaterialTheme.typography.titleLarge)
                    Text(comunicado.cuerpo, style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Tipo: ${comunicado.tipo.etiqueta}")
                    Text("Alcance: ${comunicado.alcance.etiqueta}" + (comunicado.destinoReferencia?.let { " ($it)" } ?: ""))
                    Text("Autor: ${comunicado.autorNombre}")
                    Text("Fecha y hora: ${comunicado.fechaHora}")
                    Text("Adjunto: ${comunicado.adjuntoPdfNombre ?: "sin adjunto"}")
                    comunicado.adjuntoPdfUri?.let { uri ->
                        TextButton(onClick = {
                            val abierto = runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(uri), "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                )
                            }.isSuccess
                            if (!abierto) {
                                Toast.makeText(
                                    context,
                                    "No hay una app para abrir el PDF",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }) { Text("Abrir PDF adjunto") }
                    }
                    if (comunicado.requiereFirmaFisica) {
                        Text(
                            "Autorización con firma física: la firma se entrega en papel.",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Text("Destinatarios y estado del envío (${enviosDelComunicado.size})", style = MaterialTheme.typography.titleSmall)
            if (enviosDelComunicado.isEmpty()) {
                Text("Sin registros de envío.", style = MaterialTheme.typography.bodySmall)
            }
            enviosDelComunicado.forEach { envio ->
                val apoderado = viewModel.apoderadoPorId(envio.apoderadoId)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(apoderado?.nombreCompleto ?: envio.apoderadoId, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${envio.canal.etiqueta} · ${envio.estado.etiqueta}",
                            color = if (envio.estado == EstadoEnvio.FALLIDO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
