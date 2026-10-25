package tr.canugur.mywayfiles.data.preferences

import android.content.Context

/** Persists app-wide appearance and interaction preferences. */
class AppSettingsStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

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

    fun autoHideViewerControls(): Boolean = preferences.getBoolean(KEY_AUTO_HIDE_VIEWER_CONTROLS, true)

    fun setAutoHideViewerControls(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_HIDE_VIEWER_CONTROLS, enabled).apply()
    }

    fun preferLastMoveTarget(): Boolean = preferences.getBoolean(KEY_PREFER_LAST_MOVE_TARGET, false)

    fun setPreferLastMoveTarget(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PREFER_LAST_MOVE_TARGET, enabled).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "myway_app_settings"
        const val KEY_DARK_THEME = "dark_theme_enabled"
        const val KEY_CONFIRM_TRASH = "confirm_before_trash"
        const val KEY_PERMANENT_DELETE_MODE = "permanent_delete_mode"
        const val KEY_AUTO_HIDE_VIEWER_CONTROLS = "auto_hide_viewer_controls"
        const val KEY_PREFER_LAST_MOVE_TARGET = "prefer_last_move_target"
    }
}
