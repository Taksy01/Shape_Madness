package com.example.shapemadness

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.view.animation.LinearInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.cos
import kotlin.math.sin

// Projectile spécial capable de ricocher sur les bords de l'écran plusieurs fois
class ReflectorBall(
    context: Context, // Contexte pour créer la vue
    parentLayout: ConstraintLayout, // Zone de jeu pour les rebonds
    canonRotation: Float, // Angle de tir initial
    canonX: Float, // Position de départ en X
    canonY: Float, // Position de départ en Y
    canonWidth: Int, // Largeur du canon
    canonHeight: Int, // Hauteur du canon
    damage: Int, // Dégâts de la balle
    velocityMod: Float, // Modificateur de vitesse
    sizeMultiplier: Float, // Taille de la balle
    isPiercing: Boolean, // Capacité à traverser les ennemis
    onMove: (Ball) -> Unit, // Callback de mouvement
    onDestroy: (Ball) -> Unit // Callback de destruction
) : Ball(
    context, parentLayout, canonRotation, canonX, canonY, canonWidth, canonHeight,
    damage, velocityMod, sizeMultiplier, isPiercing, onMove, onDestroy
) {
    private var bounces = 0 // Compteur actuel du nombre de rebonds effectués
    private val maxBounces = 4 // Limite maximum de ricochets autorisés avant de perdre sa propriété physique

    override fun setupView() {
        super.setupView()
        view.setImageResource(R.drawable.reflector_ball) // Charge le visuel de la balle réfléchissante
        view.translationZ = 10f // Place la balle au niveau standard des projectiles
        view.alpha = 1.0f // S'assure que la balle commence avec une visibilité maximale
    }

    override fun launch(rotation: Float, cX: Float, cY: Float, cW: Int, cH: Int) {
        val angleRad = Math.toRadians(rotation.toDouble() - 90) // Calcule l'angle de départ en radians

        val centerX = cX + cW / 2 // Centre horizontal du canon
        val centerY = cY + cH / 2 // Centre vertical du canon
        val distToMuzzle = cH / 2 // Distance jusqu'à la sortie du tube

        view.x = centerX + (distToMuzzle * cos(angleRad)).toFloat() - (ballSize / 2) // Place la balle au bout du canon
        view.y = centerY + (distToMuzzle * sin(angleRad)).toFloat() - (ballSize / 2)
        parentLayout.addView(view)

        var dirX = cos(angleRad).toFloat() // Direction horizontale initiale du mouvement
        var dirY = sin(angleRad).toFloat() // Direction verticale initiale du mouvement
        var lastFraction = 0f // Mémorise la progression de l'animation pour calculer le déplacement entre deux frames

        val massiveDistance = 20000f // Définit une distance théorique immense pour permettre les rebonds infinis
        val distanceMultiplier = massiveDistance / travelDistance

        val actualVelocity = GameManager.ballVelocity * velocityMod // Applique la vitesse actuelle du joueur
        val currentDuration = ((75000f / actualVelocity) * distanceMultiplier).toLong() // Calcule la durée totale du vol

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = currentDuration
            interpolator = LinearInterpolator() // Mouvement rectiligne uniforme sans accélération

            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                val deltaFraction = fraction - lastFraction // Calcule le temps écoulé depuis la dernière mise à jour
                lastFraction = fraction

                val stepDist = massiveDistance * deltaFraction // Calcule la distance parcourue pendant cette frame
                view.x += dirX * stepDist // Applique le mouvement en X selon la direction actuelle
                view.y += dirY * stepDist // Applique le mouvement en Y selon la direction actuelle

                // --- GESTION DES REBONDS ---
                if (bounces < maxBounces) {
                    var bounced = false
                    val rightEdge = parentLayout.width - view.width // Limite droite de l'écran de jeu
                    val bottomEdge = parentLayout.height - view.height // Limite basse de l'écran de jeu

                    if (view.x <= 0) { // Rebond sur le mur de gauche
                        view.x = 0f
                        dirX = -dirX // Inverse la direction horizontale
                        bounced = true
                    } else if (view.x >= rightEdge && rightEdge > 0) { // Rebond sur le mur de droite
                        view.x = rightEdge.toFloat()
                        dirX = -dirX
                        bounced = true
                    }

                    if (view.y <= 0) { // Rebond sur le mur du haut
                        view.y = 0f
                        dirY = -dirY // Inverse la direction verticale
                        bounced = true
                    } else if (view.y >= bottomEdge && bottomEdge > 0) { // Rebond sur le mur du bas
                        view.y = bottomEdge.toFloat()
                        dirY = -dirY
                        bounced = true
                    }

                    if (bounced) {
                        bounces++ // Incrémente le compteur après un impact réussi
                        view.alpha = 1.0f - (bounces * 0.2f) // Réduit la visibilité de 20% à chaque ricochet
                    }
                } else {
                    // Une fois les rebonds terminés la balle continue tout droit jusqu'à sortir de l'écran
                    val isOutOfBounds = view.x < -view.width || view.x > parentLayout.width ||
                            view.y < -view.height || view.y > parentLayout.height

                    if (isOutOfBounds) {
                        destroy() // Supprime la balle une fois qu'elle n'est plus visible
                    }
                }

                onMove(this@ReflectorBall) // Vérifie les collisions avec les ennemis après chaque déplacement
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    destroy() // Nettoie la mémoire si l'animation se termine avant la sortie de l'écran
                }
            })
            start() // Déclenche le vol de la balle
        }
    }
}