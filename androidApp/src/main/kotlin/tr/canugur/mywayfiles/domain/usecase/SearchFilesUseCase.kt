package tr.canugur.mywayfiles.domain.usecase

import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.domain.model.SortOption
import tr.canugur.mywayfiles.domain.repository.FileRepository
import java.util.Locale

/** Searches the current storage location's accessible descendants. */
class SearchFilesUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(
        path: String,
        query: String,
        showHidden: Boolean,
        sortOption: SortOption,
        onResults: (List<FileItem>) -> Unit = {}
    ): Result<List<FileItem>> = repository.searchFiles(path, query, showHidden) { items ->
        onResults(sortItems(items, sortOption))
    }.map { items -> sortItems(items, sortOption) }

    private fun sortItems(items: List<FileItem>, sortOption: SortOption): List<FileItem> {
        val normalizedNames = if (sortOption == SortOption.NAME_ASC || sortOption == SortOption.NAME_DESC) {
            items.associate { it.id to it.name.lowercase(Locale.ROOT) }
        } else {
            emptyMap()
        }
        val comparator = when (sortOption) {
            SortOption.NAME_ASC -> compareBy<FileItem> { normalizedNames[it.id] }
            SortOption.NAME_DESC -> compareByDescending<FileItem> { normalizedNames[it.id] }
            SortOption.DATE_DESC -> compareByDescending<FileItem> { it.lastModifiedMs }
            SortOption.DATE_ASC -> compareBy<FileItem> { it.lastModifiedMs }
            SortOption.SIZE_DESC -> compareByDescending<FileItem> { it.sizeBytes }
            SortOption.SIZE_ASC -> compareBy<FileItem> { it.sizeBytes }
        }
        return items.sortedWith(compareBy<FileItem> { !it.isDirectory }.then(comparator))
    }
}
