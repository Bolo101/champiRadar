package com.bolo101.dermavision.ui.theme.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bolo101.dermavision.ml.Classifier
import com.bolo101.dermavision.ml.ClassificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── États possibles de l'analyse ───────────────────────────────
sealed class AnalysisState {
    object Loading                             : AnalysisState()
    data class Success(val result: ClassificationResult) : AnalysisState()
    data class Error(val message: String)      : AnalysisState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    imageUri: Uri?,
    onNewAnalysis: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // mutableStateOf = variable réactive qui redessine l'UI à chaque changement
    var state by remember { mutableStateOf<AnalysisState>(AnalysisState.Loading) }

    // LaunchedEffect = bloc coroutine lancé automatiquement quand l'écran apparaît
    // Dispatchers.IO = s'exécute sur un thread de fond (jamais bloquer l'UI)
    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    android.util.Log.d("DermaVision", "Étape 1 — URI reçue : $imageUri")
                    val classifier = Classifier(context)
                    android.util.Log.d("DermaVision", "Étape 2 — Modèle chargé")
                    val result = classifier.classify(imageUri)
                    android.util.Log.d("DermaVision", "Étape 3 — Score : ${result.score}")
                    classifier.close()
                    state = AnalysisState.Success(result)
                } catch (e: Exception) {
                    android.util.Log.e("DermaVision", "ERREUR COMPLETE", e)
                    state = AnalysisState.Error("${e.javaClass.simpleName}: ${e.message}")
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        TopAppBar(
            title = { Text("Résultat") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Photo analysée ─────────────────────────────────
            AsyncImage(
                model = imageUri,
                contentDescription = "Photo du grain de beauté",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            // ── Card résultat — s'adapte à l'état ─────────────
            when (val s = state) {

                // Analyse en cours → spinner
                is AnalysisState.Loading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Analyse en cours...", fontSize = 16.sp)
                        }
                    }
                }

                // Résultat disponible → verdict + score + barre
                is AnalysisState.Success -> {
                    val result = s.result
                    val cardColor = if (result.isSuspect)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (result.isSuspect) "Suspect" else "Bénin",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Score de probabilité : ${result.confidencePercent}%",
                                fontSize = 15.sp
                            )

                            // Barre de progression visuelle du score
                            LinearProgressIndicator(
                                progress = { result.score },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = if (result.isSuspect)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = if (result.isSuspect)
                                    "Cette lésion présente des caractéristiques suspectes. " +
                                            "Consultez rapidement un dermatologue."
                                else
                                    "Cette lésion ne présente pas de caractéristiques " +
                                            "suspectes selon le modèle.",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Erreur → affichage du message
                is AnalysisState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Erreur d'analyse",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(text = s.message, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNewAnalysis,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Nouvelle analyse")
            }

            Text(
                text = "Cet outil ne remplace pas un avis médical. " +
                        "Consultez un dermatologue pour tout diagnostic.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}