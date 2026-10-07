import android.content.Context

object SaveManager {
    private const val PREFS_NAME = "GameSettings"
    private const val KEY_HIGH_SCORE = "high_score"
    private const val KEY_INVERT_ROTATION = "invertRotation"

    // --- GESTION DU RECORD ---
    fun checkAndSaveRecord(context: Context, currentWave: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val oldRecord = prefs.getInt(KEY_HIGH_SCORE, 1)

        if (currentWave > oldRecord) {
            prefs.edit().putInt(KEY_HIGH_SCORE, currentWave).apply()
            return true // Renvoie true si c'est un nouveau record
        }
        return false
    }

    fun getHighScore(context: android.content.Context): Int {
        return context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getInt(KEY_HIGH_SCORE, 1)
    }

    // --- GESTION DES PARAMÈTRES (INVERSION) ---
    fun saveInvertRotation(context: android.content.Context, isInverted: Boolean) {
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_INVERT_ROTATION, isInverted)
            .apply()
    }

    fun getInvertRotation(context: android.content.Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getBoolean(KEY_INVERT_ROTATION, false)
    }
}