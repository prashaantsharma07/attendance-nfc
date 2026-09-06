package com.attendance.nfc.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.attendance.nfc.data.AttendanceRepository
import com.attendance.nfc.ui.screens.ClassDetailScreen
import com.attendance.nfc.ui.screens.ClassListScreen
import com.attendance.nfc.ui.screens.RegistrationScreen
import com.attendance.nfc.ui.screens.ScanScreen

sealed class Screen(val route: String) {
    object ClassList : Screen("class_list")
    object ClassDetail : Screen("class_detail/{classId}") {
        fun createRoute(classId: Long) = "class_detail/$classId"
    }
    object Scan : Screen("scan/{classId}") {
        fun createRoute(classId: Long) = "scan/$classId"
    }
    object Registration : Screen("register/{classId}?rfid={rfid}&barcode={barcode}") {
        fun createRoute(classId: Long, rfid: String? = null, barcode: String? = null): String {
            val rfidParam = rfid?.let { Uri.encode(it) }.orEmpty()
            val barcodeParam = barcode?.let { Uri.encode(it) }.orEmpty()
            return "register/$classId?rfid=$rfidParam&barcode=$barcodeParam"
        }
    }
}

@Composable
fun AppNavGraph(
    repository: AttendanceRepository,
    nfcAvailable: Boolean,
    isNfcEnabled: () -> Boolean = { true },
    onOpenNfcSettings: () -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.ClassList.route) {
        composable(Screen.ClassList.route) {
            ClassListScreen(
                repository = repository,
                onClassClick = { classId ->
                    navController.navigate(Screen.ClassDetail.createRoute(classId))
                }
            )
        }

        composable(
            route = Screen.ClassDetail.route,
            arguments = listOf(navArgument("classId") { type = NavType.LongType })
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getLong("classId") ?: return@composable
            ClassDetailScreen(
                classId = classId,
                repository = repository,
                nfcAvailable = nfcAvailable,
                isNfcEnabled = isNfcEnabled,
                onOpenNfcSettings = onOpenNfcSettings,
                onBack = { navController.popBackStack() },
                onMarkAttendance = {
                    navController.navigate(Screen.Scan.createRoute(classId))
                }
            )
        }

        composable(
            route = Screen.Scan.route,
            arguments = listOf(navArgument("classId") { type = NavType.LongType })
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getLong("classId") ?: return@composable
            ScanScreen(
                classId = classId,
                repository = repository,
                nfcAvailable = nfcAvailable,
                isNfcEnabled = isNfcEnabled,
                onOpenNfcSettings = onOpenNfcSettings,
                onBack = { navController.popBackStack() },
                onUnknownCard = { rfid ->
                    navController.navigate(Screen.Registration.createRoute(classId = classId, rfid = rfid))
                },
                onUnknownBarcode = { barcode ->
                    navController.navigate(Screen.Registration.createRoute(classId = classId, barcode = barcode))
                }
            )
        }

        composable(
            route = Screen.Registration.route,
            arguments = listOf(
                navArgument("classId") { type = NavType.LongType },
                navArgument("rfid") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("barcode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getLong("classId") ?: return@composable
            val rfid = backStackEntry.arguments?.getString("rfid")?.takeIf { it.isNotBlank() }
            val barcode = backStackEntry.arguments?.getString("barcode")?.takeIf { it.isNotBlank() }
            RegistrationScreen(
                classId = classId,
                initialRfid = rfid,
                initialBarcode = barcode,
                repository = repository,
                onBack = { navController.popBackStack() },
                onRegistrationComplete = {
                    // Go back to scan screen after registering
                    navController.popBackStack()
                }
            )
        }
    }
}
