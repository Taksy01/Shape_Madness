package com.example.shapemadness

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

// Canon lourd qui tire des balles explosives avec de gros dégâts
class BlasterCanon(
    view: ImageView, // L'image du canon affichée à l'écran
    parentLayout: ConstraintLayout, // La zone où les balles vont apparaître
    collisionCallback: (Ball) -> Unit // Le signal envoyé quand une balle touche un ennemi
) : Canon(view, parentLayout, collisionCallback) {

    override val fireRateMultiplier = 1.8f // Tire beaucoup plus lentement que le canon de base
    override val damageMultiplier = 2.5f // Fait des dégâts massifs à chaque impact
    override val velocityMultiplier = 2f // Les balles partent deux fois plus vite pour compenser la lenteur de tir
    override val ballSize: Float = 1.3f // Les balles sont visiblement plus grosses que la normale

    override fun updateVisuals() {
        view.setImageResource(R.drawable.canon_blaster) // Change l'image pour afficher le design du gros canon
    }

    override fun performShoot() {
        if (view.width == 0 || view.height == 0) return // Bloque le tir si le canon n'est pas encore bien affiché à l'écran

        AudioManager.playSoundEffect(R.raw.shoot_sfx, pitch = 0.6f) // Joue le son de tir avec un ton plus grave pour faire lourd

        // Crée la balle explosive avec toutes les stats modifiées du canon
        val ball = BlasterBall(
            context = view.context,
            parentLayout = parentLayout,
            canonRotation = view.rotation, // Tire dans la même direction que le canon
            canonX = view.x,
            canonY = view.y,
            canonWidth = view.width,
            canonHeight = view.height,
            damage = (GameManager.playerDamage * damageMultiplier).toInt(), // Calcule les dégâts finaux en fonction des améliorations
            velocityMod = velocityMultiplier,
            sizeMultiplier = ballSize,
            onMove = { collisionCallback(it) }, // Vérifie les collisions pendant le vol
            onDestroy = {} // Ne fait rien de spécial ici car la balle gère son explosion toute seule en mourant
        )

        activeBalls.add(ball) // Ajoute la nouvelle balle à la liste globale des objets en vol
    }
}