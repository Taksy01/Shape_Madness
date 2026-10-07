package com.example.shapemadness

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.cos
import kotlin.math.sin

// Canon qui tire trois balles en éventail pour couvrir une large zone
class SplitterCanon(
    view: ImageView, // L'image du vaisseau sur l'interface
    parentLayout: ConstraintLayout, // La zone de jeu pour l'affichage des tirs
    collisionCallback: (Ball) -> Unit // Le signal envoyé pour gérer les dégâts sur les monstres
) : Canon(view, parentLayout, collisionCallback) {

    override val fireRateMultiplier = 0.5f // Cadence de tir très rapide pour compenser les dégâts divisés

    override val damageMultiplier = 1f // Garde la puissance de base pour chaque projectile de la salve

    override val velocityMultiplier = 1.2f // Accélère légèrement le vol des balles latérales

    override val ballSize: Float = 1f // Conserve la taille standard des munitions

    override fun updateVisuals() {
        view.setImageResource(R.drawable.canon_splitter) // Change le skin du vaisseau pour le modèle à triple sortie
    }

    override fun performShoot() {
        if (view.width == 0 || view.height == 0) return // Sécurité pour ne pas tirer si le vaisseau n'est pas encore prêt

        AudioManager.playSoundEffect(R.raw.shoot_sfx, pitch = 0.8f) // Joue un son de tir un peu plus grave pour marquer la salve

        createBall(view.rotation) // Envoie la balle centrale parfaitement droite

        createSideBall(view.rotation - 30f) // Envoie une balle inclinée de trente degrés vers la gauche
        createSideBall(view.rotation + 30f) // Envoie une balle inclinée de trente degrés vers la droite
    }

    // Crée une balle décalée vers l'arrière pour simuler une sortie sur les côtés du vaisseau
    private fun createSideBall(angle: Float) {
        val backwardOffset = view.height * 0.13f // Calcule un recul de treize pour cent de la taille du canon

        val angleBackRad = Math.toRadians(view.rotation.toDouble() + 90) // Calcule l'angle perpendiculaire au canon

        val fakeX = view.x + (backwardOffset * cos(angleBackRad)).toFloat() // Décale le point de départ en X
        val fakeY = view.y + (backwardOffset * sin(angleBackRad)).toFloat() // Décale le point de départ en Y

        val ball = Ball( // Instancie la balle avec les coordonnées reculées
            context = view.context,
            parentLayout = parentLayout,
            canonRotation = angle, // Applique l'angle de dispersion latéral
            canonX = fakeX,
            canonY = fakeY,
            canonWidth = view.width,
            canonHeight = view.height,
            damage = (GameManager.playerDamage * damageMultiplier).toInt(), // Applique les dégâts calculés
            velocityMod = velocityMultiplier,
            sizeMultiplier = ballSize,
            isPiercing = isPiercing,
            onMove = { collisionCallback(it) }, // Surveille les collisions pendant le trajet
            onDestroy = { deadBall -> activeBalls.remove(deadBall) } // Nettoie la liste pour la gestion de la pause
        )

        activeBalls.add(ball) // Enregistre le projectile dans la liste des objets actifs
    }
}