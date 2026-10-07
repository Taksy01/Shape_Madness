package com.example.shapemadness

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

// Canon de précision qui tire des projectiles rapides avec de gros dégâts
class SniperCanon(
    view: ImageView, // L'image du vaisseau sur l'interface de jeu
    parentLayout: ConstraintLayout, // La zone de jeu pour l'apparition des tirs
    collisionCallback: (Ball) -> Unit // Le signal envoyé pour gérer les impacts sur les monstres
) : Canon(view, parentLayout, collisionCallback) {

    override val fireRateMultiplier = 1.3f // Temps de rechargement légèrement plus long pour équilibrer la puissance

    override val damageMultiplier = 2.0f // Double les dégâts de base pour chaque tir réussi

    override val velocityMultiplier = 2.0f // Les balles filent deux fois plus vite pour ne pas rater les cibles mouvantes

    override val ballSize: Float = 0.8f // Projectiles plus fins et discrets pour un look de sniper

    override fun updateVisuals() {
        view.setImageResource(R.drawable.canon_sniper) // Change le skin du vaisseau pour le modèle de précision
    }

    override fun performShoot() {
        if (view.width == 0 || view.height == 0) return // Sécurité pour éviter de tirer si le vaisseau n'est pas encore prêt

        AudioManager.playSoundEffect(R.raw.shoot_sfx, pitch = 0.8f) // Joue un son de tir plus sec et plus grave

        createBall(view.rotation) // Crée une balle unique parfaitement alignée avec le viseur
    }
}