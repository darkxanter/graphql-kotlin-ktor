package example.feature.articles

import com.github.darkxanter.kesp.annotation.ExposedTable
import com.github.darkxanter.kesp.annotation.Projection
import example.feature.users.UserTable
import example.sql.offsetDateTime
import org.jetbrains.exposed.dao.id.LongIdTable
import java.time.OffsetDateTime

@ExposedTable
@Projection(Article::class)
object ArticleTable : LongIdTable("articles") {
    val title = varchar("title", 255)
    val content = text("content")
    val authorId = long("author_id").references(UserTable.id)
    val created = offsetDateTime("created").clientDefault { OffsetDateTime.now() }
}
