package tr.canugur.mywayfiles.data.preferences

import android.content.Context
import tr.canugur.mywayfiles.domain.model.FileViewMode
import tr.canugur.mywayfiles.domain.model.GridTileSize
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

    var viewMode: FileViewMode
        get() = runCatching {
            FileViewMode.valueOf(prefs.getString(KEY_VIEW_MODE, FileViewMode.LIST.name) ?: FileViewMode.LIST.name)
        }.getOrDefault(FileViewMode.LIST)
        set(value) = prefs.edit().putString(KEY_VIEW_MODE, value.name).apply()

    var gridTileSize: GridTileSize
        get() = runCatching {
            GridTileSize.valueOf(
                prefs.getString(KEY_GRID_TILE_SIZE, GridTileSize.MEDIUM.name) ?: GridTileSize.MEDIUM.name
            )
        }.getOrDefault(GridTileSize.MEDIUM)
        set(value) = prefs.edit().putString(KEY_GRID_TILE_SIZE, value.name).apply()

    companion object {
        private const val PREFS_NAME = "myway_explorer_prefs"
        private const val KEY_SHOW_HIDDEN_FILES = "key_show_hidden_files"
        private const val KEY_SORT_OPTION = "key_sort_option"
        private const val KEY_VIEW_MODE = "key_view_mode"
        private const val KEY_GRID_TILE_SIZE = "key_grid_tile_size"
    }
}
