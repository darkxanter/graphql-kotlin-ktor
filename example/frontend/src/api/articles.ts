import { graphql } from 'src/gql';


export const ArticleBriefFragment = graphql(/* GraphQL */ `
    fragment ArticleBrief on Article {
      id
      title
      created
      author {
        id
        name
      }
    }
`);


export const ArticlesQuery = graphql(/* GraphQL */ `
    query Articles($limit: Int! = 5, $sortOrder: SortOrder! = DESC) {
      articles(sortOrder: $sortOrder, limit: $limit) {
        ...ArticleBrief
      }
    }
`);


export const LastArticlesSubscription = graphql(/* GraphQL */ `
    subscription LastArticles($limit: Int! = 5) {
      articles(limit: $limit) {
        ...ArticleBrief
      }
    }
`);


export const NewArticleMutation = graphql(/* GraphQL */ `
    mutation NewArticle($input: ArticleInput!) {
      addArticle(input: $input) {
        id
        created
      }
    }
`);
