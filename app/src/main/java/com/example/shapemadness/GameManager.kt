package com.example.shapemadness

import android.annotation.SuppressLint
import android.view.View

// Cerveau global du jeu qui garde en mémoire toutes les statistiques importantes
@SuppressLint("StaticFieldLeak") // Empêche le compilateur de paniquer à cause de la vue stockée en mémoire globale
object GameManager {

    var playerCanon: View? = null // Garde une trace du vaisseau du joueur pour y accéder depuis n'importe où

    var invertRotation: Boolean = false // Option des paramètres pour inverser le sens de visée du gyroscope

    var diamondsCount = 0 // Le portefeuille global qui contient tout l'argent récolté pendant la partie

    var playerDamage = 10 // La force de base de chaque projectile avant les modificateurs de canon
    var fireRate = 500L // Le délai en millisecondes entre chaque tir de base
    var ballVelocity = 15f // La vitesse de déplacement par défaut des tirs à l'écran

    var dmgUpgradeBought = 0 // Compte combien de fois l'amélioration de dégâts a été achetée
    var spdUpgradeBought = 0 // Compte combien de fois la vitesse des balles a été augmentée
    var cdrUpgradeBought = 0 // Compte combien de fois la cadence de tir a été améliorée au magasin

}