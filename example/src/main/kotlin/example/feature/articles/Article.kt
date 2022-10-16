package example.feature.articles

import example.feature.users.User
import java.time.OffsetDateTime

data class Article(
    val id: Long,
    val created: OffsetDateTime,
    val author: User,
    val title: String,
    val content: String,
)
