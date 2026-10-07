package com.example.shapemadness

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.cos
import kotlin.math.sin

// Gère le diamant qui tombe des ennemis et vole vers le compteur d'argent
class Diamond(
    context: Context, // Contexte pour créer l'image à l'écran
    private val parentLayout: ConstraintLayout, // La zone de jeu où le diamant apparaît
    startX: Float, // Position horizontale de départ sur le cadavre du monstre
    startY: Float, // Position verticale de départ sur le cadavre du monstre
    private val onCollected: () -> Unit // Signal envoyé quand le diamant touche le compteur
) {
    companion object {
        const val SIZE = 45 // Taille fixe du diamant en pixels
    }

    val view = ImageView(context) // L'objet visuel qui sera affiché et animé

    init {
        view.setImageResource(R.drawable.diamond) // Charge le dessin du diamant
        view.layoutParams = ConstraintLayout.LayoutParams(SIZE, SIZE) // Applique la taille définie juste au-dessus

        view.x = startX // Place le diamant exactement là où l'ennemi est mort en X
        view.y = startY // Place le diamant exactement là où l'ennemi est mort en Y

        parentLayout.addView(view) // Ajoute officiellement le diamant visible sur l'écran de jeu

        scatter() // Déclenche l'animation d'explosion pour éparpiller le butin sur le sol
    }

    private fun scatter() {
        val angle = Math.random() * 2 * Math.PI // Choisit une direction au hasard en radians pour l'éparpillement

        val distance = (50..150).random().toFloat() // Calcule une distance aléatoire pour que ça ne tombe pas toujours au même endroit

        val destX = view.x + (cos(angle) * distance).toFloat() // Détermine le point de chute final en X avec la trigonométrie
        val destY = view.y + (sin(angle) * distance).toFloat() // Détermine le point de chute final en Y avec la trigonométrie

        view.scaleX = 0f // Réduit le diamant à néant pour le faire grandir pendant l'animation
        view.scaleY = 0f // Réduit le diamant à néant pour le faire grandir pendant l'animation

        view.animate()
            .x(destX) // Fait glisser le diamant vers sa destination en X
            .y(destY) // Fait glisser le diamant vers sa destination en Y
            .scaleX(1f) // Fait reprendre au diamant sa taille normale
            .scaleY(1f) // Fait reprendre au diamant sa taille normale
            .setDuration(500) // L'animation de chute dure une demi-seconde
            .setInterpolator(DecelerateInterpolator(2f)) // Ralentit doucement à la fin pour simuler la friction au sol
            .withEndAction {
                view.postDelayed({ // Attend un petit peu une fois au sol pour laisser le temps de voir le butin
                    collectToCurrency() // Lance le ramassage automatique vers l'interface
                }, 500)
            }
            .start() // Démarre l'animation d'explosion initiale
    }

    private fun collectToCurrency() {
        val activity = view.context as Activity // Récupère l'activité principale pour accéder à l'interface
        val targetView = activity.findViewById<TextView>(R.id.DiamondLabel) // Trouve le texte du compteur de diamants tout en haut

        val targetLocation = IntArray(2) // Prépare un tableau pour stocker les coordonnées absolues de l'interface
        targetView.getLocationInWindow(targetLocation) // Récupère la position exacte du compteur sur l'écran du téléphone

        val parentLocation = IntArray(2) // Prépare un tableau pour stocker les coordonnées absolues de la zone de jeu
        parentLayout.getLocationInWindow(parentLocation) // Récupère la position de la zone de jeu par rapport à l'écran global

        val targetX = (targetLocation[0] - parentLocation[0]).toFloat() // Calcule la vraie destination en X en soustrayant les décalages d'interface
        val targetY = (targetLocation[1] - parentLocation[1]).toFloat() // Calcule la vraie destination en Y en soustrayant les décalages d'interface

        view.animate()
            .x(targetX) // Envoie le diamant vers le compteur en X
            .y(targetY) // Envoie le diamant vers le compteur en Y
            .scaleX(0.5f) // Rétrécit le diamant pendant le vol pour donner un effet de perspective
            .scaleY(0.5f) // Rétrécit le diamant pendant le vol pour donner un effet de perspective
            .alpha(0.5f) // Rend le diamant un peu transparent pendant qu'il vole
            .setDuration(800) // L'envol vers l'interface dure un peu moins d'une seconde
            .setStartDelay((0..300).random().toLong()) // Ajoute un délai aléatoire pour que plusieurs diamants ne partent pas exactement en même temps
            .setInterpolator(AccelerateInterpolator()) // Accélère l'animation pour donner l'impression d'être aspiré par le compteur
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    parentLayout.removeView(view) // Efface définitivement l'image du diamant une fois arrivé en haut

                    val diamondSounds = listOf( // Prépare une liste de trois petits bruits de ramassage
                        R.raw.diamond_sfx1,
                        R.raw.diamond_sfx2,
                        R.raw.diamond_sfx3
                    )
                    val randomPitch = (90..130).random() / 100f // Alterne la note du son pour que ça ne soit pas trop répétitif
                    val randomSound = diamondSounds.random() // Choisit un des trois sons au hasard

                    AudioManager.playSoundEffect(randomSound, randomPitch) // Joue le petit bruit d'argent récolté

                    AudioManager.playSoundEffect(randomSound, 1f, 0.05f) // Joue un deuxième son très faible en fond pour donner de la texture audio

                    targetView.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction { // Fait grossir le compteur brièvement au contact
                        targetView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start() // Remet le compteur à sa taille normale juste après
                    }.start()

                    onCollected() // Prévient le grand gestionnaire que l'argent est sécurisé et qu'il faut augmenter le total
                }
            })
            .start() // Lance l'envol du diamant vers l'interface
    }
}