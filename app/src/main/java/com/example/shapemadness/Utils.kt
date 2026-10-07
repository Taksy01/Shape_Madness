package com.example.shapemadness

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

// Ajoute un effet de rebond visuel et gère le clic sur n'importe quelle vue
@SuppressLint("ClickableViewAccessibility")
fun scaleBtnOnClick(
    button: View, // L'élément visuel à animer
    activity: AppCompatActivity? = null, // L'écran actuel pour la navigation
    destination: Class<*>? = null, // La destination si le bouton doit changer d'écran
    action: (() -> Unit)? = null // L'action personnalisée à exécuter au clic
) {
    button.setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { // Se déclenche dès que le doigt touche l'écran
                ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.1f).apply { // Grossit le bouton de 10% en largeur
                    duration = 50 // L'animation est ultra rapide pour une sensation de réactivité
                    start()
                }
                ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.1f).apply { // Grossit le bouton de 10% en hauteur
                    duration = 50
                    start()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { // Se déclenche quand le doigt quitte l'écran
                ObjectAnimator.ofFloat(v, "scaleX", v.scaleX, 1f).apply { // Redonne la taille normale en largeur
                    duration = 150 // Le retour à la normale est plus lent pour un effet élastique
                    start()
                }
                ObjectAnimator.ofFloat(v, "scaleY", v.scaleY, 1f).apply { // Redonne la taille normale en hauteur
                    duration = 150
                    start()
                }

                if (event.action == MotionEvent.ACTION_UP) { // Vérifie que le clic est validé et non annulé
                    action?.invoke() // Exécute l'action de code si elle existe

                    activity?.let { act -> // Si une activité et une destination sont fournies
                        destination?.let { dest ->
                            val intent = Intent(act, dest) // Prépare le passage à l'écran suivant
                            act.startActivity(intent)
                            act.overridePendingTransition( // Applique le fondu au noir standard du jeu
                                R.anim.fade_from_black,
                                R.anim.fade_to_black
                            )
                        }
                    }
                }
            }
        }
        false // Renvoie faux pour laisser le système gérer les autres événements de clic classiques
    }
}