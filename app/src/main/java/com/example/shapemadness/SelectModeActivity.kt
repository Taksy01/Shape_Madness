package com.example.shapemadness

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
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

// Gère le choix entre le mode de jeu normal et le mode bac à sable
class SelectModeActivity : AppCompatActivity() {
    private lateinit var waveLabel: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE // Force l'affichage horizontal pour la sélection

        enableEdgeToEdge() // Étend l'interface sous les barres système

        WindowCompat.getInsetsController(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars()) // Masque les boutons de navigation Android
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContentView(R.layout.activity_select_mode)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom) // Ajuste les marges selon les encoches du téléphone
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@SelectModeActivity, MainActivity::class.java) // Prépare le retour à l'accueil
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP // Évite de créer plusieurs instances du menu
                startActivity(intent)
                finish() // Ferme l'écran actuel proprement
                overridePendingTransition(R.anim.fade_from_black, R.anim.fade_to_black) // Transition fluide vers le noir
            }
        })

        var mode = "normal" // Variable locale pour suivre le mode sélectionné avant de lancer
        val backBtn = findViewById<ImageView>(R.id.IVbackbtn)
        val modePlayBtn = findViewById<ConstraintLayout>(R.id.btnClose) // Bouton pour valider et lancer la partie

        scaleBtnOnClick(backBtn) {
            onBackPressedDispatcher.onBackPressed() // Utilise la logique de retour définie plus haut
        }

        scaleBtnOnClick(modePlayBtn) {
            if (mode == "normal") {
                AudioManager.playSoundEffect(R.raw.start_sfx, 0.42f) // Joue un son de démarrage grave
                AudioManager.stopAndResetForGame() // Arrête la musique du menu pour laisser place aux sons de combat
                val intent = Intent(this, MainGameActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.fade_from_black, R.anim.fade_to_black)
            } else {
                // Emplacement pour la future logique du mode bac à sable
            }
        }

        waveLabel = findViewById<TextView>(R.id.WaveLabel)
        val sandboxBtn = findViewById<ConstraintLayout>(R.id.sandboxBtn) // Panneau pour choisir le mode Sandbox
        val normalBtn = findViewById<ConstraintLayout>(R.id.normalBtn) // Panneau pour choisir le mode Normal
        val modeBackground = findViewById<ConstraintLayout>(R.id.containerMode2) // Cadre visuel changeant selon le mode
        val modeImg = findViewById<ImageView>(R.id.modeImg) // Illustration du mode de jeu
        val selectedMode = findViewById<TextView>(R.id.selectedMode) // Texte affichant le nom du mode
        val playLabel = findViewById<TextView>(R.id.playLabel) // Texte du bouton de validation

        scaleBtnOnClick(sandboxBtn) {
            waveLabel.visibility = android.view.View.INVISIBLE
            modeBackground.setBackgroundResource(R.drawable.premium_container) // Change le décor pour le style premium
            modeImg.setImageResource(R.drawable.sandbox_img) // Change l'image pour le bac à sable
            selectedMode.text = "Sandbox"
            sandboxBtn.setBackgroundResource(R.drawable.premium_button) // Met en avant le bouton sélectionné
            normalBtn.setBackgroundResource(R.drawable.button) // Remet l'autre bouton en style normal
            playLabel.text = "\uD83D\uDD12" // Affiche un cadenas car le mode n'est pas encore prêt
            mode = "sandbox"
        }

        scaleBtnOnClick(normalBtn) {
            waveLabel.visibility = android.view.View.VISIBLE
            modeBackground.setBackgroundResource(R.drawable.container) // Remet le décor standard
            modeImg.setImageResource(R.drawable.normal_img) // Remet l'image du mode classique
            selectedMode.text = "Normal"
            normalBtn.setBackgroundResource(R.drawable.premium_button)
            sandboxBtn.setBackgroundResource(R.drawable.button)
            playLabel.text = "Play" // Autorise le lancement du jeu
            mode = "normal"
        }
    }

    override fun onStart() {
        super.onStart()
        AudioManager.onActivityStart(this) // Relance la musique du menu si on revient sur cet écran
    }

    override fun onStop() {
        super.onStop()
        AudioManager.onActivityStop() // Coupe la musique si on quitte l'application ou l'écran
    }

    override fun onResume() {
        super.onResume()
        val record = SaveManager.getHighScore(this)
        waveLabel.text = "High score : WAVE $record"
    }
}