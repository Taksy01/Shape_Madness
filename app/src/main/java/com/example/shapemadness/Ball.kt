package com.example.shapemadness

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.cos
import kotlin.math.sin

// Projectile de base qui sert de modèle pour toutes les balles
open class Ball(
    context: Context, // Sert à créer l'image à afficher
    protected val parentLayout: ConstraintLayout, // La zone où la balle va voler
    canonRotation: Float, // Angle du canon pour la direction
    canonX: Float, // Position horizontale de départ
    canonY: Float, // Position verticale de départ
    canonWidth: Int, // Largeur du canon pour ajuster la sortie
    canonHeight: Int, // Hauteur du canon pour ajuster la sortie
    val damage: Int, // Dégâts infligés aux ennemis
    protected val velocityMod: Float, // Modificateur de vitesse de la balle
    val sizeMultiplier: Float, // Multiplicateur de taille de la balle
    val isPiercing: Boolean, // Indique si la balle peut traverser les ennemis
    protected val onMove: (Ball) -> Unit, // Callback appelé à chaque mouvement
    protected val onDestroy: (Ball) -> Unit // Callback appelé quand la balle disparaît
) {

    val view: ImageView = ImageView(context) // L'image physique affichée à l'écran
    val hitEnemies = mutableSetOf<Enemy>() // Liste des ennemis touchés pour gérer le piercing

    open val hitRadius: Float // Le rayon de la zone de choc pour les collisions
        get() = ballSize / 2f

    open val hitX: Float // Le centre exact de la balle en X
        get() = view.x + (ballSize / 2f)

    open val hitY: Float // Le centre exact de la balle en Y
        get() = view.y + (ballSize / 2f)

    protected val ballSize = (50 * sizeMultiplier).toInt() // Taille finale calculée
    protected val travelDistance = 2500f // Distance max avant destruction
    protected var animator: ValueAnimator? = null // Moteur d'animation

    init {
        setupView() // Prépare l'apparence
        launch(canonRotation, canonX, canonY, canonWidth, canonHeight) // Lance en fonction de la rotation du canon
        view.translationZ = 10f // Place la balle au premier plan
    }

    open fun destroy() {
        animator?.cancel() // Stoppe le mouvement
        parentLayout.removeView(view) // Efface l'image
        onDestroy(this) // Prévient le jeu de la destruction
    }

    fun pause() {
        animator?.pause() // Met le mouvement en pause
    }

    fun resume() {
        animator?.resume() // Relance le mouvement
    }

    protected open fun setupView() {
        view.setImageResource(R.drawable.ball) // Charge le dessin de la balle
        val layoutParams = ConstraintLayout.LayoutParams(ballSize, ballSize) // Définit la taille de l'image
        view.layoutParams = layoutParams // Applique la taille
        view.elevation = 16f // Place la balle par-dessus les ennemis car ils sont à 15f
    }

    protected open fun launch(rotation: Float, cX: Float, cY: Float, cW: Int, cH: Int) {
        val angleRad = Math.toRadians(rotation.toDouble() - 90) // Calcul de l'angle en radians

        // Calcul du point central du canon pour viser juste
        val centerX = cX + cW / 2
        val centerY = cY + cH / 2
        val distToMuzzle = cH / 2

        // Calcule le point de départ exact au bout du canon
        val startX = centerX + (distToMuzzle * cos(angleRad)).toFloat() - (ballSize / 2)
        val startY = centerY + (distToMuzzle * sin(angleRad)).toFloat() - (ballSize / 2)

        view.x = startX // Place la balle en X
        view.y = startY // Place la balle en Y

        parentLayout.addView(view, 0) // Ajoute la balle à l'écran

        // Calcule le point d'arrivée très loin hors de l'écran
        val endX = startX + (travelDistance * cos(angleRad)).toFloat()
        val endY = startY + (travelDistance * sin(angleRad)).toFloat()

        // Affecte les améliorations à la vélocité de la balle
        val actualVelocity = GameManager.ballVelocity * velocityMod
        val currentDuration = (75000f / actualVelocity).toLong() // Calcule le temps de trajet

        // Le moteur qui déplace la balle frame par frame
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = currentDuration // Temps total du trajet
            interpolator = LinearInterpolator() // Vitesse constante sans accélération

            // Boucle de mise à jour de la position
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction // Pourcentage du trajet effectué
                view.x = startX + (endX - startX) * fraction // Met à jour X
                view.y = startY + (endY - startY) * fraction // Met à jour Y
                onMove(this@Ball) // Vérifie les collisions en temps réel
            }

            // Nettoyage à la fin de l'animation
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    parentLayout.removeView(view) // Retire l'image de l'écran
                    onDestroy(this@Ball) // Détruit l'objet en mémoire
                }
            })
            start() // Démarre l'animation
        }
    }
}