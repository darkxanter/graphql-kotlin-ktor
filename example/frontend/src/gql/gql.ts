/* eslint-disable */
import * as types from './graphql';
import type { TypedDocumentNode as DocumentNode } from '@graphql-typed-document-node/core';

const documents = {
    "mutation NewArticle($title: String!, $content: String!) {\n  addArticle(input: {title: $title, content: $content}) {\n    id\n    created\n    title\n  }\n}": types.NewArticleDocument,
    "\n  fragment ArticleBrief on Article {\n    id\n    title\n    created\n  }\n": types.ArticleBriefFragmentDoc,
    "\n  query LastArticles {\n    articles(sortOrder: DESC, limit: 5) {\n      ...ArticleBrief\n    }\n  }\n": types.LastArticlesDocument,
    "\n  subscription LastArticlesSubscription {\n    articles(sortOrder: DESC, limit: 5) {\n      ...ArticleBrief\n    }\n  }\n": types.LastArticlesSubscriptionDocument,
};

export function graphql(source: "mutation NewArticle($title: String!, $content: String!) {\n  addArticle(input: {title: $title, content: $content}) {\n    id\n    created\n    title\n  }\n}"): (typeof documents)["mutation NewArticle($title: String!, $content: String!) {\n  addArticle(input: {title: $title, content: $content}) {\n    id\n    created\n    title\n  }\n}"];
export function graphql(source: "\n  fragment ArticleBrief on Article {\n    id\n    title\n    created\n  }\n"): (typeof documents)["\n  fragment ArticleBrief on Article {\n    id\n    title\n    created\n  }\n"];
export function graphql(source: "\n  query LastArticles {\n    articles(sortOrder: DESC, limit: 5) {\n      ...ArticleBrief\n    }\n  }\n"): (typeof documents)["\n  query LastArticles {\n    articles(sortOrder: DESC, limit: 5) {\n      ...ArticleBrief\n    }\n  }\n"];
export function graphql(source: "\n  subscription LastArticlesSubscription {\n    articles(sortOrder: DESC, limit: 5) {\n      ...ArticleBrief\n    }\n  }\n"): (typeof documents)["\n  subscription LastArticlesSubscription {\n    articles(sortOrder: DESC, limit: 5) {\n      ...ArticleBrief\n    }\n  }\n"];

export function graphql(source: string): unknown;
export function graphql(source: string) {
  return (documents as any)[source] ?? {};
}

export type DocumentType<TDocumentNode extends DocumentNode<any, any>> = TDocumentNode extends DocumentNode<  infer TType,  any>  ? TType  : never;