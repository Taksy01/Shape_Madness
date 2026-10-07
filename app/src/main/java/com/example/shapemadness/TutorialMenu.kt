package com.example.shapemadness

import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import android.text.style.ImageSpan
import androidx.core.content.ContextCompat
import android.text.style.DynamicDrawableSpan

// Modèle de données pour une page du guide de jeu
data class TutorialPage(
    val title: String, // Titre affiché en haut de la page
    val description: String, // Texte explicatif avec balises pour les icônes
    val imageResId: Int // Image illustrative associée
)

// Gère le carrousel d'aide et les explications visuelles pour le joueur ptn
class TutorialMenu(
    private val activity: AppCompatActivity, // L'écran qui affiche le menu
    private val parentLayout: ConstraintLayout // La zone de jeu pour l'insertion
) {
    private val view: View = LayoutInflater.from(activity).inflate(R.layout.layout_tutorial, parentLayout, false) // Charge le XML du tuto

    val isShowing: Boolean get() = view.isVisible // État actuel d'affichage du menu
    private var isAnimating = false // Sécurité pour empêcher le spam des boutons d'ouverture

    private val backgroundDim: View = view.findViewById(R.id.tutorialBackgroundDim) // Fond sombre transparent
    private val tutorialContainer: View = view.findViewById(R.id.tutorialContainer) // Boîte contenant les infos

    private val titleTv: TextView = view.findViewById(R.id.Credits) // Champ du titre de la page
    private val labelTv: TextView = view.findViewById(R.id.tutorialLabel) // Champ de la description stylisée
    private val imageIv: ImageView = view.findViewById(R.id.tutorialImage) // Image de démonstration
    private val numberPageTv: TextView = view.findViewById(R.id.numberPage) // Indicateur du numéro de page
    private val leftArrow: View = view.findViewById(R.id.leftArrow) // Flèche pour revenir en arrière
    private val rightArrow: View = view.findViewById(R.id.rightArrow) // Flèche pour avancer

    private val pages = listOf( // Liste ordonnée de toutes les pages d'aide du jeu
        TutorialPage("Device position", "Hold your device flat", R.drawable.tutorial1),
        TutorialPage("How to aim", "Rotate your phone to aim\nClick on the canon to reset orientation", R.drawable.tutorial2),
        TutorialPage("Diamonds", "Defeat enemies to earn [diamond]", R.drawable.tutorial3),
        TutorialPage("Upgrades panel", "Open/close the upgrades tab by tapping on the “UPGRADES” button", R.drawable.tutorial4),
        TutorialPage("Upgrades", "Use [diamond] to purchase upgrades", R.drawable.tutorial5),
        TutorialPage("Health", "You have 100 base HP\nYou heal 10 HP at the end of each waves", R.drawable.tutorial6),
        TutorialPage("Classes", "Select a level 2 class after beating wave 10\nSelect a level 3 class after beating wave 20", R.drawable.tutorial7),
        TutorialPage("Evolutions", "Level 3 classes depends on your level 2 classe!", R.drawable.tutorial8)
    )

    private var currentPageIndex = 0 // Index de la page actuellement consultée

    init {
        parentLayout.addView(view) // Intègre le menu à l'interface globale
        view.visibility = View.GONE
        view.elevation = 150f // Place le tuto au premier plan absolu

        updatePageContent() // Prépare la première page

        scaleBtnOnClick(leftArrow) // Ajoute l'effet de rebond sur la flèche gauche
        leftArrow.setOnClickListener {
            if (currentPageIndex > 0) {
                currentPageIndex-- // Recule d'une page
                updatePageContent()
            }
        }

        scaleBtnOnClick(rightArrow) // Ajoute l'effet de rebond sur la flèche droite
        rightArrow.setOnClickListener {
            if (currentPageIndex < pages.size - 1) {
                currentPageIndex++ // Avance à la page suivante
                updatePageContent()
            }
        }

        val btnClose = view.findViewById<View>(R.id.btnClose)
        btnClose?.let {
            scaleBtnOnClick(it)
            it.setOnClickListener { hide() } // Lance l'animation de fermeture
        }
    }

    // Applique les couleurs et insère les icônes dans le texte de la page
    private fun updatePageContent() {
        val currentPage = pages[currentPageIndex]
        val fullText = currentPage.description

        val colorMap = mapOf( // Associe des mots-clés à des couleurs spécifiques pour la lisibilité
            "upgrades" to Color.parseColor("#F00000"),
            "diamonds" to Color.parseColor("#00FFFF"),
            "HP" to Color.parseColor("#8AE300"),
            "level 2" to Color.parseColor("#FFD400"),
            "level 3" to Color.parseColor("#FFAE00"),
            "Level 3" to Color.parseColor("#FFAE00")
        )

        val builder = SpannableStringBuilder(fullText) // Outil pour modifier le style de parties de texte
        val diamondDrawable = ContextCompat.getDrawable(activity, R.drawable.diamond)

        if (diamondDrawable != null) {
            val drawableSize = labelTv.textSize.toInt() // Aligne la taille de l'icône sur celle de la police
            diamondDrawable.setBounds(0, 0, drawableSize, drawableSize)
            val placeholder = "[diamond]"
            var startIcon = fullText.indexOf(placeholder)

            while (startIcon != -1) { // Remplace chaque balise par l'image du diamant
                val endIcon = startIcon + placeholder.length
                val imageSpan = ImageSpan(diamondDrawable, DynamicDrawableSpan.ALIGN_BOTTOM)

                builder.setSpan(imageSpan, startIcon, endIcon, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                startIcon = fullText.indexOf(placeholder, endIcon)
            }
        }

        for ((word, color) in colorMap) { // Parcourt le texte pour colorer les mots importants
            var startIndex = fullText.indexOf(word)
            while (startIndex != -1) {
                val endIndex = startIndex + word.length
                builder.setSpan(ForegroundColorSpan(color), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                startIndex = fullText.indexOf(word, endIndex)
            }
        }

        titleTv.text = currentPage.title
        labelTv.text = builder
        imageIv.setImageResource(currentPage.imageResId) // Change l'image illustrative
        numberPageTv.text = "${currentPageIndex + 1} / ${pages.size}" // Met à jour le compteur de pages

        leftArrow.visibility = if (currentPageIndex == 0) View.INVISIBLE else View.VISIBLE // Cache la flèche au début
        rightArrow.visibility = if (currentPageIndex == pages.size - 1) View.INVISIBLE else View.VISIBLE // Cache la flèche à la fin
    }

    // Déclenche l'animation d'apparition du tutoriel
    fun show() {
        if (isAnimating || isShowing) return // Bloque l'action si le menu est déjà en mouvement
        isAnimating = true

        currentPageIndex = 0 // Réinitialise toujours à la première page
        updatePageContent()

        val screenHeight = view.context.resources.displayMetrics.heightPixels.toFloat()
        view.visibility = View.VISIBLE
        view.bringToFront()

        backgroundDim.alpha = 0f
        tutorialContainer.translationY = screenHeight // Positionne le menu en dehors de l'écran bas

        backgroundDim.animate().alpha(1f).setDuration(400).start()
        tutorialContainer.animate()
            .translationY(0f) // Fait remonter le menu
            .setDuration(450)
            .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f)) // Effet de freinage fluide
            .withEndAction { isAnimating = false }
            .start()
    }

    // Déclenche l'animation de disparition vers le bas
    fun hide() {
        if (isAnimating || !isShowing) return
        isAnimating = true

        val screenHeight = view.context.resources.displayMetrics.heightPixels.toFloat()
        backgroundDim.animate().alpha(0f).setDuration(300).start()

        tutorialContainer.animate()
            .translationY(screenHeight) // Fait redescendre le menu
            .setDuration(350)
            .setInterpolator(android.view.animation.AccelerateInterpolator(1.5f)) // Accélération vers le bas
            .withEndAction {
                view.visibility = View.GONE
                isAnimating = false
            }
            .start()
    }
}