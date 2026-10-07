package com.example.shapemadness

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.cos
import kotlin.math.sin

// Canon spécial qui tire en rafale avec 5 trous différents
class BarrageCanon(
    view: ImageView, // L'image du canon sur l'écran
    parentLayout: ConstraintLayout, // La zone où les balles vont apparaître
    collisionCallback: (Ball) -> Unit // Le signal envoyé quand une balle touche un truc
) : Canon(view, parentLayout, collisionCallback) {

    override val fireRateMultiplier = 0.3f // Tire beaucoup plus vite que le canon de base
    override val damageMultiplier = 1f // Les dégâts restent normaux
    override val velocityMultiplier = 1.5f // Les balles partent plus vite
    override val ballSize: Float = 0.7f // Les balles sont un peu plus petites

    private val barrels = listOf( // Coordonnées des 5 trous du canon pour décaler le tir
        Pair(0f, 0f), // Le trou central
        Pair(-0.10f, -0.10f), // Trou gauche un peu en arrière
        Pair(0.10f, -0.10f), // Trou droit un peu en arrière
        Pair(-0.2f, -0.20f), // Trou tout à gauche bien en arrière
        Pair(0.2f, -0.20f) // Trou tout à droite bien en arrière
    )

    private var currentBarrelIndex = 0 // Permet de savoir quel canon doit tirer au prochain tour

    override fun updateVisuals() {
        view.setImageResource(R.drawable.canon_barrage) // Change l'image pour afficher le gros canon
    }

    override fun performShoot() {
        if (view.width == 0 || view.height == 0) return // Stoppe tout si l'image n'est pas encore chargée

        AudioManager.playSoundEffect(R.raw.shoot_sfx, pitch = 1.3f) // Bruitage plus aigu pour la rafale

        val currentBarrel = barrels[currentBarrelIndex] // Récupère le trou qui doit tirer maintenant
        val lateralOffsetRatio = currentBarrel.first // Décalage sur le côté
        val forwardOffsetRatio = currentBarrel.second // Décalage vers l'avant ou l'arrière

        val lateralPx = view.width * lateralOffsetRatio // Convertit le décalage en pixels réels horizontaux
        val forwardPx = view.height * forwardOffsetRatio // Convertit le décalage en pixels réels verticaux

        val angleRightRad = Math.toRadians(view.rotation.toDouble()) // Calcule l'angle pour le décalage sur les côtés
        val angleForwardRad = Math.toRadians(view.rotation.toDouble() - 90) // Calcule l'angle pour le décalage avant arrière

        val shiftX = (lateralPx * cos(angleRightRad) + forwardPx * cos(angleForwardRad)).toFloat() // Déplace le point de tir en X avec de la trigo
        val shiftY = (lateralPx * sin(angleRightRad) + forwardPx * sin(angleForwardRad)).toFloat() // Déplace le point de tir en Y avec de la trigo

        val fakeCanonX = view.x + shiftX // Crée une fausse position de canon pour faire spawner la balle au bon endroit
        val fakeCanonY = view.y + shiftY

        val ball = Ball( // Fabrique la balle avec toutes les bonnes stats
            context = view.context,
            parentLayout = parentLayout,
            canonRotation = view.rotation, // Garde la même orientation que le gros canon
            canonX = fakeCanonX,
            canonY = fakeCanonY,
            canonWidth = view.width,
            canonHeight = view.height,
            damage = (GameManager.playerDamage * damageMultiplier).toInt(), // Calcule les dégâts finaux
            velocityMod = velocityMultiplier,
            sizeMultiplier = ballSize,
            isPiercing = false, // Ces balles ne traversent pas les monstres
            onMove = { collisionCallback(it) }, // Vérifie si la balle touche un monstre en volant
            onDestroy = { deadBall -> activeBalls.remove(deadBall) } // Efface la balle de la mémoire quand elle meurt
        )

        activeBalls.add(ball) // Ajoute la nouvelle balle à la liste des balles en vol

        currentBarrelIndex = (currentBarrelIndex + 1) % barrels.size // Passe au trou suivant et boucle à zéro quand c'est fini
    }
}