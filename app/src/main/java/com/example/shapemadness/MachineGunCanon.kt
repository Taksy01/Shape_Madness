package com.example.shapemadness

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

// Canon double qui tire en alternance très rapidement
class MachineGunCanon(
    view: ImageView, // L'image du vaisseau à l'écran
    parentLayout: ConstraintLayout, // La zone de jeu pour faire apparaître les balles
    collisionCallback: (Ball) -> Unit // Le signal pour détecter les impacts
) : Canon(view, parentLayout, collisionCallback) {

    override val fireRateMultiplier = 0.5f // Tire deux fois plus vite que le canon de base

    override val damageMultiplier = 0.9f // Baisse légèrement les dégâts pour équilibrer la cadence

    override val velocityMultiplier = 1.2f // Accélère un peu le vol des balles

    override val ballSize = 0.9f // Réduit la taille des balles pour faire plus réaliste

    private var fireFromLeft = true // Mémoire pour savoir de quel côté doit partir la prochaine balle

    override fun updateVisuals() {
        view.setImageResource(R.drawable.canon_machinegun) // Charge le dessin du vaisseau avec deux canons
    }

    override fun performShoot() {
        if (view.width == 0 || view.height == 0) return // Sécurité pour ne pas tirer si le vaisseau n'est pas affiché

        AudioManager.playSoundEffect(R.raw.shoot_sfx, 1.5f) // Joue un bruitage plus aigu pour accentuer l'effet de vitesse

        val offset = if (fireFromLeft) -22f else 22f // Calcule un décalage de 22 pixels à gauche ou à droite

        createBallWithOffset(view.rotation, offset) // Fabrique la balle en appliquant le décalage calculé

        fireFromLeft = !fireFromLeft // Inverse la valeur pour que le prochain tir sorte de l'autre trou
    }
}