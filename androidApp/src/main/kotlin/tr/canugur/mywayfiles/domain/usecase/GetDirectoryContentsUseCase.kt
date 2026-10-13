package tr.canugur.mywayfiles.domain.usecase

import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.domain.repository.FileRepository
import java.util.Locale

/**
 * UseCase to retrieve directory items, strictly sorted with directories first (A-Z),
 * followed by files (A-Z).
 */
class GetDirectoryContentsUseCase(
    private val repository: FileRepository
) {
    suspend operator fun invoke(path: String): Result<List<FileItem>> {
        return repository.getDirectoryContents(path).map { items ->
            items.sortedWith(
                compareBy<FileItem> { !it.isDirectory }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
        }
    }
}
