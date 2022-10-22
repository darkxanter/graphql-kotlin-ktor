import { graphql } from 'src/gql'

export const ArticleBriefFragment = graphql(/* GraphQL */ `
  fragment ArticleBrief on Article {
    id
    title
    created
  }
`);

export const LastArticlesQueryDocument = graphql(/* GraphQL */ `
  query LastArticles {
    articles(sortOrder: DESC, limit: 5) {
      ...ArticleBrief
    }
  }
`);


export const LastArticlesSubscriptionDocument = graphql(/* GraphQL */ `
  subscription LastArticlesSubscription {
    articles(sortOrder: DESC, limit: 5) {
      ...ArticleBrief
    }
  }
`);
