package com.example.shapemadness

import android.hardware.SensorEvent
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

// Canon électromagnétique tirant un rayon instantané ultra puissant
class RailgunCanon(
    view: ImageView, // L'image du vaisseau sur l'écran
    gameLayer: ConstraintLayout, // La zone de jeu pour dessiner le rayon
    onBallCreated: (Ball) -> Unit // Callback standard ignoré car le railgun n'utilise pas de balles physiques
) : Canon(view, gameLayer, onBallCreated) {

    override val fireRateMultiplier = 4f // Délai de rechargement très long entre deux tirs

    override val damageMultiplier = 5f // Dégâts massifs infligés à tout ce qui traverse le rayon

    private val rail = Rail(gameLayer) // Instancie l'objet gérant le visuel et les collisions du rayon

    var onFireCallback: (() -> Unit)? = null // Signal envoyé au moteur de jeu pour déclencher la vérification des dégâts

    override fun updateVisuals() {
        view.setImageResource(R.drawable.canon_railgun) // Change le skin du vaisseau pour le modèle Railgun
    }

    override fun startShooting() {
        super.startShooting() // Utilise la boucle de tir automatique de la classe mère
    }

    override fun stopShooting() {
        super.stopShooting() // Arrête la boucle de tir quand l'écran est lâché
    }

    // Permet au moteur de jeu d'accéder aux données du rayon pour les collisions
    fun getRail(): Rail = rail

    override fun performShoot() {
        if (view.width == 0 || view.height == 0) return // Sécurité pour ne pas tirer si le vaisseau n'est pas prêt

        AudioManager.playSoundEffect(R.raw.shoot_sfx, pitch = 0.5f) // Joue un son de décharge électrique très grave

        val cx = view.x + view.width / 2f // Centre horizontal du vaisseau
        val cy = view.y + view.height / 2f // Centre vertical du vaisseau
        val canonRadius = view.height / 2f // Distance pour faire partir le rayon du bout du canon

        rail.shoot(cx, cy, view.rotation, canonRadius) // Déclenche l'apparition visuelle du rayon instantané

        onFireCallback?.invoke() // Prévient le gestionnaire de collisions qu'un tir vient d'avoir lieu
    }

    override fun updateRotation(event: SensorEvent?) {
        super.updateRotation(event) // Met à jour l'angle du vaisseau via le gyroscope
    }
}