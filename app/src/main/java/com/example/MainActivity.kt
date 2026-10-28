package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.LanguagePreferences
import com.example.ui.navigation.BottomNavBar
import com.example.ui.navigation.Screen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.ComingSoonScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LinkVerifyScreen
import com.example.ui.screens.LiveNewsScreen
import com.example.ui.screens.MessageVerifyScreen
import com.example.ui.screens.OcrVerifyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VerifyNewsScreen
import com.example.ui.theme.TruthGuardTheme
import com.example.ui.viewmodel.FactCheckViewModel
import com.example.util.LocaleHelper

class MainActivity : ComponentActivity() {

    private val viewModel: FactCheckViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val prefs = LanguagePreferences(newBase)
        val context = LocaleHelper.applyLocale(newBase, prefs.languageCode)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val appLanguage by viewModel.appLanguage.collectAsState()
            val context = LocalContext.current

            val localizedContext = remember(appLanguage, context) {
                LocaleHelper.wrapContextForCompose(context, appLanguage)
            }
            val localizedConfiguration = remember(appLanguage, localizedContext) {
                localizedContext.resources.configuration
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                TruthGuardTheme(themeMode = themeMode) {
                    TruthGuardApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun TruthGuardApp(viewModel: FactCheckViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Splash.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(Screen.Dashboard.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigate = { screen -> navController.navigate(screen.route) },
                    onSelectVerification = { result ->
                        viewModel.verifyText(result.claimTitle, result.inputType)
                        navController.navigate(Screen.VerifyNews.route)
                    }
                )
            }

            composable(Screen.VerifyNews.route) {
                VerifyNewsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.OcrVerify.route) {
                OcrVerifyScreen(
                    viewModel = viewModel,
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            composable(Screen.LinkVerify.route) {
                LinkVerifyScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.MessageVerify.route) {
                MessageVerifyScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AiAssistant.route) {
                AiAssistantScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.LiveNews.route) {
                LiveNewsScreen(
                    viewModel = viewModel,
                    onVerifyArticle = { claimText ->
                        viewModel.verifyText(claimText, "NEWS_TEXT")
                        navController.navigate(Screen.VerifyNews.route)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel = viewModel,
                    onSelectVerification = { result ->
                        viewModel.verifyText(result.claimTitle, result.inputType)
                        navController.navigate(Screen.VerifyNews.route)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ComingSoon.route) {
                ComingSoonScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
