package example.feature.articles

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLName
import example.feature.users.User
import example.feature.users.UserDataLoader
import example.graphql.getValueFromDataLoader
import graphql.schema.DataFetchingEnvironment
import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture

@GraphQLName("Article")
@GraphQLDescription("Article")
data class ArticleDto(
    val id: Long,
    val created: OffsetDateTime,
    val authorId: Long,
    val title: String,
    val content: String,
) {
    fun author(dfe: DataFetchingEnvironment): CompletableFuture<User> {
        return dfe.getValueFromDataLoader(UserDataLoader::class, authorId)
    }
}

fun Article.toDto() = ArticleDto(
    id = id,
    created = created,
    authorId = authorId,
    title = title,
    content = content,
)
