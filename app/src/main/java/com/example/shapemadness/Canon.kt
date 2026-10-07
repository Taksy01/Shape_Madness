package com.example.shapemadness

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout

// Classe mère qui gère le canon de base et son gyroscope
open class Canon(
    val view: ImageView, // L'image du vaisseau sur l'écran
    protected val parentLayout: ConstraintLayout, // La zone de jeu pour faire spawner les tirs
    protected val collisionCallback: (Ball) -> Unit // La fonction à appeler quand un tir touche une cible
) {
    // Variables de rotation
    private var lastAngle = 0f
    private var startOffset: Float? = null
    private val smoothingFactor = 0.1f // Adoucit le mouvement du téléphone pour éviter les tremblements visuels

    // Variables de tir
    private var lastShotTime = 0L
    private var shootingRunnable: Runnable? = null
    protected val handler = Handler(Looper.getMainLooper()) // Le chronomètre qui tourne en fond pour gérer la cadence
    val activeBalls = mutableListOf<Ball>() // Garde en mémoire toutes les balles actuellement en vol
    private var timeWhenPaused = 0L
    private var cooldownElapsed = 0L

    open val fireRateMultiplier: Float = 1.0f // Cadence de tir modifiable par les futurs canons enfants
    open val damageMultiplier: Float = 1.0f
    open val velocityMultiplier: Float = 1.0f
    open val ballSize: Float = 1.0f
    open val isPiercing: Boolean = false // Indique si la balle de base traverse les cibles ou explose

    init {
        view.setOnClickListener { calibrate() } // Recalibre le gyroscope au centre avec un simple tap sur le vaisseau
        updateVisuals()
    }

    open fun updateVisuals() {
        // Fonction vide que les autres canons peuvent modifier pour changer leur apparence
    }

    open fun startShooting() {
        if (shootingRunnable != null) return // Empêche de lancer deux chronomètres en même temps pour éviter la triche

        // Calcule le temps restant avant le prochain tir en prenant en compte une éventuelle pause
        lastShotTime = System.currentTimeMillis() - cooldownElapsed

        shootingRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis() // Récupère l'heure exacte en millisecondes
                val timeSinceLastShot = currentTime - lastShotTime
                val actualFireRate = (GameManager.fireRate * fireRateMultiplier).toLong() // Applique les modificateurs de vitesse

                if (timeSinceLastShot >= actualFireRate) { // Vérifie si le canon a rechargé et est prêt à tirer
                    performShoot()
                    lastShotTime = System.currentTimeMillis()
                    handler.postDelayed(this, actualFireRate) // Relance le chronomètre pour le tir suivant
                } else {
                    val remainingTime = actualFireRate - timeSinceLastShot
                    handler.postDelayed(this, remainingTime) // Attend le temps restant si le tir était presque prêt avant la pause
                }
            }
        }
        handler.post(shootingRunnable!!)
    }

    open fun stopShooting() {
        // Sauvegarde le temps écoulé pour ne pas réinitialiser la cadence de tir après un passage dans le menu
        cooldownElapsed = System.currentTimeMillis() - lastShotTime

        shootingRunnable?.let { handler.removeCallbacks(it) } // Coupe net le chronomètre de tir
        shootingRunnable = null
    }

    open fun performShoot() {
        if (view.width == 0 || view.height == 0) return // Bloque le tir si le vaisseau n'est pas encore totalement chargé à l'écran

        val randomPitch = (90..110).random() / 100f // Change un peu la fréquence du son à chaque fois pour pas rendre fou
        AudioManager.playSoundEffect(R.raw.shoot_sfx, randomPitch)

        createBall(view.rotation) // Fabrique la balle exactement dans la direction actuelle
    }

    protected fun createBall(angle: Float) {
        val ball = Ball(
            context = view.context,
            parentLayout = parentLayout,
            canonRotation = angle, // Donne l'angle au projectile pour qu'il file tout droit
            canonX = view.x,
            canonY = view.y,
            canonWidth = view.width,
            canonHeight = view.height,
            damage = (GameManager.playerDamage * damageMultiplier).toInt(), // Applique les améliorations du joueur
            velocityMod = velocityMultiplier,
            sizeMultiplier = ballSize,
            isPiercing = isPiercing,
            onMove = collisionCallback, // Transmet la détection de collision au moteur du jeu
            onDestroy = { deadBall -> activeBalls.remove(deadBall) } // Nettoie proprement la liste quand la balle meurt
        )
        activeBalls.add(ball)
    }

    protected fun createBallWithOffset(angle: Float, lateralOffset: Float) {
        val angleRad = Math.toRadians(angle.toDouble()) // Convertit l'angle en radians pour faire des calculs trigonométriques
        val offsetX = (lateralOffset * Math.cos(angleRad)).toFloat() // Calcule le décalage sur les côtés
        val offsetY = (lateralOffset * Math.sin(angleRad)).toFloat()

        val ball = Ball(
            context = view.context,
            parentLayout = parentLayout,
            canonRotation = angle,
            canonX = view.x + offsetX, // Applique le décalage pour que la balle sorte du bon trou du canon
            canonY = view.y + offsetY,
            canonWidth = view.width,
            canonHeight = view.height,
            damage = (GameManager.playerDamage * damageMultiplier).toInt(), // Calcule les dégâts finaux
            velocityMod = velocityMultiplier,
            isPiercing = isPiercing,
            sizeMultiplier = ballSize,
            onMove = collisionCallback,
            onDestroy = { deadBall -> activeBalls.remove(deadBall) } // Supprime de la mémoire vive pour éviter les lags
        )
        activeBalls.add(ball)
    }

    fun calibrate() {
        startOffset = null // Remet l'angle de base à zéro pour centrer le vaisseau sur la position actuelle du téléphone
    }

    open fun updateRotation(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GAME_ROTATION_VECTOR) { // Vérifie que l'information vient bien du gyroscope du téléphone
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values) // Transforme les données brutes du capteur
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val azimuthRad = orientationAngles[0]
            val angleDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat() // Convertit les radians incompréhensibles en degrés lisibles
            val currentSensorAngle = -angleDeg

            if (startOffset == null) startOffset = currentSensorAngle // Sauvegarde la position de départ au tout premier mouvement

            val targetAngle = currentSensorAngle - (startOffset ?: 0f) // Calcule la différence avec le point de départ défini

            var deltaAngle = targetAngle - lastAngle
            while (deltaAngle < -180) deltaAngle += 360 // Empêche le vaisseau de faire un tour complet sur lui-même d'un coup sec
            while (deltaAngle > 180) deltaAngle -= 360

            lastAngle += deltaAngle * smoothingFactor // Applique la rotation en douceur grâce au facteur de lissage

            var finalAngle = -lastAngle

            if (GameManager.invertRotation) {
                finalAngle = -finalAngle // Inverse les contrôles de direction si l'option est cochée dans les paramètres
            }

            view.rotation = finalAngle
        }
    }

    fun destroyAllBalls() {
        val ballsToDestroy = activeBalls.toList() // Copie la liste pour éviter un plantage pendant la suppression de masse
        ballsToDestroy.forEach { it.destroy() }
        activeBalls.clear() // Vide la mémoire complètement pour faire place nette
    }

    fun pauseAllBalls() { activeBalls.forEach { it.pause() } } // Fige toutes les balles à l'écran lors d'une mise en pause

    fun resumeAllBalls() { activeBalls.forEach { it.resume() } } // Relance le mouvement des balles quand le jeu reprend
}