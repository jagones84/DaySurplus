package com.example.startapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.startapp.ui.screen.ChartScreen
import com.example.startapp.ui.screen.CounterScreen
import com.example.startapp.ui.screen.SettingsScreen
import com.example.startapp.ui.viewmodel.ChartViewModel
import com.example.startapp.ui.viewmodel.ChartViewModelFactory
import com.example.startapp.ui.viewmodel.CounterViewModel
import com.example.startapp.ui.viewmodel.CounterViewModelFactory
import com.example.startapp.worker.SurplusWorker

class MainActivity : ComponentActivity() {

    private val counterViewModel: CounterViewModel by viewModels { CounterViewModelFactory(application) }
    private val chartViewModel: ChartViewModel by viewModels { ChartViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the unique chain of surplus workers, keeping it if it already exists.
        // The worker itself is idempotent per-day, so an immediate catch-up run
        // after a chain interruption cannot double-apply the daily increase.
        val workRequest = OneTimeWorkRequestBuilder<SurplusWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "SurplusWorkerChain",
            ExistingWorkPolicy.KEEP,
            workRequest
        )

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.nav_counter)) },
                                label = { Text(stringResource(R.string.nav_counter)) },
                                selected = currentDestination?.route == "counter",
                                onClick = {
                                    navController.navigate("counter") {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.nav_chart)) },
                                label = { Text(stringResource(R.string.nav_chart)) },
                                selected = currentDestination?.route == "chart",
                                onClick = {
                                    navController.navigate("chart") {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                                label = { Text(stringResource(R.string.nav_settings)) },
                                selected = currentDestination?.route == "settings",
                                onClick = {
                                    navController.navigate("settings") {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "counter",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("counter") { CounterScreen(viewModel = counterViewModel) }
                        composable("chart") { ChartScreen(viewModel = chartViewModel) }
                        composable("settings") { SettingsScreen(viewModel = counterViewModel) }
                    }
                }
            }
        }
    }
}
