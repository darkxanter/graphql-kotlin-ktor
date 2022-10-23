/* eslint-disable */
import * as types from './graphql';
import type { TypedDocumentNode as DocumentNode } from '@graphql-typed-document-node/core';

const documents = {
    "\n    fragment ArticleBrief on Article {\n      id\n      title\n      created\n      author {\n        id\n        name\n      }\n    }\n": types.ArticleBriefFragmentDoc,
    "\n    query Articles($limit: Int! = 5, $sortOrder: SortOrder! = DESC) {\n      articles(sortOrder: $sortOrder, limit: $limit) {\n        ...ArticleBrief\n      }\n    }\n": types.ArticlesDocument,
    "\n    subscription LastArticles($limit: Int! = 5) {\n      articles(limit: $limit) {\n        ...ArticleBrief\n      }\n    }\n": types.LastArticlesDocument,
    "\n    mutation NewArticle($input: ArticleInput!) {\n      addArticle(input: $input) {\n        id\n        created\n      }\n    }\n": types.NewArticleDocument,
};

export function graphql(source: "\n    fragment ArticleBrief on Article {\n      id\n      title\n      created\n      author {\n        id\n        name\n      }\n    }\n"): (typeof documents)["\n    fragment ArticleBrief on Article {\n      id\n      title\n      created\n      author {\n        id\n        name\n      }\n    }\n"];
export function graphql(source: "\n    query Articles($limit: Int! = 5, $sortOrder: SortOrder! = DESC) {\n      articles(sortOrder: $sortOrder, limit: $limit) {\n        ...ArticleBrief\n      }\n    }\n"): (typeof documents)["\n    query Articles($limit: Int! = 5, $sortOrder: SortOrder! = DESC) {\n      articles(sortOrder: $sortOrder, limit: $limit) {\n        ...ArticleBrief\n      }\n    }\n"];
export function graphql(source: "\n    subscription LastArticles($limit: Int! = 5) {\n      articles(limit: $limit) {\n        ...ArticleBrief\n      }\n    }\n"): (typeof documents)["\n    subscription LastArticles($limit: Int! = 5) {\n      articles(limit: $limit) {\n        ...ArticleBrief\n      }\n    }\n"];
export function graphql(source: "\n    mutation NewArticle($input: ArticleInput!) {\n      addArticle(input: $input) {\n        id\n        created\n      }\n    }\n"): (typeof documents)["\n    mutation NewArticle($input: ArticleInput!) {\n      addArticle(input: $input) {\n        id\n        created\n      }\n    }\n"];

export function graphql(source: string): unknown;
export function graphql(source: string) {
  return (documents as any)[source] ?? {};
}

export type DocumentType<TDocumentNode extends DocumentNode<any, any>> = TDocumentNode extends DocumentNode<  infer TType,  any>  ? TType  : never;