package example.feature.articles

import example.feature.users.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.OffsetDateTime

class ArticleRepository {
    private val idSequence = iterator {
        var id = 1L
        while (true) {
            yield(id++)
        }
    }
    private val articles = MutableStateFlow(
        listOf(
            Article(
                id = idSequence.next(),
                created = OffsetDateTime.now(),
                authorId = 1,
                title = "Example title",
                content = "Example content"
            ),
            Article(
                id = idSequence.next(),
                created = OffsetDateTime.now(),
                authorId = 2,
                title = "Example title 2",
                content = "Example content 2"
            ),
            Article(
                id = idSequence.next(),
                created = OffsetDateTime.now(),
                authorId = 1,
                title = "Example title 3",
                content = "Example content 3"
            ),
        )
    )


    fun list() = articles.value.map { it.toDto() }

    fun listFlow(): Flow<List<ArticleDto>> {
        return articles.map { articles ->
            articles.map { it.toDto() }
        }
    }

    fun addArticle(title: String, content: String, user: User): ArticleDto {
        val article = Article(
            id = idSequence.next(),
            created = OffsetDateTime.now(),
            authorId = user.id,
            title = title,
            content = content,
        )
        articles.update { articles ->
            articles + article
        }
        return article.toDto()
    }
}
