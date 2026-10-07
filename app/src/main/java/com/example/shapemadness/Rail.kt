package com.example.shapemadness

import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

// Gère l'effet visuel et la zone de dégâts d'un tir de type Railgun
class Rail(private val gameLayer: ConstraintLayout) {

    private val railDrawables = listOf( // Différentes images pour varier le look du rayon électrique
        R.drawable.rail1,
        R.drawable.rail2,
        R.drawable.rail3
    )

    private val particleDrawables = listOf( // Images d'étincelles pour l'impact au bout du canon
        R.drawable.rail_particle1,
        R.drawable.rail_particle2,
        R.drawable.rail_particle3
    )

    private var currentCanonX = 0f // Sauvegarde la position X pour calculer la collision après le tir
    private var currentCanonY = 0f // Sauvegarde la position Y pour calculer la collision après le tir
    private var currentAngle = 0f // Sauvegarde l'angle pour savoir où le rayon a frappé

    // Déclenche l'apparition du rayon et enregistre les données pour le moteur de jeu
    fun shoot(canonCenterX: Float, canonCenterY: Float, rotationAngle: Float, canonRadius: Float) {
        val context = gameLayer.context
        val density = context.resources.displayMetrics.density

        currentCanonX = canonCenterX // Enregistre les valeurs pour que la fonction isHitting soit précise
        currentCanonY = canonCenterY
        currentAngle = rotationAngle

        val angleRad = Math.toRadians(rotationAngle.toDouble()) // Convertit l'angle en radians pour la trigonométrie
        val dirX = Math.sin(angleRad).toFloat() // Direction horizontale du tir
        val dirY = -Math.cos(angleRad).toFloat() // Direction verticale du tir

        val tipX = canonCenterX + (dirX * canonRadius) // Calcule le point exact à la sortie du canon
        val tipY = canonCenterY + (dirY * canonRadius)

        // --- 1. LE RAYON ÉLECTRIQUE ---
        val beamView = ImageView(context)
        beamView.setImageResource(railDrawables.random()) // Choisit un dessin de rayon au hasard
        val widthPx = (40 * density).toInt()
        val heightPx = (500 * density).toInt() // Définit une longueur fixe très grande pour traverser l'écran
        beamView.layoutParams = ConstraintLayout.LayoutParams(widthPx, heightPx)

        beamView.scaleType = ImageView.ScaleType.FIT_XY
        beamView.translationZ = 50f // Place le tir au-dessus de tout le reste
        beamView.pivotX = widthPx / 2f // Fixe le point de pivot à la base du rayon
        beamView.pivotY = heightPx.toFloat()

        beamView.x = canonCenterX - (widthPx / 2f) // Aligne le rayon sur le vaisseau
        beamView.y = canonCenterY - heightPx
        beamView.rotation = rotationAngle

        gameLayer.addView(beamView)
        beamView.bringToFront()
        beamView.animate().alpha(0f).setDuration(600L).setInterpolator(AccelerateInterpolator()).withEndAction { gameLayer.removeView(beamView) }.start()

        // --- 2. LE FLASH LUMINEUX ---
        val lightView = ImageView(context)
        lightView.setImageResource(R.drawable.rail_light)
        val lightSize = (90 * density).toInt() // Taille du flash à la bouche du canon
        lightView.layoutParams = ConstraintLayout.LayoutParams(lightSize, lightSize)

        lightView.translationZ = 100f
        lightView.x = tipX - (lightSize / 2f) // Centre le flash sur la sortie du canon
        lightView.y = tipY - (lightSize / 2f)
        lightView.rotation = rotationAngle

        gameLayer.addView(lightView)
        lightView.bringToFront()
        lightView.animate().scaleX(1.5f).scaleY(1.5f).alpha(0f).setDuration(200L).setInterpolator(AccelerateInterpolator()).withEndAction { gameLayer.removeView(lightView) }.start()

        // --- 3. LA PARTICULE D'ÉNERGIE ---
        val particleView = ImageView(context)
        particleView.setImageResource(particleDrawables.random())
        val particleSize = (60 * density).toInt()
        particleView.layoutParams = ConstraintLayout.LayoutParams(particleSize, particleSize)

        particleView.translationZ = 90f
        particleView.x = tipX - (particleSize / 2f)
        particleView.y = tipY - (particleSize / 2f)
        particleView.rotation = rotationAngle

        gameLayer.addView(particleView)
        particleView.bringToFront()
        particleView.animate().alpha(0f).scaleX(1.8f).scaleY(1.8f).setDuration(500L).setInterpolator(AccelerateInterpolator()).withEndAction { gameLayer.removeView(particleView) }.start()
    }

    // Calcule mathématiquement si un ennemi est touché par le rayon instantané
    fun isHitting(enemyCenterX: Float, enemyCenterY: Float, enemyRadius: Float): Boolean {
        val density = gameLayer.context.resources.displayMetrics.density
        val beamWidth = 40 * density // Utilise la même épaisseur que le visuel pour la zone de choc

        val angleRad = Math.toRadians(currentAngle.toDouble())
        val dirX = Math.sin(angleRad).toFloat()
        val dirY = -Math.cos(angleRad).toFloat()

        val vX = enemyCenterX - currentCanonX // Vecteur de distance entre le canon et le monstre
        val vY = enemyCenterY - currentCanonY

        val distanceLeLongDuRayon = (vX * dirX) + (vY * dirY) // Vérifie si le monstre est bien devant le vaisseau
        if (distanceLeLongDuRayon < 0) return false // Ignore le monstre s'il est derrière le canon

        val distanceEcart = Math.abs((vX * dirY) - (vY * dirX)) // Calcule l'écart perpendiculaire par rapport à la trajectoire

        return distanceEcart <= (beamWidth / 2f) + enemyRadius // Renvoie vrai si la zone de collision du monstre touche le rayon
    }
}