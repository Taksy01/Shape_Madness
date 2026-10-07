package com.example.shapemadness

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import java.util.*

// Arme spéciale qui tire un rayon continu avec des effets visuels et des particules
class Laser(private val gameLayer: ConstraintLayout) {

    val view: ImageView = ImageView(gameLayer.context) // L'image du rayon principal long et fin
    private val lightView: ImageView = ImageView(gameLayer.context) // Le flash lumineux au bout du canon

    private var canonX = 0f // Garde en mémoire la position X du vaisseau
    private var canonY = 0f // Garde en mémoire la position Y du vaisseau
    private var currentAngle = 0f // Garde en mémoire l'inclinaison actuelle pour suivre la visée

    private var pulseAnimator: ObjectAnimator? = null // Moteur d'animation pour faire palpiter le rayon
    private var lightPulseAnimator: ObjectAnimator? = null // Moteur d'animation pour faire clignoter le bout du canon

    private val particleDrawables = listOf( // Liste des différentes images d'étincelles pour varier les effets
        R.drawable.laser_particle1,
        R.drawable.laser_particle2,
        R.drawable.laser_particle3,
        R.drawable.laser_particle4
    )
    private val random = Random() // Générateur de hasard pour l'apparition chaotique des particules
    private var lastParticleTime = 0L // Chronomètre pour ne pas créer trop d'étincelles en même temps

    private var lightSize = 0 // Taille calculée du flash lumineux
    private var canonRadiusOffset = 0f // Distance exacte entre le centre du vaisseau et la sortie du canon

    init {
        val context = gameLayer.context
        val density = context.resources.displayMetrics.density

        canonRadiusOffset = 40f * density // Calcule le décalage pour que la lumière apparaisse au bout du canon

        // --- 1. RAYON PRINCIPAL ---
        view.translationZ = 15f // Place le laser très haut pour qu'il passe par-dessus le canon et les monstres
        val drawable = context.resources.getDrawable(R.drawable.laser, null)
        view.setImageDrawable(drawable)

        val imageRatio = drawable.intrinsicHeight.toFloat() / drawable.intrinsicWidth.toFloat() // Conserve les proportions de l'image
        val widthPx = (60 * density).toInt()
        val heightPx = (widthPx * imageRatio).toInt()
        val params = ConstraintLayout.LayoutParams(widthPx, heightPx) // Applique la taille calculée

        view.layoutParams = params
        view.scaleType = ImageView.ScaleType.FIT_XY // Force l'image à remplir tout l'espace alloué
        view.visibility = View.INVISIBLE
        gameLayer.addView(view) // Ajoute le rayon à l'écran en mode caché

        view.post {
            view.pivotX = view.width / 2f // Place le point de pivotement exactement au milieu bas du rayon
            view.pivotY = view.height.toFloat()
        }

        // --- 2. LUMIÈRE (MUZZLE FLASH) ---
        lightView.translationZ = 100f // Le flash est l'élément le plus haut pour cacher la coupure du rayon
        lightView.setImageResource(R.drawable.laser_light)

        lightSize = (widthPx * 2.0f).toInt() // Fait la lumière deux fois plus large que le rayon pour bien couvrir
        val lightParams = ConstraintLayout.LayoutParams(lightSize, lightSize)
        lightView.layoutParams = lightParams

        lightView.visibility = View.INVISIBLE
        gameLayer.addView(lightView) // Ajoute le flash lumineux à l'écran

        // --- 3. ANIMATIONS ---
        val alphaPulse = PropertyValuesHolder.ofFloat(View.ALPHA, 0.7f, 1f) // Fait varier la transparence
        val scalePulse = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.9f, 1.1f) // Fait trembler l'épaisseur du rayon

        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(view, alphaPulse, scalePulse).apply { // Combine les deux effets
            duration = 60L // Animation très rapide pour un effet électrique
            repeatCount = ObjectAnimator.INFINITE // Ne s'arrête jamais
            repeatMode = ObjectAnimator.REVERSE // Fait des allers-retours fluides
            interpolator = LinearInterpolator()
        }

        val lightScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.8f, 1.2f) // Prépare les vibrations du flash
        val lightScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.9f, 1.2f)

        lightPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(lightView, alphaPulse, lightScaleX, lightScaleY).apply {
            duration = 300L // Le flash bat un peu plus lentement que le rayon
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = LinearInterpolator()
        }
    }

    fun fire() {
        view.visibility = View.VISIBLE // Affiche les éléments visuels quand le tir commence
        lightView.visibility = View.VISIBLE

        view.bringToFront() // Sécurité pour s'assurer que ça reste bien au-dessus du reste
        lightView.bringToFront()

        pulseAnimator?.start() // Lance les animations de tremblement et de lumière
        lightPulseAnimator?.start()
    }

    fun stop() {
        view.visibility = View.INVISIBLE // Cache le rayon pour arrêter le tir
        lightView.visibility = View.INVISIBLE
        pulseAnimator?.cancel() // Coupe les moteurs d'animation pour soulager le téléphone
        lightPulseAnimator?.cancel()
    }

    fun updatePosition(canonCenterX: Float, canonCenterY: Float, rotationAngle: Float) {
        if (view.height == 0) return // Bloque le calcul si le laser n'est pas encore bien chargé en mémoire

        this.canonX = canonCenterX // Met à jour les coordonnées stockées
        this.canonY = canonCenterY
        this.currentAngle = rotationAngle

        // Position du rayon principal
        view.x = canonCenterX - (view.width / 2f) // Centre parfaitement la base du rayon sur le vaisseau
        view.y = canonCenterY - view.height // Décale vers le haut pour que la base touche le centre
        view.rotation = rotationAngle // Aligne l'inclinaison

        // Utilise la trigonométrie pour trouver le point exact au bout du canon
        val angleRad = Math.toRadians(rotationAngle.toDouble())
        val dirX = Math.sin(angleRad).toFloat() // Calcule le vecteur directionnel en X
        val dirY = -Math.cos(angleRad).toFloat() // Calcule le vecteur directionnel en Y

        lightView.x = canonCenterX + (dirX * canonRadiusOffset) - (lightSize / 2f) // Place le flash pile à la sortie
        lightView.y = canonCenterY + (dirY * canonRadiusOffset) - (lightSize / 2f)
        lightView.rotation = rotationAngle

        if (view.visibility == View.VISIBLE) { // Ne génère des particules que si le laser est allumé
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastParticleTime > 10L) { // Limite la création à une étincelle toutes les 10 millisecondes
                lastParticleTime = currentTime
                createNewParticle()
            }
        }
    }

    private fun createNewParticle() {
        val pView = ImageView(gameLayer.context) // Crée une nouvelle image vide pour l'étincelle
        pView.setImageResource(particleDrawables[random.nextInt(particleDrawables.size)]) // Lui donne un dessin au hasard

        val density = gameLayer.resources.displayMetrics.density
        val baseSize = (120 * density).toInt()
        val finalSize = (baseSize * (0.8f + random.nextFloat() * 0.5f)).toInt() // Varie aléatoirement la taille de la particule
        pView.layoutParams = ConstraintLayout.LayoutParams(finalSize, finalSize)

        pView.translationZ = 16f // Place les particules au-dessus de tout le reste

        // Prépare les calculs pour placer l'étincelle n'importe où sur le rayon
        val angleRad = Math.toRadians(currentAngle.toDouble())
        val dirX = Math.sin(angleRad).toFloat() // Vecteur qui longe le rayon
        val dirY = -Math.cos(angleRad).toFloat()
        val perpX = -dirY // Vecteur perpendiculaire pour écarter l'étincelle sur les côtés
        val perpY = dirX

        // Spawn chaotique principalement concentré sur la base et le milieu du rayon
        val distanceLeLong = (view.height * 0.10f) + random.nextFloat() * (view.height * 0.56f)
        val ecartPerp = (random.nextFloat() - 0.5f) * 2 * (view.width * 0.2f) // Décale aléatoirement à gauche ou à droite

        pView.x = canonX + (dirX * distanceLeLong) + (perpX * ecartPerp) - (finalSize / 2f) // Applique la position mathématique
        pView.y = canonY + (dirY * distanceLeLong) + (perpY * ecartPerp) - (finalSize / 2f)
        pView.rotation = random.nextFloat() * 360f // Fait tourner l'étincelle sur elle-même au hasard

        gameLayer.addView(pView) // Affiche la particule à l'écran

        pView.animate() // Fait disparaître l'étincelle en l'agrandissant très vite
            .alpha(0f)
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(200L) // L'effet dure un cinquième de seconde
            .withEndAction { gameLayer.removeView(pView) } // Nettoie la mémoire une fois invisible
            .start()
    }

    fun isHitting(enemyCenterX: Float, enemyCenterY: Float, enemyRadius: Float): Boolean {
        if (view.visibility != View.VISIBLE) return false // Impossible de toucher si le laser est éteint

        // Calcule le vecteur directionnel du rayon pour les maths de collision
        val angleRad = Math.toRadians(currentAngle.toDouble())
        val dirX = Math.sin(angleRad).toFloat()
        val dirY = -Math.cos(angleRad).toFloat()

        val vX = enemyCenterX - canonX // Calcule la distance brute entre le vaisseau et le monstre en X
        val vY = enemyCenterY - canonY // Calcule la distance brute entre le vaisseau et le monstre en Y

        val distanceLeLongDuRayon = (vX * dirX) + (vY * dirY) // Produit scalaire pour savoir si le monstre est devant ou derrière
        if (distanceLeLongDuRayon < 0) return false // Ignore le monstre s'il est derrière le vaisseau

        val distanceEcart = Math.abs((vX * dirY) - (vY * dirX)) // Calcule l'écart perpendiculaire pour voir si le monstre frôle la ligne

        return distanceEcart <= (view.width * 0.1f) / 2f + enemyRadius // Renvoie vrai si la distance est plus petite que l'épaisseur du rayon et du monstre combinés
    }
}