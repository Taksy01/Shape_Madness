package com.example.shapemadness

import android.hardware.SensorEvent
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

// Arme spéciale qui utilise un rayon continu au lieu de tirer des balles indépendantes
class LaserCanon(
    view: ImageView, // L'image du vaisseau sur l'écran
    gameLayer: ConstraintLayout, // La zone de jeu pour dessiner le rayon
    onBallCreated: (Ball) -> Unit // Callback classique ignoré ici car le laser ne crée pas de balles
) : Canon(view, gameLayer, onBallCreated) {

    private val laser = Laser(gameLayer) // Instancie l'objet laser complexe qui gère les visuels et les particules

    override fun updateVisuals() {
        view.setImageResource(R.drawable.canon_laser) // Change l'apparence du vaisseau pour montrer le design futuriste
    }

    override fun startShooting() {
        laser.fire() // Allume le rayon en continu quand l'écran est touché
        updateLaserPosition() // Force le calcul immédiat de la position pour ne pas avoir de décalage visuel
    }

    override fun stopShooting() {
        laser.stop() // Coupe net le rayon principal et les animations quand on lâche l'écran
    }

    override fun performShoot() {
        // Reste totalement vide car le laser inflige des dégâts en continu dans la boucle principale du jeu
    }

    override fun updateRotation(event: SensorEvent?) {
        super.updateRotation(event) // Garde le calcul de base du gyroscope pour faire tourner le vaisseau
        updateLaserPosition() // Aligne le rayon laser sur la nouvelle inclinaison du téléphone en temps réel
    }

    fun getLaser(): Laser {
        return laser // Permet au moteur de jeu de récupérer le rayon pour calculer les collisions avec les monstres
    }

    private fun updateLaserPosition() {
        val cx = view.x + view.width / 2f // Calcule le centre exact du vaisseau en X
        val cy = view.y + view.height / 2f // Calcule le centre exact du vaisseau en Y

        laser.updatePosition(cx, cy, view.rotation) // Transmet les coordonnées pour que la base du rayon colle parfaitement au canon
    }
}