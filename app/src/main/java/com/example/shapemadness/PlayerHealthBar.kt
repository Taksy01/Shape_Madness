package com.example.shapemadness

import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Rect
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView

// Gère l'affichage et les animations de la santé du joueur en haut de l'écran
class PlayerHealthBar(activity: Activity) {

    val healthBarFill = activity.findViewById<ImageView>(R.id.playerHealthBarFill) // L'image de la jauge qui se vide
    val maxHealth = 100 // Points de vie maximum autorisés
    var currentHealth = maxHealth // Points de vie actuels au lancement du jeu

    // Augmente la vie du joueur et déclenche l'effet visuel de soin
    fun heal(amount: Int) {
        val oldHealth = currentHealth

        currentHealth = minOf(maxHealth, currentHealth + amount) // Empêche la vie de dépasser le plafond des 100 points

        animateHealthBar(oldHealth, currentHealth) // Lance la transition fluide de la jauge vers la droite

        val randomPitch = (90..120).random() / 100f // Aléatoire sur le son pour un rendu plus organique
        AudioManager.playSoundEffect(R.raw.player_heal_sfx, randomPitch)

        flashGreen() // Fait briller la barre en vert pour confirmer le soin visuellement
    }

    // Retire de la vie et renvoie vrai si le joueur est mort
    fun takeDamage(damage: Int): Boolean {
        val oldHealth = currentHealth

        val randomPitch = (90..120).random() / 100f
        AudioManager.playSoundEffect(R.raw.player_hit_sfx, randomPitch, 0.4f) // Joue le son d'impact

        currentHealth -= damage // Soustrait les dégâts reçus
        if (currentHealth < 0) currentHealth = 0 // Bloque la vie à zéro pour éviter les bugs d'affichage

        animateHealthBar(oldHealth, currentHealth) // Fait reculer la jauge vers la gauche

        return currentHealth <= 0 // Informe le jeu si la partie doit s'arrêter
    }

    // Moteur d'animation qui déplace et découpe la barre de vie proprement
    private fun animateHealthBar(oldHealth: Int, newHealth: Int) {
        val totalWidth = healthBarFill.width // Récupère la largeur réelle de l'image en pixels
        if (totalWidth == 0) return // Sécurité si l'interface n'est pas encore totalement chargée

        val oldPercentageLost = 1f - (oldHealth.toFloat() / maxHealth.toFloat()) // Calcule l'ancienne position de coupe
        val newPercentageLost = 1f - (newHealth.toFloat() / maxHealth.toFloat()) // Calcule la nouvelle position cible

        val animator = ValueAnimator.ofFloat(oldPercentageLost, newPercentageLost) // Crée une transition entre les deux états
        animator.duration = 300 // L'animation de la barre dure moins d'une demi-seconde
        animator.interpolator = DecelerateInterpolator() // Ralentit sur la fin pour un mouvement plus naturel

        animator.addUpdateListener { anim ->
            val currentLost = anim.animatedValue as Float
            val cutLeft = (totalWidth * currentLost).toInt() // Calcule le nombre de pixels à cacher sur la gauche

            healthBarFill.translationX = -cutLeft.toFloat() // Décale l'image vers la gauche
            healthBarFill.clipBounds = Rect(cutLeft, 0, totalWidth, healthBarFill.height) // Recadre l'image pour garder la barre alignée
        }

        animator.start() // Lance le mouvement de la jauge
    }

    // Applique un filtre vert temporaire qui s'estompe doucement
    private fun flashGreen() {
        val flashAnimator = ValueAnimator.ofInt(255, 0) // Fait varier l'opacité du vert de 100% à 0%

        flashAnimator.duration = 800 // Le flash met presque une seconde à disparaître totalement

        flashAnimator.addUpdateListener { anim ->
            val alpha = anim.animatedValue as Int
            val flashColor = android.graphics.Color.argb(alpha, 0, 255, 0) // Fabrique la couleur vert fluo avec la transparence actuelle

            healthBarFill.colorFilter = android.graphics.PorterDuffColorFilter(
                flashColor,
                android.graphics.PorterDuff.Mode.SRC_ATOP // Fusionne le vert par-dessus la couleur d'origine de la barre
            )
        }

        flashAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                healthBarFill.clearColorFilter() // Retire toute trace de filtre une fois l'animation finie pour retrouver le rouge d'origine
            }
        })

        flashAnimator.start() // Déclenche l'effet lumineux
    }
}