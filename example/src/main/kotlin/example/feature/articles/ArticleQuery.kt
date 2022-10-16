package example.feature.articles

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query

class ArticleQuery(
    private val repository: ArticleRepository,
): Query {
    @GraphQLDescription("List of articles")
    fun articles(): List<ArticleDto> {
        return  repository.list()
    }
}
