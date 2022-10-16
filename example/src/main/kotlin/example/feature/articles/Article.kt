package example.feature.articles

import java.time.OffsetDateTime

data class Article(
    val id: Long,
    val created: OffsetDateTime,
    val authorId: Long,
    val title: String,
    val content: String,
)
