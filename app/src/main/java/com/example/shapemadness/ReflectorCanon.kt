package com.example.shapemadness

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

// Canon spécial qui tire des balles capables de rebondir sur les bords de l'écran
class ReflectorCanon(
    view: ImageView, // L'image du vaisseau sur l'interface
    parentLayout: ConstraintLayout, // La zone de jeu pour l'affichage des tirs
    collisionCallback: (Ball) -> Unit // Le signal envoyé pour gérer les dégâts sur les ennemis
) : Canon(view, parentLayout, collisionCallback) {

    override val fireRateMultiplier = 2.5f // Temps de rechargement assez long pour compenser la durée de vie des balles

    override val damageMultiplier = 2.5f // Dégâts augmentés pour chaque impact réussi

    override val velocityMultiplier = 1.9f // Vitesse de propulsion élevée pour favoriser les ricochets rapides

    override val ballSize: Float = 1.7f // Taille de projectile intermédiaire pour une meilleure visibilité

    override val isPiercing: Boolean = true // Permet à la balle de traverser les monstres sans disparaître

    override fun updateVisuals() {
        view.setImageResource(R.drawable.canon_reflector) // Change l'apparence du vaisseau pour le modèle à ricochets
    }

    override fun performShoot() {
        if (view.width == 0 || view.height == 0) return // Sécurité pour ne pas tirer si le vaisseau n'est pas encore prêt

        AudioManager.playSoundEffect(R.raw.shoot_sfx, pitch = 1.4f) // Joue un bruitage plus aigu et métallique pour cet effet

        val ball = ReflectorBall( // Crée la balle spécifique capable de calculer les angles de rebond
            context = view.context,
            parentLayout = parentLayout,
            canonRotation = view.rotation, // Direction initiale basée sur la visée actuelle
            canonX = view.x,
            canonY = view.y,
            canonWidth = view.width,
            canonHeight = view.height,
            damage = (GameManager.playerDamage * damageMultiplier).toInt(), // Applique la puissance d'attaque finale
            velocityMod = velocityMultiplier,
            sizeMultiplier = ballSize,
            isPiercing = isPiercing,
            onMove = { collisionCallback(it) }, // Surveille les collisions tout au long des rebonds
            onDestroy = { deadBall -> activeBalls.remove(deadBall) } // Retire la balle de la mémoire une fois épuisée
        )

        activeBalls.add(ball) // Enregistre la balle pour permettre la mise en pause globale du jeu
    }
}