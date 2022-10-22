package example.feature.articles

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import org.jetbrains.exposed.sql.SortOrder

class ArticleQuery(
    private val repository: ArticleRepository,
) : Query {
    @GraphQLDescription("List of articles")
    fun articles(
        sortOrder: SortOrder? = null,
        limit: Int? = null,
    ): List<ArticleDto> {
        return repository.list(
            sortOrder = sortOrder ?: SortOrder.DESC,
            limit = limit,
        )
    }
}
