package com.example.shapemadness

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

// Canon ultime qui tire des météores dévastateurs traversant tous les ennemis
class MeteorCanon(
    view: ImageView, // L'image du vaisseau sur l'écran
    parentLayout: ConstraintLayout, // La zone de jeu pour faire apparaître les projectiles
    collisionCallback: (Ball) -> Unit // Le signal envoyé quand un météore touche une cible
) : Canon(view, parentLayout, collisionCallback) {

    override val fireRateMultiplier = 5f // Tire très lentement à cause de la puissance de frappe

    override val damageMultiplier = 5f // Multiplie les dégâts de base par cinq pour tout raser

    override val velocityMultiplier = 3f // Les météores foncent trois fois plus vite que les balles normales

    override val ballSize = 5f // Les projectiles sont gigantesques par rapport au canon de base

    override val isPiercing = true // Permet au météore de continuer sa route après avoir tué un monstre

    override fun updateVisuals() {
        view.setImageResource(R.drawable.canon_meteor) // Change l'apparence du vaisseau pour montrer le lanceur lourd
    }

    override fun performShoot() {
        if (view.width == 0 || view.height == 0) return // Sécurité pour ne pas tirer si le vaisseau n'est pas encore affiché

        AudioManager.playSoundEffect(R.raw.shoot_sfx, 0.7f) // Joue un son de tir très grave pour marquer la puissance

        val ball = MeteorBall( // Crée l'objet météore complexe avec ses flammes et sa fumée
            context = view.context,
            parentLayout = parentLayout,
            canonRotation = view.rotation, // Garde l'angle de visée actuel
            canonX = view.x,
            canonY = view.y,
            canonWidth = view.width,
            canonHeight = view.height,
            damage = (GameManager.playerDamage * damageMultiplier).toInt(), // Calcule les dégâts finaux massifs
            velocityMod = velocityMultiplier,
            sizeMultiplier = ballSize,
            isPiercing = isPiercing,
            onMove = collisionCallback, // Vérifie les impacts pendant tout le trajet
            onDestroy = { deadBall -> activeBalls.remove(deadBall) } // Nettoie la liste quand le météore quitte l'écran
        )

        activeBalls.add(ball) // Ajoute le projectile à la liste des objets actifs à l'écran
    }
}