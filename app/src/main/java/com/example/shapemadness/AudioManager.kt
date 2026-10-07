package com.example.shapemadness

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

// On sépare les musiques longues (MediaPlayer) des bruitages courts (SoundPool) pour éviter les lags
object AudioManager {

    var mustResetNextTime = false // Force la musique du menu à recommencer depuis le début à la prochaine ouverture

    // MediaPlayers séparés pour chaque ambiance car ils lisent les gros fichiers en continue
    private var mediaPlayer: MediaPlayer? = null
    private var gameMediaPlayer: MediaPlayer? = null
    private var deathMediaPlayer: MediaPlayer? = null

    private var activeActivitiesCount = 0 // Compteur pour savoir si l'appli est en arrière-plan et couper la musique
    private var laserStreamId = 0 // Stocke l'ID du son du laser en cours pour pouvoir l'arrêter proprement car il boucle

    // Le SoundPool charge les petits bruitages (SFX) en RAM pour pouvoir les jouer instantanément sans délai
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>()
    private var isSoundPoolReady = false

    // Prépare le moteur de bruitages et précharge tout en mémoire pour éviter un freeze a chaque son
    fun initSoundPool(context: Context) {
        if (soundPool != null) return

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attributes)
            .build()

        loadSound(context, R.raw.shoot_sfx)
        loadSound(context, R.raw.diamond_sfx1)
        loadSound(context, R.raw.diamond_sfx2)
        loadSound(context, R.raw.diamond_sfx3)
        loadSound(context, R.raw.death_sfx)
        loadSound(context, R.raw.enemy_death_sfx)
        loadSound(context, R.raw.enemy_hit_sfx)
        loadSound(context, R.raw.start_sfx)
        loadSound(context, R.raw.whoosh_sfx)
        loadSound(context, R.raw.upgrade_sfx)
        loadSound(context, R.raw.player_heal_sfx)
        loadSound(context, R.raw.player_hit_sfx)
        loadSound(context, R.raw.laser_shoot)
    }

    // Gère la musique du menu quand on arrive sur un écran
    // Si c'est le premier écran ouvert, on lance le son
    fun onActivityStart(context: Context) {
        activeActivitiesCount++

        if (activeActivitiesCount == 1) {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.menu_theme)
                mediaPlayer?.isLooping = true
                mediaPlayer?.setVolume(0.1f, 0.1f)
            } else {
                if (mustResetNextTime) {
                    mediaPlayer?.seekTo(0)
                    mustResetNextTime = false
                }
            }
            mediaPlayer?.start()
        }
    }

    // Coupe la musique du menu si on quitte complètement l'application (le compteur tombe à 0)
    fun onActivityStop() {
        activeActivitiesCount--

        if (activeActivitiesCount <= 0) {
            mediaPlayer?.pause()
            activeActivitiesCount = 0
        }
    }

    // Nettoie la mémoire du MediaPlayer principal quand on n'en a plus du tout besoin
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        activeActivitiesCount = 0
        mustResetNextTime = false
    }

    fun stopAndResetForGame() {
        mediaPlayer?.pause()
        mediaPlayer?.seekTo(0)
    }

    // Lance ou reprend la musique de combat
    fun playGameMusic(context: Context, restart: Boolean = false) {
        if (gameMediaPlayer == null) {
            gameMediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.game_theme1)
            gameMediaPlayer?.isLooping = true
            gameMediaPlayer?.setVolume(0.0f, 0.0f) // volume bas
        }

        if (restart) {
            gameMediaPlayer?.seekTo(0)
        }

        try {
            val params = android.media.PlaybackParams()
            params.pitch = 1.0f
            gameMediaPlayer?.playbackParams = params
        } catch (e: Exception) { e.printStackTrace() }

        gameMediaPlayer?.start()
    }

    fun pauseGameMusic() {
        gameMediaPlayer?.pause()
    }

    fun setGameMusicVolume(volume: Float) {
        gameMediaPlayer?.setVolume(volume, volume)
    }

    // Gère la musique de défaite On la recrée à chaque fois pour être sûr qu'elle reparte de zéro proprement
    fun playDeathMusic(context: Context) {
        stopDeathMusic()

        deathMediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.death_theme)
        deathMediaPlayer?.isLooping = false
        deathMediaPlayer?.setVolume(0.1f, 0.1f)

        deathMediaPlayer?.setOnCompletionListener {
            stopDeathMusic()
        }

        deathMediaPlayer?.start()
    }

    fun pauseDeathMusic() {
        deathMediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
    }

    fun resumeDeathMusic(context: Context) {
        deathMediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
            }
        }
    }

    fun stopDeathMusic() {
        deathMediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (e: Exception) { e.printStackTrace() }
            it.release()
        }
        deathMediaPlayer = null
    }

    // Joue un bruitage court préchargé depuis la RAM
    // On peut changer le pitch pour varier un peu le son sans charger un nouveau fichier
    fun playSoundEffect(resId: Int, pitch: Float = 1.0f, volume: Float = 0.2f) {
        val soundId = soundMap[resId]
        if (soundId != null) {
            soundPool?.play(soundId, volume, volume, 1, 0, pitch)
        }
    }

    fun startLaser() {
        if (laserStreamId != 0) return

        val soundId = soundMap[R.raw.laser_shoot]
        if (soundId != null) {
            laserStreamId = soundPool?.play(soundId, 0.2f, 0.2f, 1, -1, 1.0f) ?: 0
        }
    }

    fun stopLaser() {
        if (laserStreamId != 0) {
            soundPool?.stop(laserStreamId)
            laserStreamId = 0
        }
    }

    // Charge un fichier audio dans la RAM via le SoundPool et garde son ID dans un dictionnaire
    private fun loadSound(context: Context, resId: Int) {
        soundPool?.let { pool ->
            val soundId = pool.load(context, resId, 1)
            soundMap[resId] = soundId
        }
    }
}