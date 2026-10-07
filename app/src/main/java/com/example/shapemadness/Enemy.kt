package com.example.shapemadness

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

// Gère la création, le déplacement, la vie et la mort des monstres
class Enemy(
    context: Context, // Contexte pour créer les vues
    private val parentLayout: ConstraintLayout, // Zone où le monstre apparaît
    val type: String, // Détermine la forme, la vitesse et la vie du monstre
    private var startX: Float, // Position d'apparition en X
    private var startY: Float, // Position d'apparition en Y
    val isFromBoss: Boolean = false // Si vrai, le monstre lâche plus de diamants
) {
    companion object {
        const val SIZE = 120 // Taille de base d'un monstre normal
        const val BAR_WIDTH = 120 // Largeur de la barre de vie
        const val BAR_HEIGHT = 25 // Hauteur de la barre de vie
        const val BAR_OFFSET = SIZE + 30 // Distance entre le monstre et sa barre
        const val BAR_MARGIN = 5 // Marge interne de la barre de vie
    }

    val view: ImageView = ImageView(context) // L'image du monstre à l'écran
    val isBig = type.startsWith("big_") // Vérifie s'il s'agit d'une version géante
    val size: Int = if (isBig) (SIZE * 1.5).toInt() else SIZE // Les géants sont 1.5x plus gros
    val barWidth: Int = if (isBig) (BAR_WIDTH * 1.5).toInt() else BAR_WIDTH // Leur barre de vie est aussi plus grande

    // Définition des points de vie de base selon le type de monstre
    private var health: Int = when (type) {
        "triangle" -> 30
        "square" -> 50
        "pentagon" -> 100
        "big_triangle" -> 500
        "big_square" -> 1000
        "big_pentagon" -> 1500
        else -> 100
    }

    // Définition de la vitesse de déplacement (les petits vont plus vite)
    private val speed: Float = when (type) {
        "triangle" -> 5f
        "square" -> 3f
        "pentagon" -> 2f
        "big_triangle" -> 0.8f
        "big_square" -> 0.7f
        "big_pentagon" -> 0.5f
        else -> 3f
    }

    // Dégâts infligés au joueur en cas de collision directe
    val damageValue: Int = when (type) {
        "triangle" -> 10
        "square" -> 20
        "pentagon" -> 30
        "big_triangle" -> 100
        "big_square" -> 100
        "big_pentagon" -> 100
        else -> 10
    }

    // Nombre de diamants lâchés à la mort (les serviteurs de boss en donnent beaucoup plus)
    private val diamondRange: IntRange = when (type) {
        "triangle" -> if (isFromBoss) 10..20 else 1..3
        "square"   -> if (isFromBoss) 30..40 else 4..6
        "pentagon" -> if (isFromBoss) 50..80 else 8..12
        "big_triangle", "big_square", "big_pentagon" -> 0..0 // Les boss ont leur propre système de récompense ailleurs
        else -> 1..3
    }

    private val maxHealth = health // Sauvegarde la vie max pour calculer le pourcentage de la barre
    private val backgroundBar = View(context) // Le fond blanc de la barre de vie
    private val healthBar = View(context) // La partie rouge qui se vide
    private var rotateAnimator: ObjectAnimator? = null // Moteur d'animation pour la rotation infinie

    init {
        setupView(startX, startY) // Prépare l'apparence physique
        setupHealthBar(startX, startY) // Crée la barre de vie mais la garde invisible
        startSpinning() // Lance l'animation de rotation
    }

    // Déplace le monstre d'un pas vers une cible (généralement le centre de l'écran)
    fun moveTowards(targetX: Float, targetY: Float) {
        val myCenterX = view.x + size / 2
        val myCenterY = view.y + size / 2

        val dx = targetX - myCenterX
        val dy = targetY - myCenterY
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat() // Calcul trigonométrique de la distance totale

        if (distance > 0) {
            val moveX = (dx / distance) * speed // Calcule le vecteur de mouvement en X
            val moveY = (dy / distance) * speed // Calcule le vecteur de mouvement en Y

            view.x += moveX // Applique le mouvement à l'image du monstre
            view.y += moveY

            val myBarOffset = size + 30f // Garde une distance fixe entre le monstre et sa barre

            backgroundBar.x = view.x + (size - barWidth) / 2f // Centre la barre de vie sous le monstre
            backgroundBar.y = view.y + myBarOffset

            healthBar.x = backgroundBar.x + BAR_MARGIN // Aligne la partie rouge exactement au même endroit
            healthBar.y = backgroundBar.y + BAR_MARGIN
        }
    }

    // Retire des points de vie et met à jour l'interface visuelle
    fun takeDamage(damage: Int): Boolean {
        if (backgroundBar.visibility == View.INVISIBLE) { // Affiche la barre de vie au premier coup reçu
            backgroundBar.visibility = View.VISIBLE
            healthBar.visibility = View.VISIBLE
        }

        health -= damage // Soustrait les dégâts
        val percentage = health.toFloat() / maxHealth.toFloat() // Calcule le ratio restant
        val maxRedWidth = barWidth - (2 * BAR_MARGIN) // Largeur totale de la partie rouge à 100%
        val targetWidth = (maxRedWidth * percentage).toInt() // Calcule la nouvelle largeur en pixels
        val currentWidth = healthBar.layoutParams.width // Récupère la taille actuelle avant l'animation

        val animator = ValueAnimator.ofInt(currentWidth, targetWidth) // Prépare l'animation de diminution
        animator.duration = 250 // Réduction fluide d'un quart de seconde
        animator.interpolator = DecelerateInterpolator() // Ralentit sur la fin pour l'effet visuel

        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            healthBar.layoutParams?.let { params -> // Met à jour la largeur de la vue frame par frame
                params.width = animatedValue
                healthBar.layoutParams = params
            }
        }
        animator.start() // Lance l'animation de la barre

        return health <= 0 // Renvoie vrai si le monstre n'a plus de vie du tout
    }

    // Fait exploser le monstre et génère éventuellement du butin
    fun destroy(drop: Boolean) {
        rotateAnimator?.cancel() // Stoppe la rotation pour figer le monstre
        rotateAnimator = null
        val randomPitch = (60..100).random() / 100f // Joue un son de mort plus ou moins grave
        AudioManager.playSoundEffect(R.raw.enemy_death_sfx, randomPitch, 0.1f)

        if (drop) { // Vérifie s'il faut faire apparaître des diamants
            val diamondCount = diamondRange.random() // Choisit combien en faire tomber

            for (i in 0 until diamondCount) { // Répète la création pour chaque diamant
                val centerX = view.x + (size - Diamond.SIZE) / 2 // Centre le point d'apparition
                val centerY = view.y + (size - Diamond.SIZE) / 2

                Diamond(view.context, parentLayout, centerX, centerY) { // Crée l'objet diamant avec sa logique
                    GameManager.diamondsCount += 1 // Incrémente la vraie valeur en mémoire quand le diamant est touché

                    val activity = view.context as android.app.Activity
                    val diamondLabel = activity.findViewById<TextView>(R.id.DiamondLabel) // Récupère le texte de l'UI
                    diamondLabel.text = GameManager.diamondsCount.toString() // Met à jour le compteur visuel à l'écran
                }
            }
        }

        // Animation d'explosion du monstre (il grossit et s'efface)
        view.animate()
            .scaleX(1.5f)
            .scaleY(1.5f)
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                parentLayout.removeView(view) // Nettoie complètement la vue et les barres de l'écran
                parentLayout.removeView(backgroundBar)
                parentLayout.removeView(healthBar)
            }
            .start()

        backgroundBar.animate().alpha(0f).setDuration(300).start() // Fait fondre la barre en même temps
        healthBar.animate().alpha(0f).setDuration(300).start()
    }

    fun pause() {
        rotateAnimator?.pause() // Fige la rotation quand le jeu est en pause
    }

    fun resume() {
        rotateAnimator?.resume() // Relance la rotation au retour en jeu
    }

    private fun setupView(x: Float, y: Float) {
        when (type) { // Assigne la bonne image et le bon centre de rotation selon la forme
            "triangle", "big_triangle" -> {
                view.setImageResource(R.drawable.triangle)
                view.pivotX = size / 2f // Décale le centre car le triangle n'est pas un carré parfait
                view.pivotY = size * 0.67f
            }
            "square", "big_square" -> {
                view.setImageResource(R.drawable.square) // Le carré tourne très bien par défaut
            }
            "pentagon", "big_pentagon" -> {
                view.setImageResource(R.drawable.pentagon)
                view.pivotX = size / 2f // Ajuste légèrement le pivot
                view.pivotY = size * 0.55f
            }
        }
        view.layoutParams = ConstraintLayout.LayoutParams(size, size) // Applique la taille calculée

        view.x = x // Place le monstre aux coordonnées initiales
        view.y = y
        view.elevation = 10f // Place l'ennemi au bon niveau de profondeur (sous les balles)
        view.translationZ = 0f
        parentLayout.addView(view) // L'affiche sur la zone de jeu
    }

    private fun setupHealthBar(x: Float, y: Float) {
        val myBarWidth = if (type == "big_triangle") BAR_WIDTH * 2 else BAR_WIDTH // Les boss ont une barre énorme
        val barX = x + (size - myBarWidth) / 2f // Centre la barre sous l'image
        val barY = y + size + 30f // Ajoute un petit décalage vers le bas

        // Configuration du conteneur de fond de la barre
        backgroundBar.layoutParams = ConstraintLayout.LayoutParams(barWidth, BAR_HEIGHT)
        backgroundBar.setBackgroundResource(R.drawable.health_bg)
        backgroundBar.x = barX
        backgroundBar.y = barY
        backgroundBar.elevation = 5f // La place un peu en dessous du monstre
        parentLayout.addView(backgroundBar)

        // Configuration de la jauge rouge
        val redWidth = barWidth - (2 * BAR_MARGIN) // Calcule l'espace intérieur
        val redHeight = BAR_HEIGHT - (2 * BAR_MARGIN)
        healthBar.layoutParams = ConstraintLayout.LayoutParams(redWidth, redHeight)

        healthBar.setBackgroundResource(R.drawable.health_fill)
        healthBar.x = barX + BAR_MARGIN // Décale légèrement pour centrer le rouge dans le blanc
        healthBar.y = barY + BAR_MARGIN
        healthBar.elevation = 6f // Met le rouge pile par-dessus le blanc
        parentLayout.addView(healthBar)

        backgroundBar.visibility = View.INVISIBLE // Cache la barre au début tant qu'il n'y a pas de dégâts
        healthBar.visibility = View.INVISIBLE
    }

    private fun startSpinning() {
        val spinDuration = when (type) { // Détermine la vitesse de rotation (les gros tournent très lentement)
            "triangle" -> 6000L
            "square" -> 10000L
            "pentagon" -> 16000L
            "big_triangle" -> 18000L
            "big_square" -> 20000L
            "big_pentagon" -> 22000L
            else -> 16000L
        }

        rotateAnimator = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f).apply { // Moteur d'animation qui fait tourner en boucle
            duration = spinDuration // Applique le temps qu'il faut pour faire un tour complet
            interpolator = LinearInterpolator() // Rotation constante sans à-coups
            repeatCount = ObjectAnimator.INFINITE // Ne s'arrête jamais
            start() // Lance la rotation
        }
    }
}