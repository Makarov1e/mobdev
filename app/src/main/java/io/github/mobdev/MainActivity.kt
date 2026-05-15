package io.github.mobdev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import io.github.mobdev.navigation.AppNavHost
import io.github.mobdev.ui.AppViewModel
import io.github.mobdev.ui.theme.MobdevTheme

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobdevTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController, appViewModel = appViewModel)
            }
        }
    }
}
