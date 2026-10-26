package tr.canugur.mywayfiles.data.preferences

import android.content.Context

/** Persists app-wide appearance and interaction preferences. */
class AppSettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun darkThemeEnabled(systemDefault: Boolean): Boolean =
        preferences.getBoolean(KEY_DARK_THEME, systemDefault)

    fun setDarkThemeEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
    }

    fun confirmBeforeMovingToTrash(): Boolean =
        preferences.getBoolean(KEY_CONFIRM_TRASH, true)

    fun setConfirmBeforeMovingToTrash(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_CONFIRM_TRASH, enabled).apply()
    }

    fun permanentlyDeleteInsteadOfTrashing(): Boolean =
        preferences.getBoolean(KEY_PERMANENT_DELETE_MODE, false)

    fun setPermanentlyDeleteInsteadOfTrashing(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PERMANENT_DELETE_MODE, enabled).apply()
    }

    fun videoPlaybackModeName(): String =
        preferences.getString(KEY_VIDEO_PLAYBACK_MODE, DEFAULT_VIDEO_PLAYBACK_MODE)
            ?: DEFAULT_VIDEO_PLAYBACK_MODE

    fun setVideoPlaybackModeName(modeName: String) {
        preferences.edit().putString(KEY_VIDEO_PLAYBACK_MODE, modeName).apply()
    }

    fun autoHideViewerControls(): Boolean = preferences.getBoolean(KEY_AUTO_HIDE_VIEWER_CONTROLS, true)

    fun setAutoHideViewerControls(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_HIDE_VIEWER_CONTROLS, enabled).apply()
    }

    fun preferLastMoveTarget(): Boolean = preferences.getBoolean(KEY_PREFER_LAST_MOVE_TARGET, false)

    fun setPreferLastMoveTarget(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PREFER_LAST_MOVE_TARGET, enabled).apply()
    }

    fun directoryCacheHeapPercent(): Int = preferences.getInt(
        KEY_DIRECTORY_CACHE_HEAP_PERCENT,
        DEFAULT_DIRECTORY_CACHE_HEAP_PERCENT
    ).coerceIn(MIN_DIRECTORY_CACHE_HEAP_PERCENT, MAX_DIRECTORY_CACHE_HEAP_PERCENT)

    fun setDirectoryCacheHeapPercent(percent: Int) {
        preferences.edit()
            .putInt(KEY_DIRECTORY_CACHE_HEAP_PERCENT, percent.coerceIn(MIN_DIRECTORY_CACHE_HEAP_PERCENT, MAX_DIRECTORY_CACHE_HEAP_PERCENT))
            .apply()
    }

    fun useExtendedFolderCache(): Boolean = preferences.getBoolean(KEY_EXTENDED_FOLDER_CACHE, false)

    fun setUseExtendedFolderCache(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_EXTENDED_FOLDER_CACHE, enabled).apply()
    }

    fun directoryScrollHistoryLimit(): Int = preferences.getInt(
        KEY_DIRECTORY_SCROLL_HISTORY_LIMIT,
        recommendedDirectoryScrollHistoryLimit()
    ).coerceIn(MIN_DIRECTORY_SCROLL_HISTORY_LIMIT, MAX_DIRECTORY_SCROLL_HISTORY_LIMIT)

    fun setDirectoryScrollHistoryLimit(limit: Int) {
        preferences.edit().putInt(
            KEY_DIRECTORY_SCROLL_HISTORY_LIMIT,
            limit.coerceIn(MIN_DIRECTORY_SCROLL_HISTORY_LIMIT, MAX_DIRECTORY_SCROLL_HISTORY_LIMIT)
        ).apply()
    }

    /** Recommends a small navigation-state budget based on both app heap and device RAM. */
    fun recommendedDirectoryScrollHistoryLimit(useExtendedHeap: Boolean = useExtendedFolderCache()): Int {
        val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val heapMb = if (useExtendedHeap) manager?.largeMemoryClass else manager?.memoryClass
        val appBudget = (heapMb ?: 128) / 2
        val memoryInfo = android.app.ActivityManager.MemoryInfo().also { manager?.getMemoryInfo(it) }
        val deviceBudget = if (manager != null) (memoryInfo.totalMem / (1024L * 1024L) / 32L).toInt() else 64
        return minOf(appBudget, deviceBudget).coerceIn(32, MAX_DIRECTORY_SCROLL_HISTORY_LIMIT)
    }

    companion object {
        const val MIN_DIRECTORY_CACHE_HEAP_PERCENT = 2
        const val DEFAULT_DIRECTORY_CACHE_HEAP_PERCENT = 4
        const val MAX_DIRECTORY_CACHE_HEAP_PERCENT = 12
        const val MIN_DIRECTORY_SCROLL_HISTORY_LIMIT = 0
        const val DEFAULT_DIRECTORY_SCROLL_HISTORY_LIMIT = 128
        const val MAX_DIRECTORY_SCROLL_HISTORY_LIMIT = 512
        const val PREFERENCES_NAME = "myway_app_settings"
        const val KEY_DARK_THEME = "dark_theme_enabled"
        const val KEY_CONFIRM_TRASH = "confirm_before_trash"
        const val KEY_PERMANENT_DELETE_MODE = "permanent_delete_mode"
        const val KEY_AUTO_HIDE_VIEWER_CONTROLS = "auto_hide_viewer_controls"
        const val KEY_VIDEO_PLAYBACK_MODE = "video_playback_mode"
        const val DEFAULT_VIDEO_PLAYBACK_MODE = "PLAY_IN_ORDER"
        const val KEY_PREFER_LAST_MOVE_TARGET = "prefer_last_move_target"
        private const val KEY_DIRECTORY_CACHE_HEAP_PERCENT = "directory_cache_heap_percent"
        private const val KEY_EXTENDED_FOLDER_CACHE = "extended_folder_cache"
        private const val KEY_DIRECTORY_SCROLL_HISTORY_LIMIT = "directory_scroll_history_limit"
    }
}
