package com.groqtap

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.groqtap.service.FloatingWidgetService
import com.groqtap.ui.*

class MainActivity : ComponentActivity() {

    private val app get() = application as App

    private val chatViewModel by viewModels<ChatViewModel> {
        ChatViewModel.Factory(app.prefs, app.groqApi)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GroqTapTheme {
                GroqTapNavHost(
                    chatViewModel = chatViewModel,
                    onWidgetToggle = { enabled -> handleWidgetToggle(enabled) },
                    prefs = app.prefs,
                )
            }
        }
    }

    // Handle widget toggle — start or stop the FloatingWidgetService
    private fun handleWidgetToggle(enabled: Boolean) {
        val intent = Intent(this, FloatingWidgetService::class.java)
        if (enabled) startForegroundService(intent) else stopService(intent)
    }
}

// ─────────────── Nav host ───────────────

private const val ROUTE_CHAT     = "chat"
private const val ROUTE_SETTINGS = "settings"

@Composable
private fun GroqTapNavHost(
    chatViewModel: ChatViewModel,
    onWidgetToggle: (Boolean) -> Unit,
    prefs: com.groqtap.data.Prefs,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_CHAT,
        enterTransition  = { slideInHorizontally { it } + fadeIn() },
        exitTransition   = { slideOutHorizontally { -it / 3 } + fadeOut() },
        popEnterTransition  = { slideInHorizontally { -it } + fadeIn() },
        popExitTransition   = { slideOutHorizontally { it } + fadeOut() },
    ) {
        composable(ROUTE_CHAT) {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = { navController.navigate(ROUTE_SETTINGS) },
            )
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                prefs = prefs,
                onNavigateBack = { navController.popBackStack() },
                onWidgetToggle = onWidgetToggle,
            )
        }
    }
}
