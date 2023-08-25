package sh.dominick.hoojtracker.util

import org.jetbrains.exposed.dao.ColumnWithTransform
import org.jetbrains.exposed.sql.Column
import java.time.Instant

fun Column<Long>.transformInstant(): ColumnWithTransform<Long, Instant> =
    ColumnWithTransform(
        column = this,
        toReal = Instant::ofEpochSecond,
        toColumn = Instant::getEpochSecond
    )