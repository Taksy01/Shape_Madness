package com.example.shapemadness

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

// Canon lourd qui tire des balles orbitales massives capables de traverser les ennemis
class OrbitCanon(
    view: ImageView, // L'image du vaisseau sur l'écran
    parentLayout: ConstraintLayout, // La zone de jeu pour l'affichage des tirs
    collisionCallback: (Ball) -> Unit // Le signal envoyé pour gérer les dégâts
) : Canon(view, parentLayout, collisionCallback) {

    override val fireRateMultiplier = 3f // Tire trois fois plus lentement que le canon de base à cause du poids des munitions

    override val damageMultiplier = 2f // Double les dégâts de chaque impact direct

    override val velocityMultiplier = 1.0f // Conserve une vitesse de propulsion standard

    override val ballSize: Float = 2.2f // Crée des projectiles géants pour laisser de la place aux satellites

    override val isPiercing: Boolean = true // Permet à la balle de continuer sa route après avoir touché un monstre

    override fun updateVisuals() {
        view.setImageResource(R.drawable.canon_orbit) // Change le skin du vaisseau pour le modèle orbital
    }

    override fun performShoot() {
        if (view.width == 0 || view.height == 0) return // Sécurité pour éviter de tirer avant que le vaisseau ne soit prêt

        AudioManager.playSoundEffect(R.raw.shoot_sfx, pitch = 0.6f) // Joue un bruitage très grave pour souligner la puissance

        val ball = OrbitBall( // Instancie la balle complexe avec ses trois satellites en rotation
            context = view.context,
            parentLayout = parentLayout,
            canonRotation = view.rotation, // Garde l'angle de visée actuel
            canonX = view.x,
            canonY = view.y,
            canonWidth = view.width,
            canonHeight = view.height,
            damage = (GameManager.playerDamage * damageMultiplier).toInt(), // Calcule la puissance d'attaque finale
            velocityMod = velocityMultiplier,
            sizeMultiplier = ballSize,
            isPiercing = isPiercing, // Transmet la capacité de perforation
            onMove = { collisionCallback(it) }, // Surveille les collisions pendant le trajet
            onDestroy = { deadBall -> activeBalls.remove(deadBall) } // Nettoie la liste des projectiles actifs à la fin
        )

        activeBalls.add(ball) // Enregistre la nouvelle balle pour qu'elle puisse être mise en pause si besoin
    }
}