package com.goldennova.upquest.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.toRoute
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.goldennova.upquest.presentation.alarmalert.AlarmAlertRoot
import com.goldennova.upquest.presentation.alarmdetail.AlarmDetailRoot
import com.goldennova.upquest.presentation.alarmlist.AlarmListRoot
import com.goldennova.upquest.presentation.photosetup.PhotoSetupRoot
import com.goldennova.upquest.presentation.settings.SettingsRoot

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AlarmList,
        modifier = modifier,
    ) {
        composable<AlarmList> {
            AlarmListRoot(
                onNavigateToNewAlarm = {
                    navController.navigate(AlarmDetail())
                },
                onNavigateToAlarm = { alarmId ->
                    navController.navigate(AlarmDetail(alarmId = alarmId))
                },
            )
        }

        composable<AlarmDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<AlarmDetail>()
            AlarmDetailRoot(
                alarmId = route.alarmId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPhotoSetup = { alarmId ->
                    navController.navigate(PhotoSetup(alarmId = alarmId))
                },
            )
        }

        composable<AlarmAlert> {
            AlarmAlertRoot(
                onDismiss = {
                    navController.popBackStack()
                },
            )
        }

        composable<PhotoSetup> { backStackEntry ->
            val route = backStackEntry.toRoute<PhotoSetup>()
            PhotoSetupRoot(
                alarmId = route.alarmId,
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable<Settings> {
            SettingsRoot(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}
