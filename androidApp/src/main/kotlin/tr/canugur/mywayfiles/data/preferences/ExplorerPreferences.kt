package tr.canugur.mywayfiles.data.preferences

import android.content.Context
import tr.canugur.mywayfiles.domain.model.SortOption

/**
 * Enterprise persistent preferences manager for Explorer settings (hidden files, sorting).
 * Preserves user configuration across app restarts and reboots.
 */
class ExplorerPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var showHiddenFiles: Boolean
        get() = prefs.getBoolean(KEY_SHOW_HIDDEN_FILES, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_HIDDEN_FILES, value).apply()

    var sortOption: SortOption
        get() {
            val name = prefs.getString(KEY_SORT_OPTION, SortOption.NAME_ASC.name) ?: SortOption.NAME_ASC.name
            return try {
                SortOption.valueOf(name)
            } catch (_: Exception) {
                SortOption.NAME_ASC
            }
        }
        set(value) = prefs.edit().putString(KEY_SORT_OPTION, value.name).apply()

    companion object {
        private const val PREFS_NAME = "myway_explorer_prefs"
        private const val KEY_SHOW_HIDDEN_FILES = "key_show_hidden_files"
        private const val KEY_SORT_OPTION = "key_sort_option"
    }
}
