package com.example.shapemadness

import android.animation.ValueAnimator
import android.content.Context
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.cos
import kotlin.math.sin

// Projectile spécial entouré de trois petits satellites qui tournent autour
class OrbitBall(
    context: Context, // Contexte pour créer les images des satellites
    parentLayout: ConstraintLayout, // Zone de jeu pour l'affichage
    canonRotation: Float, // Direction du tir
    canonX: Float, // Point de départ horizontal
    canonY: Float, // Point de départ vertical
    canonWidth: Int, // Largeur du canon
    canonHeight: Int, // Hauteur du canon
    damage: Int, // Dégâts de la balle centrale
    velocityMod: Float, // Modificateur de vitesse
    sizeMultiplier: Float, // Taille du projectile
    isPiercing: Boolean, // Capacité à traverser les cibles
    onMove: (Ball) -> Unit, // Callback de mouvement
    onDestroy: (Ball) -> Unit // Callback de destruction
) : Ball(
    context, parentLayout,
    canonRotation, canonX, canonY, canonWidth, canonHeight,
    damage, velocityMod, sizeMultiplier, isPiercing,
    onMove, onDestroy
) {
    // Liste pour stocker les images et les données de collision des satellites
    private var satelliteObjects: MutableList<Satellite>? = null

    // Angle actuel pour calculer la position de rotation des satellites
    private var currentOrbitAngle = 0.0

    // Rayon actuel de l'orbite qui grandit progressivement au lancement
    private var currentAnimatedRadius = 0f

    // Définit la distance finale entre la balle et ses satellites
    private val targetOrbitRadius get() = ballSize * 1.1f

    // Classe simple pour lier une image de satellite à sa liste d'ennemis touchés
    class Satellite(val view: ImageView) {
        val hitEnemies = mutableSetOf<Enemy>()
    }

    // Représente la zone de collision d'un satellite pour le moteur de jeu
    data class SatelliteHitbox(
        val x: Float, // Centre horizontal du satellite
        val y: Float, // Centre vertical du satellite
        val radius: Float, // Rayon de la zone de choc
        val hitEnemies: MutableSet<Enemy> // Ennemis déjà traversés par ce satellite
    )

    init {
        // L'initialisation se fait via setupView et launch appelés par la classe mère
    }

    override fun setupView() {
        super.setupView()
        view.setImageResource(R.drawable.ball) // Utilise l'image de balle standard
        view.scaleX = 1.2f // Rend la balle centrale un peu plus imposante
        view.scaleY = 1.2f
        view.translationZ = 10f // Place la balle principale au-dessus de ses satellites
    }

    override fun launch(rotation: Float, cX: Float, cY: Float, cW: Int, cH: Int) {
        satelliteObjects = mutableListOf() // Initialise la liste vide avant de créer les satellites
        super.launch(rotation, cX, cY, cW, cH) // Lance le mouvement de la balle principale

        val orbiterScale = 0.5f // Les satellites sont deux fois plus petits que la balle centrale
        val orbiterSize = (ballSize * orbiterScale).toInt()

        for (i in 0 until 3) { // Crée exactement trois satellites autour de la balle
            val iv = ImageView(view.context).apply {
                setImageResource(R.drawable.ball) // Donne le même visuel que la balle
                layoutParams = ConstraintLayout.LayoutParams(orbiterSize, orbiterSize)
                translationZ = 9f // Place les satellites juste en dessous de la balle centrale
            }
            satelliteObjects!!.add(Satellite(iv)) // Enregistre le satellite dans la liste
            parentLayout.addView(iv) // Affiche le satellite sur l'écran
        }

        // Animation pour écarter doucement les satellites du centre au moment du tir
        ValueAnimator.ofFloat(0f, targetOrbitRadius).apply {
            duration = 500L // L'écartement dure une demi-seconde
            interpolator = android.view.animation.DecelerateInterpolator() // Ralentit sur la fin pour plus de fluidité
            addUpdateListener { animator ->
                currentAnimatedRadius = animator.animatedValue as Float // Met à jour le rayon actuel
            }
            start()
        }

        animator?.addUpdateListener { // Boucle de mise à jour synchronisée sur le mouvement de la balle
            // Calcule la vitesse de rotation en fonction des améliorations globales du joueur
            val currentSpeedMultiplier = GameManager.ballVelocity / 15f
            val spinSpeed = 0.04 * currentSpeedMultiplier // Plus le joueur est rapide plus les satellites tournent vite
            currentOrbitAngle += spinSpeed

            val centerX = view.x + (ballSize / 2f) // Trouve le centre exact de la balle centrale
            val centerY = view.y + (ballSize / 2f)

            satelliteObjects?.forEachIndexed { index, sat ->
                val angleOffset = index * (2 * Math.PI / 3) // Espace les satellites de 120 degrés chacun
                val totalAngle = currentOrbitAngle + angleOffset

                // Calcule la position X et Y de chaque satellite avec la trigonométrie
                sat.view.x = centerX + (currentAnimatedRadius * cos(totalAngle)).toFloat() - (sat.view.layoutParams.width / 2f)
                sat.view.y = centerY + (currentAnimatedRadius * sin(totalAngle)).toFloat() - (sat.view.layoutParams.height / 2f)
            }
        }
    }

    override fun destroy() {
        satelliteObjects?.forEach { parentLayout.removeView(it.view) } // Retire chaque satellite de l'écran avant de mourir
        satelliteObjects?.clear() // Vide la liste pour libérer la mémoire vive
        super.destroy() // Détruit la balle principale
    }

    // Récupère les données de collision de chaque satellite pour le moteur de jeu
    fun getSatelliteHitboxes(): List<SatelliteHitbox> {
        val currentSats = satelliteObjects ?: return emptyList()
        val sRadius = (ballSize * 0.5f) / 2f // Calcule le rayon de collision de chaque petit satellite
        return currentSats.map { sat ->
            SatelliteHitbox(
                x = sat.view.x + (sat.view.width / 2f),
                y = sat.view.y + (sat.view.height / 2f),
                radius = sRadius,
                hitEnemies = sat.hitEnemies
            )
        }
    }
}