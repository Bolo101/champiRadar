package com.bolo101.dermavision.ml

data class ClassificationResult(
    val genusLabel: String,       // "Cantharellus" (nom scientifique)
    val frenchName: String,       // "Girolle / Chanterelle"
    val isEdible: Boolean,        // true = potentiellement comestible
    val edibilityLabel: String,   // "Comestible", "Toxique", "Non recommandé"
    val confidence: Float         // score de confiance [0,1]
) {
    val confidencePercent: Int get() = (confidence * 100).toInt()
}