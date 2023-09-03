package sh.dominick.hoojtracker.util

import io.javalin.http.Context
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.sql.*

class Filter(val query: Op<Boolean>, val limit: Int, val offset: Long = 0) {
    data class Parameter<TCol>(
        val column: Column<TCol>,
        val name: String = column.name
    )

    enum class Method(val query: (Column<*>, String) -> Op<Boolean>, val aliases: Set<String> = setOf()) {
        NOT_LIKE({ col, v ->
            val pattern = LikePattern(v)
            LikeEscapeOp(col, stringParam(pattern.pattern), false, pattern.escapeChar)
        }, setOf("!like")),
        LIKE({ col, v ->
            val pattern = LikePattern(v)
            LikeEscapeOp(col, stringParam(pattern.pattern), true, pattern.escapeChar)
        }),

        NOT_EQ({ col, v ->
            NeqOp(col, QueryParameter(v, col.columnType))
        }, setOf("!eq", "neq")),
        EQ({ col, v ->
            EqOp(col, QueryParameter(v, col.columnType))
        }),

        LT({ col, v -> LessOp(col, QueryParameter(v, col.columnType))}),
        LTE({ col, v -> LessEqOp(col, QueryParameter(v, col.columnType))}),

        GT({ col, v -> GreaterOp(col, QueryParameter(v, col.columnType))}),
        GTE({ col, v -> GreaterEqOp(col, QueryParameter(v, col.columnType))}),
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
            defaultLimit: Int = maxLimit,
            queryModifier: (Op<Boolean>) -> Op<Boolean> = { it }
        ): Filter {
            val limit = (ctx.queryParam("limit")?.toIntOrNull() ?: defaultLimit).coerceAtMost(maxLimit)
            val offset = ctx.queryParam("offset")?.toLongOrNull() ?: 0

            val matchType = when(ctx.queryParam("match")?.lowercase()) {
                "all" -> Match.ALL
                "any" -> Match.ANY
                else -> Match.ALL
            }

            val queries: MutableList<Op<Boolean>> = mutableListOf()

            for (filter in filters) {
                val parameter = ctx.queryParam("f:${filter.name}") ?: continue

                if (parameter.isBlank())
                    continue

                var method = Method.EQ
                var value = parameter

                for (possible in Method.values()) {
                    val aliases = possible.aliases.plus(possible.name)

                    for (alias in aliases) {
                        if (parameter.startsWith("$alias:", ignoreCase = true)) {
                            method = possible
                            value = parameter.substring((possible.name + ":").length)
                        }
                    }
                }

                val addon = method.query(filter.column, value)
                queries += addon
            }

            if (queries.isEmpty())
                return Filter(queryModifier(Op.TRUE), limit, offset)

            var query = queries[0]
            for (i in 1 until queries.size)
                query = matchType.joinQuery(query, queries[i])

            return Filter(queryModifier(query), limit, offset)
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