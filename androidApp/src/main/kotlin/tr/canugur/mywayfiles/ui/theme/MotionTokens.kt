package tr.canugur.mywayfiles.ui.theme

/** Shared durations and travel distances for short, state-explaining UI motion. */
object MotionTokens {
    /** Capacity indicators use a slower interpolation when a measured value changes. */
    const val DURATION_DEFAULT_MS: Int = 400
    const val NAVIGATION_ENTER_MS: Int = 220
    const val NAVIGATION_EXIT_MS: Int = 180
    const val CONTENT_ENTER_MS: Int = 190
    const val CONTENT_EXIT_MS: Int = 150
    const val NAVIGATION_OFFSET_DP: Int = 24
}

/**
 * Storage capacity threshold rules.
 */
object StorageThresholds {
    const val WARNING_RATIO: Float = 0.80f
    const val CRITICAL_RATIO: Float = 0.90f
}
