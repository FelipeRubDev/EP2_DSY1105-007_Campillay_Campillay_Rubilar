package com.mpaznotifica.app.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.mpaznotifica.app.navigation.Rutas
import com.mpaznotifica.app.ui.components.BarraSuperior
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

/**
 * Panel principal después del login: perfil activo, resumen y acceso a cada módulo.
 *
 * TODO(equipo): decidir con el CITT si la configuración de apoderados es solo para Inspectoría
 * General, y en ese caso ocultar ese botón al Profesor/a Jefe.
 */
@Composable
fun PanelScreen(
    navController: NavHostController,
    viewModel: MpazNotificaViewModel,
    onCerrarSesion: () -> Unit
) {
    val usuario by viewModel.usuarioActivo.collectAsState()
    val comunicados by viewModel.comunicados.collectAsState()

    val context = LocalContext.current
    val permisoNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(topBar = { BarraSuperior("MPAZ Notifica", navController, onCerrarSesion) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Hola, ${usuario?.nombre.orEmpty()}", style = MaterialTheme.typography.titleLarge)

            Card(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Perfil: ${usuario?.perfil?.etiqueta.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                    usuario?.cursoAsignado?.let {
                        Text("Curso de jefatura: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    AnimatedContent(targetState = comunicados.size, label = "contadorComunicados") { total ->
                        Text("Comunicados en la bitácora: $total", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Button(modifier = Modifier.fillMaxWidth(), onClick = { navController.navigate(Rutas.REDACTAR) }) {
                Text("Redactar comunicado")
            }
            Button(modifier = Modifier.fillMaxWidth(), onClick = { navController.navigate(Rutas.HISTORIAL) }) {
                Text("Historial de comunicados")
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { navController.navigate(Rutas.APODERADOS) }) {
                Text("Apoderados y canal preferente")
            }
        }
    }
}
