/* eslint-disable */
import type { TypedDocumentNode as DocumentNode } from '@graphql-typed-document-node/core';
export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: string;
  String: string;
  Boolean: boolean;
  Int: number;
  Float: number;
  /** An RFC-3339 compliant DateTime Scalar */
  DateTime: string;
  /** A 64-bit signed integer */
  Long: BigInt;
};

/** Article */
export type Article = {
  __typename?: 'Article';
  author: User;
  authorId: Scalars['Long'];
  content: Scalars['String'];
  created: Scalars['DateTime'];
  id: Scalars['Long'];
  title: Scalars['String'];
};

export type ArticleInput = {
  content: Scalars['String'];
  title: Scalars['String'];
};

export type Mutation = {
  __typename?: 'Mutation';
  /** Add new article */
  addArticle: Article;
};


export type MutationAddArticleArgs = {
  input: ArticleInput;
};

export type Query = {
  __typename?: 'Query';
  /** Hello example with auth context for admin users only */
  adminHelloWithContext: Scalars['String'];
  /** List of articles */
  articles: Array<Article>;
  /** Hello example */
  hello: Scalars['String'];
  /** Hello example with auth context */
  helloWithContext: Scalars['String'];
  /** List of users */
  users: Array<User>;
};


export type QueryArticlesArgs = {
  limit?: InputMaybe<Scalars['Int']>;
  sortOrder?: InputMaybe<SortOrder>;
};

export enum Role {
  Admin = 'Admin',
  Manager = 'Manager',
  User = 'User'
}

export enum SortOrder {
  Asc = 'ASC',
  AscNullsFirst = 'ASC_NULLS_FIRST',
  AscNullsLast = 'ASC_NULLS_LAST',
  Desc = 'DESC',
  DescNullsFirst = 'DESC_NULLS_FIRST',
  DescNullsLast = 'DESC_NULLS_LAST'
}

export type Subscription = {
  __typename?: 'Subscription';
  /** List of articles */
  articles: Array<Article>;
  /** Returns a random number every second */
  counter: Scalars['Int'];
  /** Returns a random number every second */
  counter2: Scalars['Int'];
  /** Returns a random number every second, errors if even */
  counterWithError: Scalars['Int'];
  /** Returns a delayed value */
  delayedValue: Scalars['Int'];
  /** Returns stream of errors */
  flowOfErrors?: Maybe<Scalars['String']>;
  /** Returns a single value */
  singleValueSubscription: Scalars['Int'];
  /** Returns one value then an error */
  singleValueThenError: Scalars['Int'];
};


export type SubscriptionArticlesArgs = {
  limit?: InputMaybe<Scalars['Int']>;
  sortOrder?: InputMaybe<SortOrder>;
};


export type SubscriptionCounterArgs = {
  limit?: InputMaybe<Scalars['Int']>;
};


export type SubscriptionCounter2Args = {
  limit?: InputMaybe<Scalars['Int']>;
};


export type SubscriptionDelayedValueArgs = {
  seconds: Scalars['Int'];
};

/** User account */
export type User = {
  __typename?: 'User';
  id: Scalars['Long'];
  /** Display name */
  name: Scalars['String'];
  /** User role */
  role: Role;
  username: Scalars['String'];
};

export type NewArticleMutationVariables = Exact<{
  title: Scalars['String'];
  content: Scalars['String'];
}>;


export type NewArticleMutation = { __typename?: 'Mutation', addArticle: { __typename?: 'Article', id: BigInt, created: string, title: string } };

export type ArticleBriefFragment = { __typename?: 'Article', id: BigInt, title: string, created: string } & { ' $fragmentName'?: 'ArticleBriefFragment' };

export type LastArticlesQueryVariables = Exact<{ [key: string]: never; }>;


export type LastArticlesQuery = { __typename?: 'Query', articles: Array<(
    { __typename?: 'Article' }
    & { ' $fragmentRefs'?: { 'ArticleBriefFragment': ArticleBriefFragment } }
  )> };

export type LastArticlesSubscriptionSubscriptionVariables = Exact<{ [key: string]: never; }>;


export type LastArticlesSubscriptionSubscription = { __typename?: 'Subscription', articles: Array<(
    { __typename?: 'Article' }
    & { ' $fragmentRefs'?: { 'ArticleBriefFragment': ArticleBriefFragment } }
  )> };

export const ArticleBriefFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"ArticleBrief"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Article"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"title"}},{"kind":"Field","name":{"kind":"Name","value":"created"}}]}}]} as unknown as DocumentNode<ArticleBriefFragment, unknown>;
export const NewArticleDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"NewArticle"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"title"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"content"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"addArticle"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"input"},"value":{"kind":"ObjectValue","fields":[{"kind":"ObjectField","name":{"kind":"Name","value":"title"},"value":{"kind":"Variable","name":{"kind":"Name","value":"title"}}},{"kind":"ObjectField","name":{"kind":"Name","value":"content"},"value":{"kind":"Variable","name":{"kind":"Name","value":"content"}}}]}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"created"}},{"kind":"Field","name":{"kind":"Name","value":"title"}}]}}]}}]} as unknown as DocumentNode<NewArticleMutation, NewArticleMutationVariables>;
export const LastArticlesDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"LastArticles"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"articles"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"sortOrder"},"value":{"kind":"EnumValue","value":"DESC"}},{"kind":"Argument","name":{"kind":"Name","value":"limit"},"value":{"kind":"IntValue","value":"5"}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"ArticleBrief"}}]}}]}},...ArticleBriefFragmentDoc.definitions]} as unknown as DocumentNode<LastArticlesQuery, LastArticlesQueryVariables>;
export const LastArticlesSubscriptionDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"subscription","name":{"kind":"Name","value":"LastArticlesSubscription"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"articles"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"sortOrder"},"value":{"kind":"EnumValue","value":"DESC"}},{"kind":"Argument","name":{"kind":"Name","value":"limit"},"value":{"kind":"IntValue","value":"5"}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"ArticleBrief"}}]}}]}},...ArticleBriefFragmentDoc.definitions]} as unknown as DocumentNode<LastArticlesSubscriptionSubscription, LastArticlesSubscriptionSubscriptionVariables>;