package example.feature.articles

import example.feature.users.UserTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.time.ZoneId

object ArticleTable : LongIdTable("articles") {
    val title = varchar("title", 255)
    val content = text("content")
    val authorId = long("author_id").references(UserTable.id)
    val created = timestamp("created").clientDefault { Instant.now() }
}

fun ResultRow.toArticle() = Article(
    id = this[ArticleTable.id].value,
    created = this[ArticleTable.created].atZone(ZoneId.systemDefault()).toOffsetDateTime(),
    authorId = this[ArticleTable.authorId],
    title = this[ArticleTable.title],
    content = this[ArticleTable.content],
)
