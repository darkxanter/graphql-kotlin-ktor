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
        input: ArticleInput,
        dfe: DataFetchingEnvironment,
    ): ArticleDto {
        return repository.addArticle(input.title, input.content, dfe.graphQlContext.user)
    }
}

data class ArticleInput(
    val title: String,
    val content: String,
)
