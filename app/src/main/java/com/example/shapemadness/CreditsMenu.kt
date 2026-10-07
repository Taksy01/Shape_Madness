package com.example.shapemadness

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible

// Gère l'apparition et la disparition du menu des crédits avec des animations fluides
class CreditsMenu(
    private val activity: AppCompatActivity,
    private val parentLayout: ConstraintLayout
) {
    private val view: View = LayoutInflater.from(activity).inflate(R.layout.layout_credits, parentLayout, false) // Transforme le fichier XML en objet affichable

    val isShowing: Boolean get() = view.isVisible // Un raccourci pratique pour savoir si le menu est visible

    private var isAnimating = false // Une sécurité pour empêcher de spammer le bouton et de faire bugger l'animation

    private val backgroundDim: View = view.findViewById(R.id.settingsBackgroundDim) // Le fond noir transparent qui assombrit le jeu

    private val creditsContainer: View = view.findViewById(R.id.CreditsContainer) // La boîte principale qui contient le texte

    init {
        parentLayout.addView(view) // Ajoute le menu par-dessus le jeu mais le garde invisible au démarrage
        view.visibility = View.GONE

        view.elevation = 200f // Force le menu à s'afficher tout au-dessus du reste comme un calque

        // Prépare le bouton de fermeture pour qu'il réagisse au clic
        val btnClose = view.findViewById<View>(R.id.btnClose)
        btnClose?.let {
            scaleBtnOnClick(it) // Fait le petit effet de rebond quand on appuie dessus
            it.setOnClickListener { hide() }
        }
    }

    // Fait monter le menu depuis le bas de l'écran
    fun show() {
        if (isAnimating || isShowing) return // Bloque tout si le menu est déjà là ou en train de bouger
        isAnimating = true

        val screenHeight = view.context.resources.displayMetrics.heightPixels.toFloat() // Calcule la taille de l'écran pour le point de départ

        view.visibility = View.VISIBLE

        view.bringToFront() // S'assure encore une fois qu'il est bien au premier plan

        // Prépare les éléments avant l'animation avec le fond invisible et la boîte tout en bas
        backgroundDim.alpha = 0f
        creditsContainer.translationY = screenHeight

        backgroundDim.animate().alpha(1f).setDuration(400).start() // Fait apparaître doucement le fond sombre

        // Fait glisser la boîte vers le haut jusqu'à sa place normale
        creditsContainer.animate()
            .translationY(0f)
            .setDuration(450)
            .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f)) // Ralentit l'animation sur la fin pour faire plus naturel
            .withEndAction { isAnimating = false } // Libère la sécurité une fois l'animation finie
            .start()
    }

    // Fait retomber le menu vers le bas pour le ranger
    fun hide() {
        if (isAnimating || !isShowing) return // Bloque si le menu est déjà caché ou en mouvement
        isAnimating = true

        val screenHeight = view.context.resources.displayMetrics.heightPixels.toFloat()

        backgroundDim.animate().alpha(0f).setDuration(300).start() // Fait disparaître le fond sombre

        // Fait tomber la boîte en dehors de l'écran par le bas
        creditsContainer.animate()
            .translationY(screenHeight)
            .setDuration(350)
            .setInterpolator(android.view.animation.AccelerateInterpolator(1.5f)) // Accélère la chute pour simuler la gravité
            .withEndAction {
                view.visibility = View.GONE // Cache totalement la vue une fois en bas pour soulager le téléphone
                isAnimating = false
            }
            .start()
    }
}