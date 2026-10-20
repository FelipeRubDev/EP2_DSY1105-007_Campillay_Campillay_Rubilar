package com.mpaznotifica.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mpaznotifica.app.data.local.AppDatabase
import com.mpaznotifica.app.data.repository.ComunicacionRepository
import com.mpaznotifica.app.navigation.AppNavigation
import com.mpaznotifica.app.ui.theme.MpazNotificaTheme
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MpazNotificaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Inicializamos la base de datos Room y el Repositorio
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ComunicacionRepository(database.comunicacionDao())

        // 2. Conectamos el Repositorio con el ViewModel
        viewModel.inicializarRepository(repository)

        setContent {
            MpazNotificaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Si tu AppNavigation acepta viewModel por parámetro, lo pasa aquí.
                    AppNavigation()
                }
            }
        }
    }
}