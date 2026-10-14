package tr.canugur.mywayfiles.domain.usecase

import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.domain.model.SortOption
import tr.canugur.mywayfiles.domain.repository.FileRepository
import java.util.Locale

/**
 * UseCase to retrieve directory items with configurable sorting and hidden files inclusion.
 * Directories always stay pinned to the top.
 */
class GetDirectoryContentsUseCase(
    private val repository: FileRepository
) {
    suspend operator fun invoke(
        path: String,
        showHidden: Boolean = false,
        sortOption: SortOption = SortOption.NAME_ASC,
        forceRefresh: Boolean = false
    ): Result<List<FileItem>> {
        return repository.getDirectoryContents(path, showHidden, forceRefresh).map { items ->
            val comparator = when (sortOption) {
                SortOption.NAME_ASC -> compareBy<FileItem> { it.name.lowercase(Locale.ROOT) }
                SortOption.NAME_DESC -> compareByDescending<FileItem> { it.name.lowercase(Locale.ROOT) }
                SortOption.DATE_DESC -> compareByDescending<FileItem> { it.lastModifiedMs }
                SortOption.DATE_ASC -> compareBy<FileItem> { it.lastModifiedMs }
                SortOption.SIZE_DESC -> compareByDescending<FileItem> { it.sizeBytes }
                SortOption.SIZE_ASC -> compareBy<FileItem> { it.sizeBytes }
            }
            items.sortedWith(
                compareBy<FileItem> { !it.isDirectory }
                    .then(comparator)
            )
        }
    }
}
