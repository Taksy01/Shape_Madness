package com.example.shapemadness

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible

// Gère l'affichage des options et la sauvegarde des préférences utilisateur
class SettingsMenu(
    private val activity: AppCompatActivity, // L'écran qui accueille le menu
    parentLayout: ConstraintLayout // La zone de jeu pour l'insertion du layout
) {
    private val view: View = LayoutInflater.from(activity).inflate(R.layout.layout_settings, parentLayout, false) // Transforme le XML en objet vue
    private val backgroundDim: View = view.findViewById(R.id.settingsBackgroundDim) // Le fond sombre derrière la fenêtre
    private val settingsContainer: View = view.findViewById(R.id.CreditsContainer) // Le conteneur principal des options
    private val switchInvert: SwitchCompat = view.findViewById(R.id.switchInvert) // Le bouton à glissière pour l'inversion

    val isShowing: Boolean get() = view.isVisible // Indique si le menu est actuellement affiché à l'écran
    private var isAnimating = false // Verrou de sécurité pour éviter de lancer deux animations en même temps

    init {
        parentLayout.addView(view) // Ajoute le menu au-dessus du reste du jeu
        view.visibility = View.INVISIBLE
        view.elevation = 150f // Force l'affichage au premier plan absolu

        val btnClose = view.findViewById<View>(R.id.btnClose) // Trouve le bouton de fermeture

        val isSavedInverted = SaveManager.getInvertRotation(activity) // Récupère la valeur enregistrée ou utilise "faux" par défaut

        switchInvert.isChecked = isSavedInverted // Aligne l'interrupteur visuel sur la sauvegarde
        GameManager.invertRotation = isSavedInverted // Synchronise le moteur de jeu dès le lancement

        switchInvert.setOnCheckedChangeListener { _, isChecked -> // Déclenche l'action quand le joueur touche au bouton
            SaveManager.saveInvertRotation(activity, isChecked) // Enregistre immédiatement le nouveau choix dans le téléphone
            GameManager.invertRotation = isChecked // Met à jour la direction de visée pour tous les canons
        }

        btnClose.setOnClickListener { hide() } // Lance l'animation de sortie quand on clique sur la croix
    }

    // Déclenche l'animation de montée du menu depuis le bas de l'écran
    fun show() {
        if (isAnimating || isShowing) return // Bloque l'action si le menu bouge déjà ou s'il est déjà là
        isAnimating = true

        if (switchInvert.isChecked != GameManager.invertRotation) { // S'assure que le bouton est toujours raccord avec le moteur de jeu
            switchInvert.isChecked = GameManager.invertRotation
        }

        val screenHeight = view.context.resources.displayMetrics.heightPixels.toFloat() // Récupère la hauteur totale de l'écran
        view.visibility = View.VISIBLE
        view.bringToFront() // S'assure de passer devant les autres menus potentiels

        backgroundDim.alpha = 0f // Prépare l'apparition progressive du fond noir
        settingsContainer.translationY = screenHeight // Place le menu tout en bas hors de l'écran

        backgroundDim.animate().alpha(1f).setDuration(400).start() // Fait apparaître le voile sombre en moins d'une demi-seconde

        settingsContainer.animate()
            .translationY(0f) // Fait remonter le menu à sa place normale
            .setDuration(450)
            .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f)) // Ajoute un effet de freinage fluide sur la fin
            .withEndAction {
                isAnimating = false // Libère le verrou une fois le mouvement terminé
            }
            .start()
    }

    // Déclenche l'animation de descente pour cacher le menu
    fun hide() {
        if (isAnimating || !isShowing) return // Évite de cacher un menu qui n'existe pas ou qui bouge déjà
        isAnimating = true

        val screenHeight = view.context.resources.displayMetrics.heightPixels.toFloat()

        backgroundDim.animate().alpha(0f).setDuration(300).start() // Efface le fond noir doucement

        settingsContainer.animate()
            .translationY(screenHeight) // Fait glisser le menu vers le bas pour le sortir de la vue
            .setDuration(300)
            .setInterpolator(android.view.animation.AccelerateInterpolator(1.5f)) // Accélère la chute pour un effet plus naturel
            .withEndAction {
                view.visibility = View.INVISIBLE // Masque techniquement la vue une fois l'animation finie
                isAnimating = false // Permet de rouvrir le menu plus tard
            }
            .start()
    }
}