package com.goldennova.upquest.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
            // PhotoSetup에서 전달한 사진 경로를 NavBackStackEntry.savedStateHandle로 수신
            val photoPathResult by backStackEntry.savedStateHandle
                .getStateFlow<String?>("referencePhotoPath", null)
                .collectAsStateWithLifecycle()
            AlarmDetailRoot(
                alarmId = route.alarmId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPhotoSetup = { alarmId ->
                    navController.navigate(PhotoSetup(alarmId = alarmId))
                },
                photoPathResult = photoPathResult,
                onPhotoPathResultConsumed = {
                    backStackEntry.savedStateHandle["referencePhotoPath"] = null
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
                onNavigateBackWithPath = { path ->
                    // 이전 백스택(AlarmDetail)의 SavedStateHandle에 사진 경로 전달
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("referencePhotoPath", path)
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
