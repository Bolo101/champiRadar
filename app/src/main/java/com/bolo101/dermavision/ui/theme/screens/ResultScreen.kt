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
import androidx.compose.ui.graphics.Color
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

sealed class AnalysisState {
    object Loading                                        : AnalysisState()
    data class Success(val result: ClassificationResult)  : AnalysisState()
    data class Error(val message: String)                 : AnalysisState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    imageUri: Uri?,
    onNewAnalysis: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<AnalysisState>(AnalysisState.Loading) }

    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val classifier = Classifier(context)
                    val result     = classifier.classify(imageUri)
                    classifier.close()
                    state = AnalysisState.Success(result)
                } catch (e: Exception) {
                    android.util.Log.e("MycoVision", "Erreur", e)
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
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Photo
            AsyncImage(
                model = imageUri,
                contentDescription = "Photo du champignon",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            when (val s = state) {

                is AnalysisState.Loading -> {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Identification en cours...", fontSize = 16.sp)
                        }
                    }
                }

                is AnalysisState.Success -> {
                    val result = s.result
                    val cardColor = when {
                        result.genusLabel == "Inconnu"  -> MaterialTheme.colorScheme.surfaceVariant
                        result.isEdible                 -> MaterialTheme.colorScheme.secondaryContainer
                        else                            -> MaterialTheme.colorScheme.errorContainer
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Nom français
                            Text(
                                text = result.frenchName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            // Nom scientifique
                            if (result.genusLabel != "Inconnu") {
                                Text(
                                    text = result.genusLabel,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Comestibilité
                            Text(
                                text = result.edibilityLabel,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (result.isEdible)
                                    Color(0xFF2E7D32)   // vert foncé
                                else
                                    MaterialTheme.colorScheme.error
                            )

                            // Barre de confiance
                            Text(
                                text = "Confiance : ${result.confidencePercent}%",
                                fontSize = 13.sp
                            )
                            LinearProgressIndicator(
                                progress = { result.confidence },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )

                            // Avertissement si confiance faible
                            if (result.confidence < 0.6f) {
                                Text(
                                    text = "Confiance faible — identification incertaine.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                is AnalysisState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Erreur d'analyse", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(text = s.message, fontSize = 11.sp)
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
                Text("Nouvelle identification")
            }

            Text(
                text = "Ne consommez jamais un champignon sur la seule base " +
                       "de cette application. Consultez un mycologue expert.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}