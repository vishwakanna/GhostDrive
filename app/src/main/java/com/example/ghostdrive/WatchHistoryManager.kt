package com.example.ghostdrive

import android.content.Context

/**
 * WatchHistoryManager
 *
 * What it does:
 *   - Saves the last playback position (in milliseconds) for every video you watch
 *   - When you open the same video again, ExoPlayer resumes from where you stopped
 *
 * How it works:
 *   - Uses Android SharedPreferences — a simple key-value store that persists on disk
 *   - Key   = the file path  (e.g. "/home/vishwa/Videos/movie.mp4")
 *   - Value = position in ms (e.g. 123456)
 *
 * SharedPreferences is like a tiny database built into Android.
 * You don't need SQLite or any external library for this kind of simple storage.
 */
object WatchHistoryManager {

    private const val PREFS_NAME     = "watch_history"
    private const val MAX_HISTORY    = 50   // keep only the 50 most recent entries

    // Save position for a file path
    fun savePosition(context: Context, filePath: String, positionMs: Long) {
        if (positionMs < 3000) return  // don't save if watched less than 3 seconds

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(filePath, positionMs).apply()

        // Trim old entries if we exceed MAX_HISTORY
        val all = prefs.all
        if (all.size > MAX_HISTORY) {
            val oldest = all.keys.first()
            prefs.edit().remove(oldest).apply()
        }
    }

    // Get saved position for a file (returns 0 if never watched)
    fun getPosition(context: Context, filePath: String): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(filePath, 0L)
    }

    // Get all watched files as a list (most recent first)
    fun getHistory(context: Context): List<Pair<String, Long>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all
            .filterValues { it is Long }
            .map { (path, pos) -> path to (pos as Long) }
            .sortedByDescending { it.second }
    }

    // Remove a single entry
    fun removeEntry(context: Context, filePath: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(filePath).apply()
    }

    // Clear all history
    fun clearAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
