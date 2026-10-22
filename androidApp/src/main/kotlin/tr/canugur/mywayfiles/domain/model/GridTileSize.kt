package tr.canugur.mywayfiles.domain.model

/** Minimum adaptive cell width used by the Explorer grid presentation. */
enum class GridTileSize(val minWidthDp: Int) {
    SMALL(120),
    MEDIUM(160),
    LARGE(208)
}
