package com.example.shapemadness

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.cos
import kotlin.math.sin

// Projectile enflammé avec une traînée de fumée et une aura lumineuse
class MeteorBall(
    context: Context, // Contexte pour créer les vues
    parentLayout: ConstraintLayout, // Zone d'affichage du jeu
    canonRotation: Float, // Angle de tir
    canonX: Float, // Position de départ en X
    canonY: Float, // Position de départ en Y
    canonWidth: Int, // Largeur du canon
    canonHeight: Int, // Hauteur du canon
    damage: Int, // Dégâts infligés
    velocityMod: Float, // Vitesse du météore
    sizeMultiplier: Float, // Taille du projectile
    isPiercing: Boolean, // Capacité à traverser les ennemis
    onMove: (Ball) -> Unit, // Action au déplacement
    onDestroy: (Ball) -> Unit // Action à la destruction
) : Ball(
    context, parentLayout,
    canonRotation, canonX, canonY, canonWidth, canonHeight,
    damage, velocityMod, sizeMultiplier, isPiercing,
    onMove, onDestroy
) {
    private val particleHandler = Handler(Looper.getMainLooper()) // Gestionnaire pour exécuter l'animation en boucle
    private var particleRunnable: Runnable? = null // Tâche qui contient la logique de l'animation

    private val canonRotationSaved = canonRotation // Sauvegarde l'angle initial pour garder la fumée alignée

    private val travelAngleRad = Math.toRadians((canonRotation - 90).toDouble()) // Calcule l'angle en radians pour la trigonométrie
    private val travelDirX = cos(travelAngleRad).toFloat() // Vecteur directionnel X pour reculer la fumée derrière le météore
    private val travelDirY = sin(travelAngleRad).toFloat() // Vecteur directionnel Y pour reculer la fumée derrière le météore

    private val animationDrawables = listOf( // Les 10 images successives pour animer le cœur du météore
        R.drawable.meteor1, R.drawable.meteor2, R.drawable.meteor3,
        R.drawable.meteor4, R.drawable.meteor5, R.drawable.meteor6,
        R.drawable.meteor7, R.drawable.meteor8, R.drawable.meteor9, R.drawable.meteor10
    )
    private var currentFrameIndex = 0 // Garde en mémoire l'image actuellement affichée

    private val smokeDrawables = listOf( // Les différentes images possibles pour la traînée de fumée
        R.drawable.meteor_smoke1,
        R.drawable.meteor_smoke2,
        R.drawable.meteor_smoke3
    )
    private var smokeIndex = 0 // Compteur pour alterner les nuages de fumée

    private var glowView: View? = null // L'objet visuel qui sert de halo lumineux
    private val glowSize get() = (ballSize * 3.5f).toInt() // Calcule une aura bien plus grande que le projectile lui-même

    init {
        view.rotation = canonRotationSaved // Oriente l'image du météore dans le sens du tir

        view.scaleX = 0.2f // Fait commencer le projectile en tout petit
        view.scaleY = 0.2f
        glowView?.scaleX = 0.2f // Fait pareil pour l'aura lumineuse
        glowView?.scaleY = 0.2f

        view.animate() // Lance une animation fluide pour atteindre la taille max rapidement
            .scaleX(2.0f) // Double la taille finale du sprite
            .scaleY(2.0f)
            .setDuration(200L) // L'apparition dure un cinquième de seconde
            .start()

        glowView?.let { glow -> // Fait grandir l'aura en même temps de manière sécurisée
            glow.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(200L)
                .start()
        }

        startAnimationAndTrail() // Déclenche la boucle d'effets visuels
    }

    override fun setupView() {
        super.setupView()

        view.setImageResource(R.drawable.meteor1) // Charge la toute première image de l'animation par défaut
        view.translationZ = 15f // Place le météore bien au-dessus des balles classiques
        view.elevation = 2f

        createGlow() // Génère l'aura lumineuse dès le départ
    }

    override fun launch(rotation: Float, cX: Float, cY: Float, cW: Int, cH: Int) {
        super.launch(rotation, cX, cY, cW, cH)

        val forwardOffset = ballSize * 0.6f // Calcule un décalage vers l'avant pour bien centrer la lumière sur la tête du météore

        animator?.addUpdateListener { // Synchronise parfaitement la position du halo avec celle de la balle frame par frame
            glowView?.x = view.x + (ballSize / 2f) - (glowSize / 2f) + (travelDirX * forwardOffset)
            glowView?.y = view.y + (ballSize / 2f) - (glowSize / 2f) + (travelDirY * forwardOffset)
        }
    }

    override fun destroy() {
        particleRunnable?.let { particleHandler.removeCallbacks(it) } // Coupe proprement la boucle d'animation pour libérer la mémoire
        particleRunnable = null

        glowView?.let { parentLayout.removeView(it) } // Supprime le halo lumineux de l'écran quand le météore disparaît
        glowView = null

        super.destroy() // Appelle la destruction standard de la balle mère
    }

    private fun createGlow() {
        glowView = View(view.context).apply {
            layoutParams = ConstraintLayout.LayoutParams(glowSize, glowSize) // Applique la grande taille calculée

            val gradient = GradientDrawable() // Prépare un dégradé circulaire pour faire un effet de lumière
            gradient.shape = GradientDrawable.OVAL
            gradient.gradientType = GradientDrawable.RADIAL_GRADIENT
            gradient.gradientRadius = (glowSize / 2f).coerceAtLeast(1f) // Sécurise le rayon minimum
            gradient.colors = intArrayOf(
                Color.parseColor("#60FF8C00"), // Centre incandescent très visible
                Color.parseColor("#20AB2A00"), // Halo périphérique plus doux
                Color.TRANSPARENT              // Fondu total sur les bords externes
            )
            background = gradient

            translationZ = 14f // Place la lumière juste en dessous du météore solide

            parentLayout.addView(this) // Ajoute l'effet au jeu
        }
    }

    private fun startAnimationAndTrail() {
        particleRunnable = object : Runnable {
            override fun run() {
                currentFrameIndex = (currentFrameIndex + 1) % animationDrawables.size // Passe à l'image suivante en bouclant à zéro
                view.setImageResource(animationDrawables[currentFrameIndex]) // Met à jour l'apparence du météore

                glowView?.alpha = if (currentFrameIndex % 2 == 0) 1.0f else 0.8f // Fait pulser l'aura lumineuse une frame sur deux

                if (currentFrameIndex % 2 == 0) { // Génère un nuage de fumée périodiquement
                    spawnSmoke()
                }

                particleHandler.postDelayed(this, 30L) // Relance la boucle très rapidement pour un rendu nerveux
            }
        }
        particleHandler.post(particleRunnable!!) // Démarre le cycle
    }

    private fun spawnSmoke() {
        val size = (ballSize * 1.3f).toInt() // Calcule une taille généreuse pour le nuage
        val behindOffset = ballSize * -0.2f // Prépare le décalage vers l'arrière

        val centerX = view.x + ballSize / 2f - travelDirX * behindOffset // Place le nuage dans le sillage exact du projectile
        val centerY = view.y + ballSize / 2f - travelDirY * behindOffset

        val smoke = ImageView(view.context).apply {
            setImageResource(smokeDrawables[smokeIndex % smokeDrawables.size]) // Alterne le design de la fumée
            layoutParams = ConstraintLayout.LayoutParams(size, size)
            x = centerX - size / 2f
            y = centerY - size / 2f
            rotation = canonRotationSaved // Oriente la fumée selon l'angle de tir initial
            alpha = 0.50f // Rend la fumée semi-transparente dès le départ
            elevation = 1f
        }

        parentLayout.addView(smoke, 0) // Ajoute la fumée tout au fond de l'écran

        smoke.animate() // Lance la dissipation du nuage
            .alpha(0f) // S'efface complètement
            .scaleX(0.2f) // Se rétracte en disparaissant
            .scaleY(0.2f)
            .setDuration(350L) // Dure un tiers de seconde environ
            .withEndAction { parentLayout.removeView(smoke) } // Nettoie l'image morte pour éviter les lags
            .start()

        smokeIndex++ // Prépare le prochain design de fumée
    }
}