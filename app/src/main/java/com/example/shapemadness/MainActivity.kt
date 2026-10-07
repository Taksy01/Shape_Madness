package com.example.shapemadness

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

// Gère l'écran principal du jeu au lancement de l'application
class MainActivity : AppCompatActivity() {

    private lateinit var settingsMenu: SettingsMenu // Prépare l'espace mémoire pour le menu des paramètres

    private lateinit var tutorialMenu: TutorialMenu // Prépare l'espace mémoire pour le tutoriel

    private lateinit var creditsMenu: CreditsMenu // Prépare l'espace mémoire pour les crédits

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge() // Étend l'affichage sur tout l'écran en ignorant les bordures noires
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE // Force le téléphone à rester en mode paysage
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) // Active le thème sombre par défaut pour éviter de cramer les yeux

        WindowCompat.getInsetsController(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars()) // Cache la barre d'état et la barre de navigation
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE // Permet de faire réapparaître les barres en glissant le doigt
        }

        setContentView(R.layout.activity_main) // Charge le fichier de design principal
        val mainLayout = findViewById<ConstraintLayout>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()) // Applique les marges de sécurité de l'écran
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom) // Évite que les boutons soient cachés par les bords physiques
            insets
        }

        settingsMenu = SettingsMenu(this, mainLayout) // Instancie l'objet du menu des paramètres
        tutorialMenu = TutorialMenu(this, mainLayout) // Instancie l'objet du tutoriel
        creditsMenu = CreditsMenu(this, mainLayout) // Instancie l'objet des crédits

        val playBtn = findViewById<ImageView>(R.id.IVPlayBtn) // Trouve le bouton pour lancer le jeu
        val helpBtn = findViewById<ImageView>(R.id.IVHelpBtn) // Trouve le bouton d'aide
        val settingsBtn = findViewById<ImageView>(R.id.IVSettingsBtn) // Trouve le bouton des paramètres
        val creditsBtn = findViewById<ImageView>(R.id.creditsButton) // Trouve le bouton pour voir les créateurs

        scaleBtnOnClick(playBtn, this, SelectModeActivity::class.java) // Ajoute l'animation de rebond et le lien vers la sélection de mode

        scaleBtnOnClick(helpBtn) // Ajoute l'animation au bouton d'aide
        helpBtn.setOnClickListener { tutorialMenu.show() } // Affiche le tuto au clic

        scaleBtnOnClick(settingsBtn) // Ajoute l'animation au bouton des réglages
        settingsBtn.setOnClickListener { settingsMenu.show() } // Ouvre le menu des paramètres au clic

        scaleBtnOnClick(creditsBtn) // Ajoute l'animation au bouton des crédits
        creditsBtn.setOnClickListener { creditsMenu.show() } // Affiche la page des créateurs au clic

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { // Remplace le comportement du bouton retour du téléphone
            override fun handleOnBackPressed() {
                when { // Vérifie s'il y a un menu d'ouvert avant de quitter l'application
                    settingsMenu.isShowing -> settingsMenu.hide() // Ferme les paramètres s'ils sont ouverts
                    tutorialMenu.isShowing -> tutorialMenu.hide() // Ferme le tuto s'il est ouvert
                    creditsMenu.isShowing -> creditsMenu.hide() // Ferme les crédits s'ils sont ouverts
                    else -> finish() // Quitte le jeu définitivement s'il n'y a rien d'autre à fermer
                }
            }
        })

        AudioManager.initSoundPool(this) // Charge tous les petits bruitages en mémoire pour éviter les lags plus tard
    }

    override fun onStart() {
        super.onStart()
        AudioManager.onActivityStart(this) // Lance la musique d'ambiance du menu principal au démarrage
    }

    override fun onStop() {
        super.onStop()
        AudioManager.onActivityStop() // Met la musique en pause si l'application passe en arrière-plan
    }
}