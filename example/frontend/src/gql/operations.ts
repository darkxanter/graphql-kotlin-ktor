import * as Types from './graphql';
import gql from 'graphql-tag';
import * as Urql from '@urql/vue';
export type Omit<T, K extends keyof T> = Pick<T, Exclude<keyof T, K>>;
export const ArticleBriefFragmentDoc = gql`
    fragment ArticleBrief on Article {
  id
  title
  created
  author {
    id
    name
  }
}
    `;
export const ArticlesDocument = gql`
    query Articles($limit: Int! = 5, $sortOrder: SortOrder! = DESC) {
  articles(sortOrder: $sortOrder, limit: $limit) {
    ...ArticleBrief
  }
}
    ${ArticleBriefFragmentDoc}`;

export function useArticlesQuery(options: Omit<Urql.UseQueryArgs<never, Types.ArticlesQueryVariables>, 'query'> = {}) {
  return Urql.useQuery<Types.ArticlesQuery>({ query: ArticlesDocument, ...options });
};
export const LastArticlesDocument = gql`
    subscription LastArticles($limit: Int! = 5) {
  articles(limit: $limit) {
    ...ArticleBrief
  }
}
    ${ArticleBriefFragmentDoc}`;

export function useLastArticlesSubscription<R = Types.LastArticlesSubscription>(options: Omit<Urql.UseSubscriptionArgs<never, Types.LastArticlesSubscriptionVariables>, 'query'> = {}, handler?: Urql.SubscriptionHandlerArg<Types.LastArticlesSubscription, R>) {
  return Urql.useSubscription<Types.LastArticlesSubscription, R, Types.LastArticlesSubscriptionVariables>({ query: LastArticlesDocument, ...options }, handler);
};
export const NewArticleDocument = gql`
    mutation NewArticle($input: ArticleInput!) {
  addArticle(input: $input) {
    id
    created
  }
}
    `;

export function useNewArticleMutation() {
  return Urql.useMutation<Types.NewArticleMutation, Types.NewArticleMutationVariables>(NewArticleDocument);
};