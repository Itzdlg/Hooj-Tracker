package sh.dominick.hoojtracker.util

import org.jetbrains.exposed.dao.ColumnWithTransform
import org.jetbrains.exposed.sql.Column
import sh.dominick.hoojtracker.data.Account.Companion.transform
import java.time.Instant

fun Column<Long>.transformInstant(): ColumnWithTransform<Long, Instant> =
    this.transform({ it.toEpochMilli() }, { Instant.ofEpochMilli(it) })