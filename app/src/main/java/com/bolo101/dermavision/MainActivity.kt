package com.bolo101.dermavision

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bolo101.dermavision.screens.CameraScreen
import com.bolo101.dermavision.ui.theme.screens.HomeScreen
import com.bolo101.dermavision.ui.theme.screens.ResultScreen
import com.bolo101.dermavision.ui.theme.DermaVisionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DermaVisionTheme {
                DermaVisionApp()
            }
        }
    }
}

@Composable
fun DermaVisionApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                onStartAnalysis = { navController.navigate("camera") }
            )
        }

        composable("camera") {
            CameraScreen(
                onPhotoTaken = { uri ->
                    // Uri.encode() encode les caractères spéciaux de l'URI
                    // pour qu'elle puisse voyager dans une route de navigation
                    val encoded = Uri.encode(uri.toString())
                    navController.navigate("result/$encoded")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // {imageUri} est un paramètre dynamique dans la route
        composable(
            route = "result/{imageUri}",
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            // On récupère et décode l'URI depuis les arguments de navigation
            val encoded = backStackEntry.arguments?.getString("imageUri") ?: ""
            val imageUri = if (encoded.isNotEmpty()) Uri.parse(Uri.decode(encoded)) else null

            ResultScreen(
                imageUri = imageUri,
                onNewAnalysis = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}