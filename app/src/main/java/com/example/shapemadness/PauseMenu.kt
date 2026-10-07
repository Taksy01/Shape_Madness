package com.example.shapemadness

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

// Gère l'interface de pause pour arrêter le jeu et accéder aux options
class PauseMenu(
    private val activity: AppCompatActivity, // L'écran de jeu actuel
    parentLayout: ConstraintLayout, // La zone où le menu vient se superposer
    private val onResumeClicked: () -> Unit, // Le signal envoyé pour reprendre la partie
    private val onSettingsClicked: () -> Unit, // Le signal envoyé pour ouvrir les paramètres
) {
    // Transforme le fichier XML de mise en page en un véritable objet visuel
    private val view: View = LayoutInflater.from(activity).inflate(R.layout.layout_pause, parentLayout, false)

    init {
        parentLayout.addView(view) // Ajoute le menu à l'écran mais le garde invisible au début
        view.visibility = View.INVISIBLE
        view.elevation = 100f // Place le menu tout au-dessus du jeu pour qu'il soit bien visible

        val btnResume = view.findViewById<ImageView>(R.id.btnRestart) // Trouve le bouton pour reprendre
        val btnSettings = view.findViewById<ImageView>(R.id.btnSettings) // Trouve le bouton des options
        val btnMenu = view.findViewById<ImageView>(R.id.btnMenu) // Trouve le bouton pour quitter

        scaleBtnOnClick(btnResume) // Ajoute l'effet de rebond au clic sur le bouton reprendre
        scaleBtnOnClick(btnSettings) // Ajoute l'effet de rebond au clic sur le bouton paramètres
        scaleBtnOnClick(btnMenu) // Ajoute l'effet de rebond au clic sur le bouton menu

        btnResume.setOnClickListener { onResumeClicked() } // Déclenche la reprise du jeu au clic

        btnSettings.setOnClickListener { onSettingsClicked() } // Ouvre les réglages par-dessus la pause au clic

        btnMenu.setOnClickListener { // Gère le retour à l'écran de sélection des modes
            val intent = Intent(activity, SelectModeActivity::class.java)
            activity.startActivity(intent)

            activity.overridePendingTransition(R.anim.fade_from_black, R.anim.fade_to_black) // Applique un fondu au noir fluide
        }
    }

    // Rend le menu de pause visible et le place au premier plan
    fun show() {
        view.visibility = View.VISIBLE
        view.bringToFront() // S'assure que rien ne passe devant les boutons de pause
    }

    // Cache le menu pour laisser place à l'action du jeu
    fun hide() {
        view.visibility = View.INVISIBLE
    }
}