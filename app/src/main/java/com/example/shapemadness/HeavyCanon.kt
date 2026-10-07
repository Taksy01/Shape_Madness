package com.example.shapemadness

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

class HeavyCanon(
    view: ImageView,
    parentLayout: ConstraintLayout,
    collisionCallback: (Ball) -> Unit
) : Canon(view, parentLayout, collisionCallback) {

    override val fireRateMultiplier = 3f
    override val damageMultiplier = 2f
    override val velocityMultiplier = 0.8f
    override val ballSize = 2.8f
    override val isPiercing = true

    override fun updateVisuals() {
        view.setImageResource(R.drawable.canon_heavy)
    }

    override fun performShoot() {
        if (view.width == 0 || view.height == 0) return

        AudioManager.playSoundEffect(R.raw.shoot_sfx, 0.5f)

        createBall(view.rotation)
    }
}