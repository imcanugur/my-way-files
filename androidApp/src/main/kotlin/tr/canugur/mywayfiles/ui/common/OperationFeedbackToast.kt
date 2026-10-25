package tr.canugur.mywayfiles.ui.common

import android.content.Context
import android.widget.Toast
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.explorer.FileOperation
import tr.canugur.mywayfiles.ui.explorer.OperationFeedback

fun Context.showOperationFeedback(feedback: OperationFeedback) {
    val label = getString(
        when (feedback.operation) {
            FileOperation.COPY -> R.string.operation_copy
            FileOperation.CUT -> R.string.operation_cut
            FileOperation.PASTE -> R.string.operation_paste
            FileOperation.RENAME -> R.string.operation_rename
            FileOperation.MOVE_TO_TRASH -> R.string.operation_move_to_trash
            FileOperation.DELETE, FileOperation.PERMANENT_DELETE -> R.string.operation_delete
            FileOperation.CREATE_FOLDER -> R.string.operation_create_folder
            FileOperation.MOVE_MEDIA -> R.string.operation_move
            FileOperation.RESTORE -> R.string.operation_restore
            FileOperation.EMPTY_TRASH -> R.string.operation_empty_trash
            FileOperation.ADD_FAVORITE -> R.string.operation_add_favorite
            FileOperation.REMOVE_FAVORITE -> R.string.operation_remove_favorite
        }
    )
    val message = when {
        feedback.failed == 0 && feedback.succeeded > 1 ->
            getString(R.string.operation_completed_count, label, feedback.succeeded)
        feedback.failed == 0 -> getString(R.string.operation_completed, label)
        feedback.succeeded > 0 -> getString(
            R.string.operation_partial, label, feedback.succeeded, feedback.failed
        )
        else -> getString(R.string.operation_failed, label)
    }
    val withDetail = feedback.detail?.takeIf { feedback.failed > 0 }?.let { "$message. $it" } ?: message
    Toast.makeText(this, withDetail, if (feedback.failed > 0) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}
