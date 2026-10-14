package tr.canugur.mywayfiles.data.cache

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tr.canugur.mywayfiles.domain.model.FileItem

/**
 * Enterprise SQLite database backing directory and file indexing.
 * Provides ACID durability, indexed B-Tree lookups, and survives thousands of app restarts.
 */
class FileSystemCacheDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "myway_file_cache.db"
        private const val DATABASE_VERSION = 3

        private const val TABLE_DIR_META = "dir_metadata"
        private const val TABLE_FILES = "cached_files"

        // Dir Meta columns
        private const val COL_META_PATH = "dir_path"
        private const val COL_META_DISK_MODIFIED = "disk_last_modified"
        private const val COL_META_SCANNED_AT = "scanned_at"

        // Cached Files columns
        private const val COL_FILE_ID = "id"
        private const val COL_FILE_DIR_PATH = "parent_dir"
        private const val COL_FILE_NAME = "name"
        private const val COL_FILE_PATH = "file_path"
        private const val COL_FILE_IS_DIR = "is_dir"
        private const val COL_FILE_SIZE = "size_bytes"
        private const val COL_FILE_TIME = "last_modified_ms"
        private const val COL_FILE_EXT = "extension"
        private const val COL_FILE_IS_HIDDEN = "is_hidden"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_DIR_META (
                $COL_META_PATH TEXT PRIMARY KEY,
                $COL_META_DISK_MODIFIED INTEGER NOT NULL,
                $COL_META_SCANNED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_FILES (
                $COL_FILE_ID TEXT PRIMARY KEY,
                $COL_FILE_DIR_PATH TEXT NOT NULL,
                $COL_FILE_NAME TEXT NOT NULL,
                $COL_FILE_PATH TEXT NOT NULL,
                $COL_FILE_IS_DIR INTEGER NOT NULL,
                $COL_FILE_SIZE INTEGER NOT NULL,
                $COL_FILE_TIME INTEGER NOT NULL,
                $COL_FILE_EXT TEXT NOT NULL,
                $COL_FILE_IS_HIDDEN INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_files_parent ON $TABLE_FILES ($COL_FILE_DIR_PATH)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FILES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DIR_META")
        onCreate(db)
    }

    suspend fun getDirLastModified(dirPath: String): Long? = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery(
            "SELECT $COL_META_DISK_MODIFIED FROM $TABLE_DIR_META WHERE $COL_META_PATH = ? LIMIT 1",
            arrayOf(dirPath)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }

    suspend fun getCachedItems(dirPath: String, showHidden: Boolean): List<FileItem>? = withContext(Dispatchers.IO) {
        val query = if (showHidden) {
            "SELECT * FROM $TABLE_FILES WHERE $COL_FILE_DIR_PATH = ?"
        } else {
            "SELECT * FROM $TABLE_FILES WHERE $COL_FILE_DIR_PATH = ? AND $COL_FILE_IS_HIDDEN = 0"
        }

        readableDatabase.rawQuery(query, arrayOf(dirPath)).use { cursor ->
            if (cursor.count == 0) {
                // Check if directory meta exists (it could be legitimately empty folder)
                val hasMeta = getDirLastModified(dirPath) != null
                return@withContext if (hasMeta) emptyList() else null
            }

            val items = ArrayList<FileItem>(cursor.count)
            val idIdx = cursor.getColumnIndexOrThrow(COL_FILE_ID)
            val nameIdx = cursor.getColumnIndexOrThrow(COL_FILE_NAME)
            val pathIdx = cursor.getColumnIndexOrThrow(COL_FILE_PATH)
            val isDirIdx = cursor.getColumnIndexOrThrow(COL_FILE_IS_DIR)
            val sizeIdx = cursor.getColumnIndexOrThrow(COL_FILE_SIZE)
            val timeIdx = cursor.getColumnIndexOrThrow(COL_FILE_TIME)
            val extIdx = cursor.getColumnIndexOrThrow(COL_FILE_EXT)
            val hiddenIdx = cursor.getColumnIndexOrThrow(COL_FILE_IS_HIDDEN)

            while (cursor.moveToNext()) {
                items.add(
                    FileItem(
                        id = cursor.getString(idIdx),
                        name = cursor.getString(nameIdx),
                        path = cursor.getString(pathIdx),
                        isDirectory = cursor.getInt(isDirIdx) == 1,
                        sizeBytes = cursor.getLong(sizeIdx),
                        lastModifiedMs = cursor.getLong(timeIdx),
                        extension = cursor.getString(extIdx),
                        childCount = null,
                        isHidden = cursor.getInt(hiddenIdx) == 1
                    )
                )
            }
            items
        }
    }

    suspend fun saveDirectory(dirPath: String, diskLastModified: Long, items: List<FileItem>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // 1. Update dir metadata
            val metaValues = ContentValues().apply {
                put(COL_META_PATH, dirPath)
                put(COL_META_DISK_MODIFIED, diskLastModified)
                put(COL_META_SCANNED_AT, System.currentTimeMillis())
            }
            db.insertWithOnConflict(TABLE_DIR_META, null, metaValues, SQLiteDatabase.CONFLICT_REPLACE)

            // 2. Remove old items for this directory
            db.delete(TABLE_FILES, "$COL_FILE_DIR_PATH = ?", arrayOf(dirPath))

            // 3. Batch insert new items
            val statement = db.compileStatement(
                """
                INSERT OR REPLACE INTO $TABLE_FILES 
                ($COL_FILE_ID, $COL_FILE_DIR_PATH, $COL_FILE_NAME, $COL_FILE_PATH, $COL_FILE_IS_DIR, $COL_FILE_SIZE, $COL_FILE_TIME, $COL_FILE_EXT, $COL_FILE_IS_HIDDEN)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            )
            try {
                for (item in items) {
                    statement.bindString(1, item.id)
                    statement.bindString(2, dirPath)
                    statement.bindString(3, item.name)
                    statement.bindString(4, item.path)
                    statement.bindLong(5, if (item.isDirectory) 1L else 0L)
                    statement.bindLong(6, item.sizeBytes)
                    statement.bindLong(7, item.lastModifiedMs)
                    statement.bindString(8, item.extension)
                    statement.bindLong(9, if (item.isHidden) 1L else 0L)
                    statement.executeInsert()
                }
            } finally {
                statement.close()
            }

            db.setTransactionSuccessful()
        } catch (_: Exception) {
        } finally {
            db.endTransaction()
        }
    }

    suspend fun invalidateDirectory(dirPath: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_DIR_META, "$COL_META_PATH = ?", arrayOf(dirPath))
            db.delete(TABLE_FILES, "$COL_FILE_DIR_PATH = ?", arrayOf(dirPath))
            db.setTransactionSuccessful()
        } catch (_: Exception) {
        } finally {
            db.endTransaction()
        }
    }
}
