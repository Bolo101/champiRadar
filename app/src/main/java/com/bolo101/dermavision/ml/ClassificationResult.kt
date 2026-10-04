package com.bolo101.dermavision.ml

// Contient le résultat brut de l'inférence
data class ClassificationResult(
    val score: Float,        // probabilité brute [0.0 → 1.0]
    val isSuspect: Boolean   // true si score >= THRESHOLD
) {
    // Pourcentage affiché à l'utilisateur
    val confidencePercent: Int get() = (score * 100).toInt()

    // Libellé lisible
    val label: String get() = if (isSuspect) "Suspect" else "Bénin"
}