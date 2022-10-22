package example.feature.articles

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Subscription
import kotlinx.coroutines.flow.Flow
import org.jetbrains.exposed.sql.SortOrder

class ArticleSubscription(
    private val repository: ArticleRepository,
) : Subscription {
    @GraphQLDescription("List of articles")
    fun articles(
        sortOrder: SortOrder? = null,
        limit: Int? = null,
    ): Flow<List<ArticleDto>> {
        return repository.listFlow(
            sortOrder = sortOrder ?: SortOrder.DESC,
            limit = limit,
        )
    }
}
