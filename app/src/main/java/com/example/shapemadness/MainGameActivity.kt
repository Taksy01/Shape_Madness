package com.example.shapemadness

import android.animation.ValueAnimator
import android.content.pm.ActivityInfo
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainGameActivity : AppCompatActivity(), SensorEventListener {

    lateinit var canon: Canon // Objet Canon du joueur
    val enemiesList = mutableListOf<Enemy>() // liste des enemies actifs

    private lateinit var sensorManager: SensorManager // Gestionnaire de capteurs
    private var gameRotationSensor: Sensor? = null // Capteur de rotation du jeu
    private lateinit var gameOverMenu: GameOverMenu // Menu de game over
    private val handler = Handler(Looper.getMainLooper()) // Gère les evenement (boucle 60fps)
    private lateinit var gameRunnable: Runnable // Definit la boucle du jeu
    private lateinit var playerHealthBar: PlayerHealthBar // Barre de vie du joueur
    private var isGameRunning = false // Verifier si le jeu tourne encore
    private var isGameOver = false // Verifie si le joueur est mort
    private var isGamePaused = false // verifie si le jeu est en pause
    private var waveAnimator: android.animation.ObjectAnimator? = null // Gère l'animation visuelle de l'annonce des vagues
    private lateinit var waveIndicatorLabel: TextView // indicateur de la vague
    private lateinit var pauseMenu: PauseMenu // Menu de pause
    private lateinit var settingsMenu: SettingsMenu // Menu des parametres
    private lateinit var waveManager: WaveManager // Gere les vagues du jeu
    private var isUpgradeMenuOpen = false // Verifie si le menu d'upgrade est ouvert
    private var isTutorialFinished = false // Verifie si le tutoriel au debut du jeu est fini
    private var tutorialAnimator: android.animation.ObjectAnimator? = null // Gere l'animation du tutoriel
    // Couts des upgrades de base
    private var costUpgradeDmg = 5
    private var costUpgradeSpd = 5
    private var costUpgradeCdr = 5
    private var isFirstTimeMusic = true // Indique si la musique doit être lancée pour la toute première fois (pour gerer le resume)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE // force paysage
        enableEdgeToEdge()

        // Cache les barres systemes
        WindowCompat.getInsetsController(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContentView(R.layout.activity_main_game)

        val mainLayout = findViewById<ConstraintLayout>(R.id.main)

        // Ajoute la marge
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // On affiche le menu pause si on fait retour
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (settingsMenu.isShowing) {
                    settingsMenu.hide()
                } else {
                    togglePause() // On ferme le menu pause s'il est deja ouvert
                }
            }
        })

        // boucle des 60 fps
        gameRunnable = object : Runnable {
            override fun run() {
                updateGame() // mise a jour UI
                if (isGameRunning) {
                    handler.postDelayed(this, 16) // rappel toute les 16ms (environ 60fps)
                    // on check les collisions
                    checkPlayerCollisions()
                    checkLaserCollisions()
                }
            }
        }

        settingsMenu = SettingsMenu(this, mainLayout) // Menu des parametres
        // Menu de pause
        pauseMenu = PauseMenu(
            activity = this,
            parentLayout = mainLayout,
            onResumeClicked = { togglePause() },
            onSettingsClicked = {
                settingsMenu.show()
            }
        )
        // Menu game over
        gameOverMenu = GameOverMenu(
            activity = this,
            parentLayout = mainLayout,
            onSettingsClicked = {
                settingsMenu.show()
            },
        )

        val gameLayer = findViewById<ConstraintLayout>(R.id.gameLayer) // conteneur des elements jeu
        val ivCanon = findViewById<ImageView>(R.id.IVcanon) // image du canon
        // classe de depart (Canon), changer pour commencer avec une autre
        canon = Canon(ivCanon, gameLayer) { ball ->
            checkBallCollisions(ball) // callback pour les collisions des balles
        }
        // Si le canon est un railgun on change ses collisions
        if (canon is RailgunCanon) {
            (canon as RailgunCanon).onFireCallback = {
                checkRailgunCollisions()
            }
        }

        playerHealthBar = PlayerHealthBar(this) // Barre de vie du joueur

        // Stats de base du jeu
        GameManager.playerCanon = canon.view
        GameManager.diamondsCount = 0
        GameManager.playerDamage = 10
        GameManager.fireRate = 500L
        GameManager.ballVelocity = 15f
        GameManager.dmgUpgradeBought = 0
        GameManager.spdUpgradeBought = 0
        GameManager.cdrUpgradeBought = 0

        // Gestionnaire de capteurs
        // TYPE_GAME_ROTATION_VECTOR car c'est beaucoup plus stable et fluide
        // Et ne depend pas du nord et des objets métaliques
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gameRotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        waveIndicatorLabel = findViewById(R.id.waveIndicatorLabel)
        // Gestionnaire des vagues
        waveManager = WaveManager(
            onSpawnEnemyRequested = { typeDemande ->
                spawnEnemy(typeDemande) // spawn les enemies selon le type demandé
            },
            onWaveStarted = { numeroVague ->
                showWaveAnimation(numeroVague) // animation de la vague
            },
            onWaveCleared = { waveJustCleared ->
                if (waveJustCleared == 10 || waveJustCleared == 20) {
                    handler.postDelayed({
                        if (!isGameOver) {
                            showLevelUpMenu(waveJustCleared) // on ouvre le menu lvl up si la wave clear = 20 ou 1°
                        }
                    }, 2000)
                } else {
                    handler.postDelayed({
                        if (isGameRunning && !isGameOver) {
                            playerHealthBar.heal(10) // on heal a chaque fin de vague
                        }
                    }, 2000)
                }
            }
        )
        waveManager.forceWave(0)
        // Bouton pause
        val btnPause = findViewById<View>(R.id.pause_button)
        scaleBtnOnClick(btnPause)
        btnPause.setOnClickListener {
            if (isGamePaused || isGameOver) return@setOnClickListener // On ne fait rien si le jeu est en pause ou game over
            togglePause()
        }

        // Menu level up
        val levelUpMenu = findViewById<ConstraintLayout>(R.id.innerContainer)
        levelUpMenu.visibility = View.GONE // on le cache de base

        val card1 = findViewById<ConstraintLayout>(R.id.card1) // carte de gauche
        val card2 = findViewById<ConstraintLayout>(R.id.card2) // carte du milieu
        val card3 = findViewById<ConstraintLayout>(R.id.card3) // carte de droite

        scaleBtnOnClick(card1)
        scaleBtnOnClick(card2)
        scaleBtnOnClick(card3)

        // Animation du tutoriel
        val tutorialImage = findViewById<ImageView>(R.id.tutorial)

        // On gere l'animation du tutoriel
        tutorialAnimator = android.animation.ObjectAnimator.ofFloat(tutorialImage, View.ALPHA, 1f, 0f).apply {
            startDelay = 2000L // delai de 2s au debut
            duration = 1000L // reste 1s

            // on utlilise un listener pour pouvoir pause l'animation
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    tutorialImage.visibility = View.GONE
                    isTutorialFinished = true
                    if (!isGameOver && !isGamePaused) {
                        isGameRunning = true
                        handler.post(gameRunnable) // on lance le jeu
                        canon.startShooting() // DEBUT DU TIR
                        if (canon is LaserCanon) AudioManager.startLaser()
                        waveManager.resume()
                    }
                }
            })
            start()
        }

        // Menu d'upgrade
        val upgradeMenu = findViewById<View>(R.id.upgradeMenu)
        val btnToggleMenu = findViewById<View>(R.id.panel_upgrade_extra) // bouton de rabat
        val arrowIcon = findViewById<View>(R.id.imageView3) // petite fleche du menu

        upgradeMenu.post { // attendre qu'il soit bien affiché
            val density = resources.displayMetrics.density
            val buttonWidthPx = 32f * density
            val hideDistancePx = (upgradeMenu.width - btnToggleMenu.right).toFloat()

            upgradeMenu.translationX = hideDistancePx
            arrowIcon.rotation = 180f

            btnToggleMenu.setOnClickListener {
                if (isGamePaused || isGameOver) return@setOnClickListener // ignore si pause

                // on anime le menu pour le ranger
                if (isUpgradeMenuOpen) {
                    upgradeMenu.animate()
                        .translationX(hideDistancePx)
                        .setDuration(300)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()

                    arrowIcon.animate().rotation(180f).setDuration(300).start()
                } else {
                    upgradeMenu.animate()
                        .translationX(0f)
                        .setDuration(300)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()

                    arrowIcon.animate().rotation(0f).setDuration(300).start()
                }

                isUpgradeMenuOpen = !isUpgradeMenuOpen
            }
        }

        // Bouton pour acheter upgrade DMG
        val btnBuyDmg = findViewById<View>(R.id.btn_buy_dmg)
        val txtPriceDmg = findViewById<TextView>(R.id.txt_price_dmg)
        val diamondLabel = findViewById<TextView>(R.id.DiamondLabel)
        val dmgUpgradeCount = findViewById<TextView>(R.id.dmgUpgradeCount)

        diamondLabel.text = GameManager.diamondsCount.toString()

        // Gestion uprgade DMG
        txtPriceDmg.text = costUpgradeDmg.toString() // on affiche le prix
        val maxDmgUp = 30 // Nombre max d'upgrade DMG

        btnBuyDmg.setOnClickListener {
            if (isGamePaused || isGameOver) return@setOnClickListener

            // Achat
            if (GameManager.diamondsCount >= costUpgradeDmg && GameManager.dmgUpgradeBought < maxDmgUp) {
                AudioManager.playSoundEffect(R.raw.upgrade_sfx, pitch = 1f)
                GameManager.diamondsCount -= costUpgradeDmg // On fait payer le prix
                GameManager.playerDamage += 1 // Augmentation de l'attaque
                GameManager.dmgUpgradeBought += 1 // on ajoute une upgrade achetée

                // Quand on est max
                if (GameManager.dmgUpgradeBought >= maxDmgUp) {
                    txtPriceDmg.text = "MAX"
                    dmgUpgradeCount.text = "${GameManager.dmgUpgradeBought}/$maxDmgUp"
                } else {
                    dmgUpgradeCount.text = "${GameManager.dmgUpgradeBought}/$maxDmgUp"
                    costUpgradeDmg = (costUpgradeDmg * 1.1 + 3).toInt() // Augmentation du prix
                    txtPriceDmg.text = costUpgradeDmg.toString()
                }
                diamondLabel.text = GameManager.diamondsCount.toString()

                // Animation du bouton
                it.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()
            } else {
                // Animation si on ne peut pas acheter
                it.animate().translationX(10f).setDuration(50).withEndAction {
                    it.animate().translationX(-10f).setDuration(50).withEndAction {
                        it.animate().translationX(0f).setDuration(50).start()
                    }.start()
                }.start()
            }
        }

        // On fait la meme chose pour le SPD
        val btnBuySpd = findViewById<View>(R.id.btn_buy_spd)
        val txtPriceSpd = findViewById<TextView>(R.id.txt_price_spd)
        val spdUpgradeCount = findViewById<TextView>(R.id.spdUpgradeCount)
        val maxSpdUp = 40

        txtPriceSpd.text = costUpgradeSpd.toString()

        btnBuySpd.setOnClickListener {
            if (isGamePaused || isGameOver) return@setOnClickListener
            if (GameManager.diamondsCount >= costUpgradeSpd && GameManager.spdUpgradeBought < maxSpdUp) {
                AudioManager.playSoundEffect(R.raw.upgrade_sfx, pitch = 1f)

                GameManager.diamondsCount -= costUpgradeSpd
                GameManager.ballVelocity += 0.5f
                GameManager.spdUpgradeBought += 1

                if (GameManager.spdUpgradeBought >= maxSpdUp) {
                    txtPriceSpd.text = "MAX"
                    spdUpgradeCount.text = "${GameManager.spdUpgradeBought}/$maxSpdUp"

                } else {
                    spdUpgradeCount.text = "${GameManager.spdUpgradeBought}/$maxSpdUp"

                    costUpgradeSpd = (costUpgradeSpd * 1.1 + 2).toInt()

                    txtPriceSpd.text = costUpgradeSpd.toString()
                }
                diamondLabel.text = GameManager.diamondsCount.toString()

                it.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()

            } else {
                it.animate().translationX(10f).setDuration(50).withEndAction {
                    it.animate().translationX(-10f).setDuration(50).withEndAction {
                        it.animate().translationX(0f).setDuration(50).start()
                    }.start()
                }.start()
            }
        }

        // On fait la meme chose pour le CDR
        val btnBuyCdr = findViewById<View>(R.id.btn_buy_cdr)
        val txtPriceCdr = findViewById<TextView>(R.id.txt_price_cdr)
        val cdrUpgradeCount = findViewById<TextView>(R.id.cdrUpgradeCount)
        val maxCdrUp = 50

        txtPriceCdr.text = costUpgradeCdr.toString()
        diamondLabel.text = GameManager.diamondsCount.toString()

        btnBuyCdr.setOnClickListener {
            if (isGamePaused || isGameOver) return@setOnClickListener

            if (GameManager.diamondsCount >= costUpgradeCdr && GameManager.cdrUpgradeBought < maxCdrUp) {
                AudioManager.playSoundEffect(R.raw.upgrade_sfx, pitch = 1f)

                GameManager.diamondsCount -= costUpgradeCdr
                GameManager.fireRate = (GameManager.fireRate * 0.96f).toLong()
                GameManager.cdrUpgradeBought += 1

                if (GameManager.cdrUpgradeBought >= maxCdrUp) {
                    txtPriceCdr.text = "MAX"
                    cdrUpgradeCount.text = "${GameManager.cdrUpgradeBought}/$maxCdrUp"

                } else {
                    cdrUpgradeCount.text = "${GameManager.cdrUpgradeBought}/$maxCdrUp"
                    costUpgradeCdr = (costUpgradeCdr * 1.1 + 1).toInt()
                    txtPriceCdr.text = costUpgradeCdr.toString()
                }
                diamondLabel.text = GameManager.diamondsCount.toString()

                it.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()
            } else {
                it.animate().translationX(10f).setDuration(50).withEndAction {
                    it.animate().translationX(-10f).setDuration(50).withEndAction {
                        it.animate().translationX(0f).setDuration(50).start()
                    }.start()
                }.start()
            }
        }

        // On lance la musique du jeu
        AudioManager.playGameMusic(this, restart = true)
    }

    override fun onResume() {
        super.onResume()
        // On rallume les capteur au resume
        gameRotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        // On rallume le laser au resume
        if (isTutorialFinished && !isGamePaused && !isGameOver) {
            if (canon is LaserCanon) AudioManager.startLaser()
        }

        // Gestion des pause musique
        canon.calibrate()
        if (isGameOver) {
            AudioManager.resumeDeathMusic(this)
            return
        }
        if (isGamePaused) {
            return
        }
        if (isFirstTimeMusic) {
            isFirstTimeMusic = false
            AudioManager.playGameMusic(this, restart = true)
        } else {
            AudioManager.playGameMusic(this)
        }

        // Resume du tutoriel
        if (!isTutorialFinished) {
            tutorialAnimator?.resume()
        } else {
            // on resume tout le jeu
            isGameRunning = true
            handler.post(gameRunnable)

            canon.startShooting()
            canon.resumeAllBalls()
            waveAnimator?.resume()
            enemiesList.forEach { it.resume() }
            waveManager.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        // On pause tout
        AudioManager.stopLaser()
        sensorManager.unregisterListener(this)
        AudioManager.pauseGameMusic()
        AudioManager.pauseDeathMusic()
        if (!isGameOver) {
            pauseMenu.show()
            tutorialAnimator?.pause()

            if (canon !is LaserCanon) {
                canon.stopShooting()
            }
            canon.pauseAllBalls()
            waveAnimator?.pause()
            enemiesList.toList().forEach { it.pause() }

            isGameRunning = false
            handler.removeCallbacks(gameRunnable)
            waveManager.stop()

            isGamePaused = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioManager.stopDeathMusic()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isGameOver || isGamePaused) return
        canon.updateRotation(event) // Le canon tourne
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // Fonction qui gère les degats AOE
    fun triggerAreaDamage(centerX: Float, centerY: Float, radius: Float, damage: Int) {
        val currentEnemies = enemiesList.toList() // on recup les enemies

        for (enemy in currentEnemies) {
            if (!enemiesList.contains(enemy)) continue

            val ex = enemy.view.x + (enemy.view.width / 2f)
            val ey = enemy.view.y + (enemy.view.height / 2f)

            val dx = centerX - ex
            val dy = centerY - ey
            val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat() // calcul de la distance

            if (distance <= radius) {
                val isDead = enemy.takeDamage(damage) // on inflige le degat

                if (isDead) {
                    val miniEnemySize = Enemy.SIZE
                    val perfectStartX = enemy.view.x + (enemy.size / 2f) - (miniEnemySize / 2f)
                    val perfectStartY = enemy.view.y + (enemy.size / 2f) - (miniEnemySize / 2f)

                    // On supprime l'enemy
                    enemiesList.remove(enemy)
                    enemy.destroy(true)

                    // Gestion des mini si boss
                    if (enemy.type.startsWith("big_")) {
                        val miniType = enemy.type.replace("big_", "")
                        waveManager.pendingMiniEnemies += 3
                        spawnMiniEnemies(perfectStartX, perfectStartY, miniType)
                    }
                    waveManager.checkWaveCleared(enemiesList.size)
                }
            }
        }
    }

    fun shakeScreen(duration: Long = 800L, maxIntensity: Float = 40f) {
        val gameLayer = findViewById<ConstraintLayout>(R.id.gameLayer)
        val animator = ValueAnimator.ofFloat(1f, 0f)
        animator.duration = duration
        animator.interpolator = android.view.animation.DecelerateInterpolator(1.2f) // Rapide puis lent

        var lastUpdate = 0L
        val shakeFrequency = 20L

        animator.addUpdateListener { valueAnimator ->
            val elapsed = valueAnimator.currentPlayTime
            val fade = valueAnimator.animatedValue as Float

            if (elapsed - lastUpdate >= shakeFrequency) {
                lastUpdate = elapsed

                val currentIntensity = (maxIntensity * fade).toInt()

                if (currentIntensity > 0) {
                    val randomX = ((-currentIntensity..currentIntensity).random()).toFloat()
                    val randomY = ((-currentIntensity..currentIntensity).random()).toFloat()

                    // Vibration
                    gameLayer.translationX = randomX
                    gameLayer.translationY = randomY
                }
            }
        }

        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                gameLayer.animate()
                    .translationX(0f)
                    .translationY(0f)
                    .setDuration(100)
                    .start()
            }
        })

        animator.start()
    }

    private fun updateGame() {
        // Fonction qui met a jour le comportement des element chaque frames
        println("DEBUG: Balles actives = ${canon.activeBalls.size}") // Debug pour voir les balles en vie
        val targetX = canon.view.x + canon.view.width / 2f
        val targetY = canon.view.y + canon.view.height / 2f

        for (i in enemiesList.indices.reversed()) {
            enemiesList[i].moveTowards(targetX, targetY) // avance les enemies vers le joueur
        }

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val ballsCopy = canon.activeBalls.toList()

        // On verifie si la balle est en dehors de l'ecran => supprime
        for (ball in ballsCopy) {
            if (ball.view.x < -200 || ball.view.x > screenWidth + 200 ||
                ball.view.y < -200 || ball.view.y > screenHeight + 200) {
                ball.destroy()
            }
        }
    }

    private fun spawnEnemy(enemyType: String) {
        // Spawn un enemy a un endroit random en haut de l'ecran
        val screenWidth = resources.displayMetrics.widthPixels
        val enemySize = 120
        val maxX = screenWidth - enemySize
        val randomX = (0..maxX).random().toFloat()
        val spawnY = -Enemy.SIZE.toFloat() * 2

        val newEnemy = Enemy(
            context = this,
            parentLayout = findViewById(R.id.gameLayer),
            type = enemyType,
            startX = randomX,
            startY = spawnY
        )

        enemiesList.add(newEnemy) // Ajout dans la liste
    }

    // Spawn des mini enemies si l'enemy tué est un boss
    private fun spawnMiniEnemies(startX: Float, startY: Float, miniType: String) {
        val gameLayout = findViewById<ConstraintLayout>(R.id.gameLayer)
        val scatterDistance = 150f

        for (i in 0 until 3) { // 3 enemy
            val miniEnemy = Enemy(this, gameLayout, miniType, startX, startY, true)
            miniEnemy.view.scaleX = 0f
            miniEnemy.view.scaleY = 0f

            val angle = (i * (2 * Math.PI / 3)) - (Math.PI / 2)

            val destX = startX + (Math.cos(angle) * scatterDistance).toFloat()
            val destY = startY + (Math.sin(angle) * scatterDistance).toFloat()

            // Animation d'ecartement du centre
            miniEnemy.view.animate()
                .x(destX)
                .y(destY)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(android.view.animation.DecelerateInterpolator(2f))
                .withEndAction {
                    enemiesList.add(miniEnemy)
                    waveManager.pendingMiniEnemies--
                }
                .start()
        }
    }

    // Fonction qui gère la mort des enemies
    private fun handleEnemyDeath(enemy: Enemy) {
        if (!enemiesList.contains(enemy)) return

        val miniSize = Enemy.SIZE
        val startX = enemy.view.x + (enemy.size / 2f) - (miniSize / 2f)
        val startY = enemy.view.y + (enemy.size / 2f) - (miniSize / 2f)

        enemy.destroy(true)
        enemiesList.remove(enemy)

        // Si enemy boss
        if (enemy.type.startsWith("big_")) {
            val miniType = enemy.type.replace("big_", "")
            waveManager.pendingMiniEnemies += 3
            spawnMiniEnemies(startX, startY, miniType)
        }

        waveManager.checkWaveCleared(enemiesList.size)
    }

    // Fonction qui gère les collisions des balles avec les enemies
    private fun checkBallCollisions(ball: Ball) {
        // Calcul de la taille de la balle
        val ballRadiusSq = ball.hitRadius * ball.hitRadius
        val ballX = ball.hitX
        val ballY = ball.hitY

        for (i in enemiesList.indices.reversed()) {
            val enemy = enemiesList[i]

            // Calcul de la taille de l'enemy
            val enemyRadius = enemy.view.width * 0.4f
            val enemyX = enemy.view.x + (enemy.view.width / 2f)
            val enemyY = enemy.view.y + (enemy.view.height / 2f)

            val dx = ballX - enemyX
            val dy = ballY - enemyY

            // Calcul de la distance
            val distanceSquared = dx * dx + dy * dy
            val radiusSum = ball.hitRadius + enemyRadius
            val radiusSumSquared = radiusSum * radiusSum

            if (distanceSquared < radiusSumSquared && !ball.hitEnemies.contains(enemy)) {
                ball.hitEnemies.add(enemy) // On conserve si l'enemy a déja été touché
                if (!ball.isPiercing) ball.destroy() // On supprime la balle

                AudioManager.playSoundEffect(R.raw.enemy_hit_sfx, (50..100).random() / 100f)

                if (enemy.takeDamage(ball.damage)) {
                    handleEnemyDeath(enemy)
                }
                if (!ball.isPiercing) break
            }

            // Gestion des collisions pour la classe orbit
            if (ball is OrbitBall) {
                val hitboxes = ball.getSatelliteHitboxes()
                for (sat in hitboxes) {
                    val satDx = sat.x - enemyX
                    val satDy = sat.y - enemyY
                    val satDistSq = satDx * satDx + satDy * satDy
                    val satRadiusSum = sat.radius + enemyRadius
                    val satRadiusSumSq = satRadiusSum * satRadiusSum

                    if (satDistSq < satRadiusSumSq && !sat.hitEnemies.contains(enemy)) {
                        sat.hitEnemies.add(enemy)
                        AudioManager.playSoundEffect(R.raw.enemy_hit_sfx, (50..100).random() / 100f)

                        if (enemy.takeDamage(ball.damage)) {
                            handleEnemyDeath(enemy)
                        }
                    }
                }
            }
        }
    }

    // Gestion des collisions pour le canon, différent des balles car collision constante
    private fun checkLaserCollisions() {
        if (canon !is LaserCanon) return

        // On recupère le laser du canon pour verifier s'il touche un enemy
        val activeLaser = (canon as LaserCanon).getLaser()
        val iterator = enemiesList.iterator()

        while (iterator.hasNext()) {
            val enemy = iterator.next()

            val enemyCenterX = enemy.view.x + (enemy.view.width / 2f)
            val enemyCenterY = enemy.view.y + (enemy.view.height / 2f)
            val enemyRadius = enemy.view.width * 0.4f // On reduit le radius pour nerf

            if (activeLaser.isHitting(enemyCenterX, enemyCenterY, enemyRadius)) {

                val isDead = enemy.takeDamage((GameManager.playerDamage.toFloat() / 10f * (500f / GameManager.fireRate.toFloat())).toInt())

                if (isDead) {
                    val miniEnemySize = Enemy.SIZE
                    val perfectStartX = enemy.view.x + (enemy.size / 2f) - (miniEnemySize / 2f)
                    val perfectStartY = enemy.view.y + (enemy.size / 2f) - (miniEnemySize / 2f)

                    enemy.destroy(true)
                    iterator.remove()

                    if (enemy.type.startsWith("big_")) {
                        val miniType = enemy.type.replace("big_", "")
                        waveManager.pendingMiniEnemies += 3
                        spawnMiniEnemies(perfectStartX, perfectStartY, miniType)
                    }
                    waveManager.checkWaveCleared(enemiesList.size)
                }
            }
        }
    }

    // Gestion des collisions pour le railgun
    private fun checkRailgunCollisions() {
        if (canon !is RailgunCanon) return
        // On fait shake de moins en moins fort si le fire rate est haut
        val fireRateRatio = GameManager.fireRate / 500f

        val dynamicIntensity = (50f * fireRateRatio).coerceAtLeast(10f)
        val dynamicDuration = (600L * fireRateRatio).toLong().coerceAtLeast(150L)

        shakeScreen(duration = dynamicDuration, maxIntensity = dynamicIntensity)

        // On recupère le rail pour verifier s'il touche un enemy
        val activeRail = (canon as RailgunCanon).getRail()
        var hitAny = false

        for (i in enemiesList.indices.reversed()) {
            val enemy = enemiesList[i]
            val enemyCenterX = enemy.view.x + (enemy.view.width / 2f)
            val enemyCenterY = enemy.view.y + (enemy.view.height / 2f)
            val enemyRadius = enemy.view.width * 0.4f

            if (activeRail.isHitting(enemyCenterX, enemyCenterY, enemyRadius)) {
                hitAny = true

                if (enemy.takeDamage((GameManager.playerDamage * 5f).toInt())) {
                    handleEnemyDeath(enemy)
                }
            }
        }
        // Si on touche un enemy, on joue un son à pitch random
        if (hitAny) {
            val randomPitch = (50..100).random() / 100f
            AudioManager.playSoundEffect(R.raw.enemy_hit_sfx, randomPitch)
        }
    }

    // Collisions entre enemy et joueur
    private fun checkPlayerCollisions() {
        val canonView = GameManager.playerCanon ?: return
        val flashView = findViewById<View>(R.id.damageFlash)

        val canonCenterX = canonView.x + (canonView.width / 2f)
        val canonCenterY = canonView.y + (canonView.height / 2f)
        val canonRadius = canonView.width / 3f

        for (i in enemiesList.indices.reversed()) {
            val enemy = enemiesList[i]

            val enemyCenterX = enemy.view.x + (enemy.view.width / 2f)
            val enemyCenterY = enemy.view.y + (enemy.view.height / 2f)
            val enemyRadius = enemy.view.width / 2f

            val dx = canonCenterX - enemyCenterX
            val dy = canonCenterY - enemyCenterY
            val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

            // Calcul de la distance entre canon et enemy
            if (distance < (canonRadius + enemyRadius)) {
                // on retire l'enemy
                val dmg = enemy.damageValue
                enemy.destroy(false)
                enemiesList.removeAt(i)

                // Flash rouge sur l'écran
                flashView?.let {
                    it.animate().alpha(0.2f).setDuration(5).withEndAction {
                        it.animate().alpha(0f).setDuration(400).start()
                    }.start()
                }

                // On compte quand mem l'enemy en tant que mort
                waveManager.checkWaveCleared(enemiesList.size)

                // Si le joueur n'a plus de vie il meurt
                if (playerHealthBar.takeDamage(dmg)) {
                    isGameOver = true
                    isGameRunning = false
                    handler.removeCallbacks(gameRunnable)
                    AudioManager.stopLaser()
                    canon.stopShooting()
                    canon.pauseAllBalls()
                    enemiesList.toList().forEach { it.pause() }
                    waveManager.stop()
                    waveAnimator?.pause()

                    triggerDeathAnimation()
                }
            }
        }
    }

    // Fonction qui affiche le menu d'evolution de classe
    private fun showLevelUpMenu(wave: Int) {
        isGamePaused = true
        isGameRunning = false

        canon.stopShooting()
        canon.pauseAllBalls()
        waveAnimator?.pause()
        enemiesList.toList().forEach { it.pause() }
        handler.removeCallbacks(gameRunnable)
        waveManager.stop()

        // On recup les elements de la ve
        val levelUpMenu = findViewById<ConstraintLayout>(R.id.innerContainer)

        val txt1 = findViewById<TextView>(R.id.card1_label)
        val txt2 = findViewById<TextView>(R.id.card2_label)
        val txt3 = findViewById<TextView>(R.id.card3_label)

        val img1 = findViewById<ImageView>(R.id.card1_canon)
        val img2 = findViewById<ImageView>(R.id.card2_canon)
        val img3 = findViewById<ImageView>(R.id.card3_canon)

        val card1 = findViewById<ConstraintLayout>(R.id.card1)
        val card2 = findViewById<ConstraintLayout>(R.id.card2)
        val card3 = findViewById<ConstraintLayout>(R.id.card3)

        // Vague 10 = choix des premieres classes simples
        if (wave == 10) {
            txt1.text = "Sniper"
            img1.setImageResource(R.drawable.canon_sniper)
            card1.setOnClickListener { changeCanonClass("Sniper") }

            txt2.text = "Machine Gun"
            img2.setImageResource(R.drawable.canon_machinegun)
            card2.setOnClickListener { changeCanonClass("MachineGun") }

            txt3.text = "Heavy"
            img3.setImageResource(R.drawable.canon_heavy)
            card3.setOnClickListener { changeCanonClass("Heavy") }

        }
        // Vague 20 = choix des secondes classes complexes
        else if (wave == 20) {
            when (canon) {
                is SniperCanon -> {
                    setupCards(
                        "Laser", R.drawable.canon_laser,
                        "Railgun", R.drawable.canon_railgun,
                        "Blaster", R.drawable.canon_blaster,
                        txt1, img1, card1, txt2, img2, card2, txt3, img3, card3
                    )
                }
                is MachineGunCanon -> {
                    setupCards(
                        "Gatling", R.drawable.canon_gatling,
                        "Barrage", R.drawable.canon_barrage,
                        "Splitter", R.drawable.canon_splitter,
                        txt1, img1, card1, txt2, img2, card2, txt3, img3, card3
                    )
                }
                is HeavyCanon -> {
                    setupCards(
                        "Reflector", R.drawable.canon_reflector,
                        "Meteor", R.drawable.canon_meteor,
                        "Orbit", R.drawable.canon_orbit,
                        txt1, img1, card1, txt2, img2, card2, txt3, img3, card3
                    )
                }
            }
        }
        // Petite animation
        levelUpMenu.visibility = View.VISIBLE
        levelUpMenu.alpha = 0f
        levelUpMenu.translationY = 300f
        levelUpMenu.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700)
            .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f))
            .start()

        AudioManager.playSoundEffect(R.raw.upgrade_sfx, pitch = 1.0f)
        canon.destroyAllBalls()
    }

    // Affiche les cartes des classes à choisir
    private fun setupCards(
        name1: String, res1: Int,
        name2: String, res2: Int,
        name3: String, res3: Int,
        t1: TextView, i1: ImageView, c1: View,
        t2: TextView, i2: ImageView, c2: View,
        t3: TextView, i3: ImageView, c3: View
    ) {
        t1.text = name1
        i1.setImageResource(res1)
        c1.setOnClickListener { changeCanonClass(name1) }

        t2.text = name2
        i2.setImageResource(res2)
        c2.setOnClickListener { changeCanonClass(name2) }

        t3.text = name3
        i3.setImageResource(res3)
        c3.setOnClickListener { changeCanonClass(name3) }
    }

    // Fonction qui fait changer de classe au moment ou le joueur choisit une carte
    private fun changeCanonClass(className: String) {
        val levelUpMenu = findViewById<ConstraintLayout>(R.id.innerContainer)

        // Animation
        levelUpMenu.animate()
            .alpha(0f)
            .translationY(300f)
            .setDuration(300)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                // On reprend le jeu
                levelUpMenu.visibility = View.GONE

                val ivCanon = findViewById<ImageView>(R.id.IVcanon)
                val gameLayer = findViewById<ConstraintLayout>(R.id.gameLayer)

                canon.stopShooting()

                // On change le canon fonction de la classe selectionnée
                canon = when (className) {
                    "Sniper" -> SniperCanon(ivCanon, gameLayer) { checkBallCollisions(it) }
                    "MachineGun" -> MachineGunCanon(ivCanon, gameLayer) { checkBallCollisions(it) }
                    "Heavy" -> HeavyCanon(ivCanon, gameLayer) { checkBallCollisions(it) }
                    "Laser" -> LaserCanon(ivCanon, gameLayer) { checkBallCollisions(it) }
                    "Railgun" -> {
                        val railgun = RailgunCanon(ivCanon, gameLayer) { checkBallCollisions(it) }
                        railgun.onFireCallback = {
                            checkRailgunCollisions()
                        }
                        railgun
                    }
                    "Blaster" -> BlasterCanon(ivCanon, gameLayer) { checkBallCollisions(it) }
                    "Gatling" -> GatlingCanon(ivCanon, gameLayer) { checkBallCollisions(it) }
                    "Barrage" -> BarrageCanon(ivCanon, gameLayer) { checkBallCollisions(it) }
                    "Splitter" -> SplitterCanon(ivCanon, gameLayer) { checkBallCollisions(it) }
                    "Reflector" -> ReflectorCanon(ivCanon, gameLayer) { checkBallCollisions(it) }
                    "Meteor" -> MeteorCanon(ivCanon, gameLayer) { checkBallCollisions(it) }
                    "Orbit" -> OrbitCanon(ivCanon, gameLayer) { checkBallCollisions(it) }
                    else -> Canon(ivCanon, gameLayer) { checkBallCollisions(it) }
                }

                isGamePaused = false
                isGameRunning = true
                canon.calibrate()
                canon.startShooting()
                if (canon is LaserCanon) AudioManager.startLaser()
                waveAnimator?.resume()
                enemiesList.toList().forEach { it.resume() }
                handler.post(gameRunnable)

                waveManager.isGameRunning = true
                waveManager.startWave()

            }.start()
    }

    // Animation WAVE X a chaque debut de vague
    private fun showWaveAnimation(waveNumber: Int) {
        val isNewRecord = SaveManager.checkAndSaveRecord(this, waveNumber)

        if (isNewRecord) {
            waveIndicatorLabel.setTextColor(android.graphics.Color.parseColor("#FFD700"))
        } else {
            waveIndicatorLabel.setTextColor(android.graphics.Color.WHITE)
        }

        val waveText = findViewById<TextView>(R.id.waveText)
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val waveString = "WAVE $waveNumber"

        waveIndicatorLabel.text = waveString

        // On crée un petit effet de "rebond" sur le label sous les diamants
        waveIndicatorLabel.animate()
            .scaleX(1.3f) // Il grossit un peu
            .scaleY(1.3f)
            .setDuration(150)
            .withEndAction {
                waveIndicatorLabel.animate()
                    .scaleX(1f) // Il reprend sa taille normale
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()

        val colorAnim = android.animation.ValueAnimator.ofObject(
            android.animation.ArgbEvaluator(),
            android.graphics.Color.parseColor("#FFAE00"), // Couleur Or au début
            android.graphics.Color.WHITE // Retour au blanc pur
        )
        colorAnim.duration = 600
        colorAnim.addUpdateListener { animator ->
            waveIndicatorLabel.setTextColor(animator.animatedValue as Int)
        }
        colorAnim.start()

        waveText.text  = waveString
        waveText.visibility = View.VISIBLE

        waveAnimator?.cancel()

        // On utilise des keyframe pour pouvoir pause l'animation en plein milieu
        val kfStart = android.animation.Keyframe.ofFloat(0f, screenWidth)
        val kfCenterIn = android.animation.Keyframe.ofFloat(0.25f, 0f)
        kfCenterIn.interpolator = android.view.animation.DecelerateInterpolator()
        val kfCenterOut = android.animation.Keyframe.ofFloat(0.75f, 0f)
        val kfEnd = android.animation.Keyframe.ofFloat(1f, -screenWidth)
        kfEnd.interpolator = android.view.animation.AccelerateInterpolator()

        val pvhTranslation = android.animation.PropertyValuesHolder.ofKeyframe(
            View.TRANSLATION_X, kfStart, kfCenterIn, kfCenterOut, kfEnd
        )

        waveAnimator = android.animation.ObjectAnimator.ofPropertyValuesHolder(waveText, pvhTranslation).apply {
            duration = 1600L

            var playedFirstWhoosh = false
            var playedSecondWhoosh = false
            // L'animation est composée de 2 partie, l'entrée et sortie
            addUpdateListener { animation ->
                val progress = animation.animatedFraction

                if (progress >= 0f && !playedFirstWhoosh) {
                    AudioManager.playSoundEffect(R.raw.whoosh_sfx, pitch = 1.0f)
                    playedFirstWhoosh = true
                }

                if (progress >= 0.5f && !playedSecondWhoosh) {
                    AudioManager.playSoundEffect(R.raw.whoosh_sfx, pitch = 0.8f)
                    playedSecondWhoosh = true
                }
            }

            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    waveText.visibility = View.INVISIBLE
                }
            })
            start()
        }
    }

    // Fonction qui fait basculer la pause du jeu
    private fun togglePause() {
        if (isGameOver) return
        if (isGamePaused) {
            isGamePaused = false
            pauseMenu.hide()
            AudioManager.playGameMusic(this)
            if (!isTutorialFinished) {
                tutorialAnimator?.resume()
            } else {
                isGameRunning = true
                if (canon !is LaserCanon) {
                    canon.startShooting()
                }
                canon.resumeAllBalls()
                waveAnimator?.resume()
                enemiesList.toList().forEach { it.resume() }
                if (canon is LaserCanon) AudioManager.startLaser()
                handler.post(gameRunnable)
                waveManager.resume()
            }

        } else {
            isGamePaused = true
            isGameRunning = false
            pauseMenu.show()
            AudioManager.stopLaser()
            AudioManager.pauseGameMusic()
            tutorialAnimator?.pause()

            if (canon !is LaserCanon) {
                canon.stopShooting()
            }
            canon.pauseAllBalls()
            waveAnimator?.pause()
            enemiesList.toList().forEach { it.pause() }
            handler.removeCallbacks(gameRunnable)
            waveManager.stop()
        }
    }

    // Animation de la mort, fade out au noir et blanc
    private fun triggerDeathAnimation() {
        val gameLayer = findViewById<ConstraintLayout>(R.id.gameLayer)

        val paint = Paint()
        val colorMatrix = ColorMatrix()

        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 1500L
            addUpdateListener { animation ->
                val saturation = animation.animatedValue as Float
                colorMatrix.setSaturation(saturation)
                paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
                gameLayer.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
            }
            start()
        }

        // On tourne l'ecran aléatoirement
        val randomAngle = (-15..15).random().toFloat()

        gameLayer.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .rotation(randomAngle)
            .setDuration(1500L)
            .setInterpolator(android.view.animation.DecelerateInterpolator(2f))
            .withEndAction {
                gameOverMenu.show()
            }
            .start()

        // On joue un son a la mort
        AudioManager.playSoundEffect(R.raw.death_sfx)
        val startVolume = 0.1f

        ValueAnimator.ofFloat(startVolume, 0f).apply {
            duration = 500L

            addUpdateListener { animation ->
                val currentVolume = animation.animatedValue as Float
                AudioManager.setGameMusicVolume(currentVolume)
            }

            // Reduction progressive du son du jeu
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    AudioManager.pauseGameMusic()
                    AudioManager.setGameMusicVolume(startVolume)

                    Handler(Looper.getMainLooper()).postDelayed({
                        AudioManager.playDeathMusic(this@MainGameActivity)
                    }, 500)
                }
            })
            start()
        }
    }
}