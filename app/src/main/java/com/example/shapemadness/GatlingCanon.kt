package com.example.shapemadness

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

// Arme à tir très rapide qui remplace le canon de base
class GatlingCanon(
    view: ImageView, // L'image du vaisseau affichée à l'écran
    parentLayout: ConstraintLayout, // La zone de jeu où les tirs vont apparaître
    collisionCallback: (Ball) -> Unit // Le signal envoyé quand une balle touche un ennemi
) : Canon(view, parentLayout, collisionCallback) {

    override val fireRateMultiplier = 0.3f // Divise le temps de rechargement pour tirer en rafale continue

    override val damageMultiplier = 1f // Garde les dégâts normaux pour chaque petite balle générée

    override val velocityMultiplier = 2.0f // Fait voler les balles deux fois plus vite vers les monstres

    override val ballSize: Float = 0.8f // Rend les projectiles légèrement plus petits visuellement

    override fun updateVisuals() {
        view.setImageResource(R.drawable.canon_gatling) // Change l'image du vaisseau pour afficher la mitrailleuse
    }

    override fun performShoot() {
        if (view.width == 0 || view.height == 0) return // Bloque le tir si le vaisseau n'est pas encore visible

        AudioManager.playSoundEffect(R.raw.shoot_sfx, pitch = 1.2f) // Rend le bruitage plus aigu pour simuler une arme légère et nerveuse

        createBall(view.rotation) // Crée une seule balle qui part tout droit selon l'angle actuel du canon
    }
}