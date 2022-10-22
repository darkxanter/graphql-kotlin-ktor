package example.feature.articles

import example.feature.users.User
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class ArticleRepository {
    private val articlesChanged = MutableSharedFlow<ArticleDto>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun list(
        sortOrder: SortOrder = SortOrder.ASC,
        limit: Int? = null,
    ): List<ArticleDto> {
        return transaction {
            ArticleTable
                .selectAll()
                .orderBy(ArticleTable.created, sortOrder)
                .also { query ->
                    limit?.let {
                        query.limit(limit)
                    }
                }
                .map {
                    it.toArticle().toDto()
                }
        }
    }

    fun getById(id: Long): ArticleDto {
        return transaction {
            ArticleTable.select {
                ArticleTable.id eq id
            }.single().toArticle().toDto()
        }
    }

    fun listFlow(
        sortOrder: SortOrder = SortOrder.DESC,
        limit: Int? = null,
    ): Flow<List<ArticleDto>> {
        fun getArticles() = list(
            sortOrder = sortOrder,
            limit = limit,
        )
        return channelFlow {
            send(getArticles())
            articlesChanged.collect {
                send(getArticles())
            }
        }.distinctUntilChanged()
    }

    fun addArticle(title: String, content: String, user: User): ArticleDto {
        return transaction {
            val id = ArticleTable.insertAndGetId {
                it[authorId] = user.id
                it[ArticleTable.title] = title
                it[ArticleTable.content] = content
            }
            getById(id.value)
        }.also {
            articlesChanged.tryEmit(it)
        }
    }
}
