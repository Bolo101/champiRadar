# DermaVision 🔬

Application Android d'analyse locale de grains de beauté par intelligence artificielle. Aucune donnée ne quitte le téléphone.

> ⚠️ **Avertissement médical** : DermaVision est un outil de sensibilisation uniquement. Il ne remplace en aucun cas l'avis d'un dermatologue qualifié. Consultez un médecin pour tout diagnostic.

---

## Fonctionnalités

- Photographie d'un grain de beauté via la caméra du téléphone
- Analyse entièrement locale — aucun transfert de données vers un serveur
- Score de probabilité et verdict (Bénin / Suspect) en quelques secondes
- Disclaimer médical intégré à chaque résultat

---

## Stack technique

| Composant | Technologie |
|---|---|
| Langage | Kotlin |
| Interface | Jetpack Compose |
| Caméra | CameraX |
| Navigation | Navigation Compose |
| Chargement image | Coil |
| Inférence IA | TensorFlow Lite 2.5.0 |
| Architecture | MVVM — écrans passifs, callbacks |

---

## Modèle IA

Le modèle est un **EfficientNetB4** entraîné par transfer learning sur le dataset **HAM10000** (Human Against Machine with 10000 training images), contenant des images de lésions cutanées annotées par des dermatologues.

### Dataset

- Source : [HAM1000 Segmentation and Classification](https://www.kaggle.com/datasets/surajghuwalewala/ham1000-segmentation-and-classification)
- 10 015 images de lésions cutanées
- 7 classes regroupées en 2 catégories :

| Catégorie | Classes |
|---|---|
| **Suspect (1)** | MEL (mélanome), BCC (carcinome basocellulaire), AKIEC (kératose actinique) |
| **Bénin (0)** | NV, BKL, DF, VASC |

### Entraînement

```
Phase 1 — Tête seule (base model gelé)     : 15 epochs, lr=1e-3
Phase 2 — Fine-tuning complet               : 30 epochs, lr=1e-5
Phase 3 — Affinement final                  : 15 epochs, lr=5e-6
```

Techniques utilisées : class weights, data augmentation, EarlyStopping, ReduceLROnPlateau.

### Performances

| Métrique | Valeur |
|---|---|
| AUC | 0.923 |
| Sensibilité suspects | 93.9% |
| Seuil de décision | 0.35 |
| Taille modèle | ~75 MB |

### Export

Le modèle est exporté en TFLite avec `SELECT_TF_OPS` pour supporter les opérations avancées d'EfficientNetB4 sur Android.

---

## Architecture du projet

```
app/src/main/
├── assets/
│   └── dermavision.tflite          ← modèle IA
├── java/com/bolo101/dermavision/
│   ├── MainActivity.kt             ← navigation principale
│   ├── ml/
│   │   ├── Classifier.kt           ← pipeline d'inférence
│   │   └── ClassificationResult.kt ← modèle de résultat
│   └── screens/
│       ├── HomeScreen.kt           ← écran d'accueil
│       ├── CameraScreen.kt         ← capture photo
│       └── ResultScreen.kt         ← affichage résultat
└── AndroidManifest.xml
```

---

## Prérequis

- Android 8.0+ (API 26 minimum)
- Caméra arrière requise

---

## Installation

### Depuis les sources

```bash
git clone https://github.com/bolo101/DermaVision.git
cd DermaVision
```

Ouvre le projet dans **Android Studio**, connecte un téléphone Android ou lance un émulateur, puis appuie sur ▶.

### Dépendances principales

```kotlin
// build.gradle.kts
implementation("androidx.navigation:navigation-compose:2.7.7")
implementation("androidx.camera:camera-core:1.3.4")
implementation("androidx.camera:camera-view:1.3.4")
implementation("io.coil-kt:coil-compose:2.6.0")
implementation("org.tensorflow:tensorflow-lite:2.5.0")
implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.5.0")
```

---

## Réentraîner le modèle

Le notebook d'entraînement est disponible dans `/notebook/DermaVisionIAv2.ipynb`.

Ouvre-le dans [Google Colab](https://colab.research.google.com), active le GPU T4 (**Runtime → Change runtime type → T4 GPU**) et exécute les cellules dans l'ordre.

Durée estimée : ~2h sur GPU T4 gratuit.

---

## Flux de l'application

```
Accueil
  └── [Commencer l'analyse]
        └── Caméra (aperçu live CameraX)
              └── [Capture]
                    └── Résultat
                          ├── Photo analysée
                          ├── Score de probabilité + barre
                          ├── Verdict : Bénin / Suspect
                          └── [Nouvelle analyse] → Accueil
```

---

## Avertissement légal

DermaVision est un projet éducatif et expérimental. Il ne constitue pas un dispositif médical certifié. Les résultats fournis ne doivent pas être utilisés comme base de décision médicale. En cas de doute sur une lésion cutanée, consultez impérativement un dermatologue.

---

## Licence

MIT License — voir [LICENSE](LICENSE)