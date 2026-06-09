package com.multica.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.multica.app.di.ServiceLocator
import com.multica.app.ui.dashboard.DashboardScreen
import com.multica.app.ui.settings.SettingsScreen
import com.multica.app.ui.viewmodel.DashboardViewModel
import com.multica.app.ui.viewmodel.SettingsViewModel

object Routes {
    const val Dashboard = "dashboard"
    const val Settings = "settings"
}

@Composable
fun MulticaNav() {
    val nav: NavHostController = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.Dashboard) {
        composable(Routes.Dashboard) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val vm: DashboardViewModel = viewModel {
                // v0.3.20: 传 Application 用于播放状态变化音
                DashboardViewModel(ctx.applicationContext as android.app.Application, ServiceLocator.repo, ServiceLocator.settings, ServiceLocator.net)
            }
            // 首次进入触发一次 refresh
            androidx.compose.runtime.LaunchedEffect(Unit) { vm.refresh() }
            DashboardScreen(
                vm = vm,
                onOpenSettings = { nav.navigate(Routes.Settings) }
            )
        }
        composable(Routes.Settings) {
            val vm: SettingsViewModel = viewModel {
                SettingsViewModel(ServiceLocator.settings, ServiceLocator.repo)
            }
            SettingsScreen(
                vm = vm,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
