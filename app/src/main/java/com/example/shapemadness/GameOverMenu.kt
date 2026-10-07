package com.example.shapemadness

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

// Gère l'affichage de l'écran de fin de partie avec ses boutons et ses animations
class GameOverMenu(
    private val activity: AppCompatActivity, // L'écran principal du jeu en cours
    parentLayout: ConstraintLayout, // La zone où le menu va se superposer
    private val onSettingsClicked: () -> Unit, // Le signal envoyé pour ouvrir les paramètres
) {
    private val view: View = LayoutInflater.from(activity).inflate(R.layout.layout_gameover, parentLayout, false) // Transforme le fichier XML en interface visible à l'écran

    init {
        parentLayout.addView(view) // Ajoute le menu au jeu mais le garde invisible pour l'instant
        view.visibility = View.INVISIBLE
        view.elevation = 100f // Force l'affichage tout au-dessus des autres éléments du jeu

        val btnRestart = view.findViewById<View>(R.id.btnRestart) // Utilise View générique pour supporter les boutons et les images
        val btnMenu = view.findViewById<View>(R.id.btnMenu)
        val btnSettings = view.findViewById<View>(R.id.btnSettings)

        scaleBtnOnClick(btnRestart) // Ajoute un effet visuel de rebond au clic
        scaleBtnOnClick(btnMenu)
        scaleBtnOnClick(btnSettings)

        btnRestart.setOnClickListener { // Gère l'action quand le joueur veut recommencer la partie
            AudioManager.stopDeathMusic() // Coupe la musique triste avant de relancer l'action

            val intent = Intent(activity, MainGameActivity::class.java) // Prépare le lancement d'une nouvelle partie toute neuve
            activity.startActivity(intent)

            activity.overridePendingTransition(R.anim.fade_from_black, R.anim.fade_to_black) // Applique un fondu au noir élégant entre les deux écrans
            activity.finish() // Détruit l'ancienne partie pour libérer la mémoire du téléphone
        }

        btnMenu.setOnClickListener { // Gère le retour au menu principal de sélection des modes
            AudioManager.stopDeathMusic()

            val intent = Intent(activity, SelectModeActivity::class.java) // Prépare le chargement de l'écran d'accueil
            activity.startActivity(intent)

            activity.overridePendingTransition(R.anim.fade_from_black, R.anim.fade_to_black) // Applique la même transition fluide
            activity.finish() // N'oublie pas de fermer l'activité de jeu en cours pour éviter les fuites de mémoire
        }

        btnSettings.setOnClickListener {
            onSettingsClicked() // Déclenche l'ouverture du menu des paramètres par-dessus le Game Over
        }
    }

    fun show() {
        view.alpha = 0f // Rend le menu totalement transparent pour préparer l'apparition
        view.translationY = 100f // Décale le menu vers le bas pour le faire remonter ensuite

        view.visibility = View.VISIBLE // Active l'affichage technique du menu même s'il est invisible
        view.bringToFront() // Sécurité supplémentaire pour le mettre au premier plan

        view.animate() // Lance le moteur d'animation pour faire apparaître les boutons
            .alpha(1f) // Fait passer le menu à 100% opaque progressivement
            .translationY(0f) // Le fait remonter à sa position d'origine
            .setDuration(1300L) // L'animation dure un peu plus d'une seconde pour bien marquer la défaite
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f)) // Ajoute un léger effet de rebond très satisfaisant à la fin du mouvement
            .start() // Démarre l'action visuelle
    }
}