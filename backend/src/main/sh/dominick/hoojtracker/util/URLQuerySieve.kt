package sh.dominick.hoojtracker.util

import io.javalin.http.Context
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like

class Filter(val query: Op<Boolean>, val limit: Int, val offset: Long = 0) {
    data class Parameter<TCol>(
        val name: String,
        val column: Column<TCol>,
        val columnClass: Class<TCol>
    )

    enum class Method {
        LIKE, EQ, LT, GT,
    }

    enum class Match(val joinQuery: (Op<Boolean>, Op<Boolean>) -> (Op<Boolean>)) {
        ALL({ a, b -> a and b }),
        ANY({ a, b -> a or b })
    }

    companion object {
        fun of(
            ctx: Context,
            filters: Set<Parameter<*>>,
            maxLimit: Int,
            defaultLimit: Int = maxLimit): Filter {
            val limit = (ctx.queryParam("limit")?.toIntOrNull() ?: defaultLimit).coerceAtMost(maxLimit)
            val offset = ctx.queryParam("offset")?.toLongOrNull() ?: 0

            val matchType = when(ctx.queryParam("match")?.lowercase()) {
                "all" -> Match.ALL
                "any" -> Match.ANY
                else -> Match.ALL
            }

/*            val joinQuery: (Op<Boolean>, Op<Boolean>) -> (Op<Boolean>) = when (ctx.queryParam("match")?.lowercase()) {
                "all" -> { a, b -> a and b }
                "any" -> { a, b -> a or b }

                else -> { a, b -> a and b }
            }*/

            var query: Op<Boolean>
                = if (matchType == Match.ANY) Op.FALSE else Op.TRUE

            for (filter in filters) {
                val parameter = ctx.queryParam("f:${filter.name}") ?: continue

                if (parameter.isBlank())
                    continue

                var method = when (filter.columnClass) {
                    String::class.java -> Method.LIKE
                    else -> Method.EQ
                }

                var value = parameter

                for (possible in Method.values()) {
                    if (parameter.startsWith(possible.name + ":", ignoreCase = true)) {
                        method = possible
                        value = parameter.substring((possible.name + ":").length)
                    }
                }

                val addon = when (method) {
                    Method.LIKE -> when (filter.columnClass) {
                        String::class.java -> (filter.column as Column<String>) like "%${value}%"
                        else -> Op.TRUE
                    }
                    Method.EQ -> when (filter.columnClass) {
                        Int::class.java -> (filter.column as Column<Int>) eq value.toInt()
                        Long::class.java -> (filter.column as Column<Long> eq value.toLong())
                        else -> Op.TRUE
                    }
                    Method.LT -> when (filter.columnClass) {
                        Int::class.java -> (filter.column as Column<Int>) lessEq value.toInt()
                        Long::class.java -> (filter.column as Column<Long> lessEq value.toLong())
                        else -> Op.TRUE
                    }
                    Method.GT -> when (filter.columnClass) {
                        Int::class.java -> (filter.column as Column<Int>) greaterEq  value.toInt()
                        Long::class.java -> (filter.column as Column<Long> greaterEq value.toLong())
                        else -> Op.TRUE
                    }
                }

                query = matchType.joinQuery(query, addon)
            }

            return Filter(query, limit, offset)
        }
    }
}

class Sort(val column: Column<*>, val order: SortOrder) {
    companion object {
        fun of(ctx: Context, sorts: Set<Pair<String, Column<*>>>, default: Pair<Column<*>, SortOrder>): Sort? {
            val parameterName = ctx.queryParam("sortBy")
                ?: return Sort(default.first, default.second)

            val parameterSortOrder = ctx.queryParam("sort")
                ?: "desc"

            val sortOrder = when(parameterSortOrder.lowercase()) {
                "asc" -> SortOrder.ASC
                "desc" -> SortOrder.DESC
                else -> SortOrder.ASC
            }

            for (sort in sorts) {
                val name = sort.first
                val column = sort.second

                if (name.equals(parameterName, ignoreCase = true))
                    return Sort(column, sortOrder)
            }

            return null
        }
    }
}

data class SieveResult<T>(val results: List<T>, val totalRecords: Long, val limit: Int, val offset: Long)

fun <T : Entity<*>, R> sieve(
    companion: EntityClass<*, T>,
    filter: Filter = Filter(Op.TRUE, 25, 0),
    sort: Sort? = null,
    queryModifier: (SizedIterable<T>) -> SizedIterable<T> = { it },
    resultModifier: (T) -> R = { it as R }
): SieveResult<R> {
    val totalRecords = companion.count(filter.query)

    val results = companion.find { filter.query }
        .limit(filter.limit, filter.offset)
        .also {
            if (sort != null)
                it.orderBy(sort.column to sort.order)
        }
        .let { queryModifier(it) }

    return SieveResult(
        results.map { resultModifier(it) },
        totalRecords,
        filter.limit,
        filter.offset
    )
}