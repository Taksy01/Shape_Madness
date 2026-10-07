package com.example.shapemadness

import android.content.Context
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

// Balle spéciale qui explose à l'impact pour faire des dégâts de zone
class BlasterBall(
    context: Context, // Le contexte pour créer les images
    parentLayout: ConstraintLayout, // La zone de jeu principale
    canonRotation: Float, // L'angle de tir
    canonX: Float, // Position horizontale du canon
    canonY: Float, // Position verticale du canon
    canonWidth: Int, // Largeur du canon
    canonHeight: Int, // Hauteur du canon
    damage: Int, // Dégâts de base du projectile
    velocityMod: Float, // Vitesse de la balle
    sizeMultiplier: Float, // Taille de la balle
    onMove: (Ball) -> Unit, // Callback pour le mouvement
    onDestroy: (Ball) -> Unit // Callback pour la destruction
) : Ball(context, parentLayout, canonRotation, canonX, canonY, canonWidth, canonHeight,
    damage, velocityMod, sizeMultiplier, false, onMove, onDestroy) {

    private val explosionRadius = 250f // Définit la taille de la zone d'effet de l'explosion en pixels
    private val explosionDamage = damage / 2 // Calcule les dégâts de l'explosion qui sont la moitié des dégâts normaux

    init {
        view.setImageResource(R.drawable.blaster_ball) // Charge le visuel spécifique de cette balle explosive
    }

    override fun destroy() {
        if (hitEnemies.isNotEmpty()) { // Vérifie si la balle a touché au moins un ennemi avant de disparaître
            triggerDoubleExplosion() // Lance l'animation et les dégâts de zone si l'impact est confirmé
        }
        super.destroy() // Appelle la destruction de la balle de base pour nettoyer la mémoire
    }

    // Gère l'animation visuelle en deux couches et l'application des dégâts de zone
    private fun triggerDoubleExplosion() {
        val activity = view.context as? MainGameActivity ?: return // Récupère l'activité pour utiliser ses fonctions globales
        val gameLayer = (view.parent as? ViewGroup) ?: return // Récupère la couche de jeu pour dessiner l'explosion au bon endroit

        val size = (explosionRadius * 2).toInt() // Calcule le diamètre total de l'image d'explosion
        val centerX = view.x + (view.width / 2) // Trouve le centre exact de la balle en X
        val centerY = view.y + (view.height / 2) // Trouve le centre exact de la balle en Y

        val density = activity.resources.displayMetrics.density // Récupère la densité de l'écran pour l'affichage
        val explosionZ = 16f * density // Calcule la profondeur pour que l'explosion soit bien visible au premier plan

        // Fonction interne pour fabriquer facilement les images de l'explosion
        fun createLayer(isReverse: Boolean): ImageView {
            val img = ImageView(activity) // Crée un conteneur d'image vide
            img.setImageResource(R.drawable.explosion) // Assigne le dessin de l'explosion
            img.layoutParams = ConstraintLayout.LayoutParams(size, size) // Applique la taille calculée
            img.x = centerX - (size / 2) // Centre l'image sur le point d'impact en X
            img.y = centerY - (size / 2) // Centre l'image sur le point d'impact en Y
            img.translationZ = explosionZ // Applique la profondeur calculée

            gameLayer.addView(img) // Ajoute l'image fraîchement créée sur l'écran
            return img
        }

        val l1 = createLayer(false) // Fabrique la première couche visuelle
        val l2 = createLayer(true) // Fabrique la deuxième couche pour donner du volume

        // Lance l'animation de la première couche qui grandit et tourne en s'effaçant
        l1.animate().scaleX(1.2f).scaleY(1.2f).rotation(90f).alpha(0f).setDuration(400)
            .setInterpolator(DecelerateInterpolator()).withEndAction { gameLayer.removeView(l1) }.start()

        // Lance l'animation de la deuxième couche qui tourne dans l'autre sens pour un effet stylé
        l2.animate().scaleX(1.6f).scaleY(1.6f).rotation(-120f).alpha(0f).setDuration(450)
            .setInterpolator(DecelerateInterpolator()).withEndAction { gameLayer.removeView(l2) }.start()

        activity.triggerAreaDamage(centerX, centerY, explosionRadius, explosionDamage) // Blesse tous les ennemis dans le rayon

        activity.shakeScreen(duration = 250L, 50f) // Fait trembler l'écran violemment pour donner une sensation de puissance
    }
}