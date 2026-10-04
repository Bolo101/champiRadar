package com.bolo101.dermavision.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onPhotoTaken: (Uri) -> Unit,  // on retourne maintenant l'URI de la photo
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── Gestion de la permission ───────────────────────────────────────────

    // mutableStateOf = variable réactive : quand elle change, Compose redessine l'interface
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    // rememberLauncherForActivityResult = lance une demande de permission système
    // et récupère le résultat (accordée ou refusée) dans le callback
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // LaunchedEffect(Unit) = exécuté une seule fois au premier affichage de l'écran
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ── Référence à l'objet de capture ────────────────────────────────────
    // null au départ, sera rempli quand la caméra sera prête
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    // ── Interface ─────────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize()) {

        TopAppBar(
            title = { Text("Photographier") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Retour")
                }
            }
        )

        // Affiche la caméra si permission OK, sinon un écran explicatif
        if (hasCameraPermission) {

            // ── Aperçu caméra en temps réel ───────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // AndroidView = passerelle entre Compose et les vues Android classiques
                // PreviewView est une vue Android (pas un Composable), d'où AndroidView
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)

                        // ProcessCameraProvider gère le cycle de vie de la caméra
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            // Preview = ce qu'on voit en temps réel à l'écran
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            // ImageCapture = ce qui sera utilisé pour prendre la photo
                            val imageCaptureUseCase = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            // On stocke la référence pour l'utiliser dans le bouton
                            imageCapture = imageCaptureUseCase

                            try {
                                // Détache tous les cas d'usage précédents
                                cameraProvider.unbindAll()
                                // Attache Preview + ImageCapture à la caméra arrière
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCaptureUseCase
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

        } else {

            // ── Écran de permission refusée ────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Permission caméra requise",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "DermaVision a besoin d'accéder à votre caméra " +
                                "pour photographier le grain de beauté.",
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Text("Autoriser la caméra")
                    }
                }
            }
        }

        // ── Bouton déclencheur ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    // Crée un fichier dans le cache interne de l'app
                    val photoFile = File(
                        context.cacheDir,
                        "mole_${
                            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                .format(Date())
                        }.jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions
                        .Builder(photoFile)
                        .build()

                    imageCapture?.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                // Photo sauvegardée → on retourne l'URI au NavController
                                onPhotoTaken(Uri.fromFile(photoFile))
                            }
                            override fun onError(exc: ImageCaptureException) {
                                exc.printStackTrace()
                            }
                        }
                    )
                },
                enabled = hasCameraPermission,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = Color.White,
                    border = ButtonDefaults.outlinedButtonBorder
                ) {}
            }
        }
    }
}