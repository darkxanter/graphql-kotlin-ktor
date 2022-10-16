package example.feature.articles

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Subscription
import kotlinx.coroutines.flow.Flow

class ArticleSubscription(
    private val repository: ArticleRepository,
): Subscription {
    @GraphQLDescription("List of articles")
    fun articles(): Flow<List<ArticleDto>> {
        return  repository.listFlow()
    }
}
