package example.feature.articles

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import example.graphql.user
import graphql.schema.DataFetchingEnvironment

class ArticleMutation(
    private val repository: ArticleRepository,
) : Mutation {
    @GraphQLDescription("Add new article")
    fun addArticle(
        title: String,
        content: String,
        dfe: DataFetchingEnvironment,
    ): ArticleDto {
        return repository.addArticle(title, content, dfe.graphQlContext.user)
    }
}
