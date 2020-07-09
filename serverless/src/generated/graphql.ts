import gql from 'graphql-tag';
export type Maybe<T> = T | null;
export type Exact<T extends { [key: string]: any }> = { [K in keyof T]: T[K] };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: string;
  String: string;
  Boolean: boolean;
  Int: number;
  Float: number;
  Unit: any;
};

export type BookZio = {
  __typename?: 'BookZIO';
  id: Scalars['ID'];
  title: Scalars['String'];
  url: Scalars['String'];
  authors: Array<Scalars['String']>;
  quotes?: Maybe<Array<QuoteZio>>;
  userQuotes?: Maybe<Array<QuoteZio>>;
};


export type BookZioUserQuotesArgs = {
  value: Scalars['ID'];
};

export type MutationsZio = {
  __typename?: 'MutationsZIO';
  importKindleBookQuotes?: Maybe<Scalars['Unit']>;
};


export type MutationsZioImportKindleBookQuotesArgs = {
  twitterUserId: Scalars['ID'];
};

export type QueriesZio = {
  __typename?: 'QueriesZIO';
  user?: Maybe<UserZio>;
  book?: Maybe<BookZio>;
  users: Array<UserZio>;
};


export type QueriesZioUserArgs = {
  value: Scalars['ID'];
};


export type QueriesZioBookArgs = {
  value: Scalars['ID'];
};

export type QuoteZio = {
  __typename?: 'QuoteZIO';
  tweetId: Scalars['ID'];
  url: Scalars['String'];
  body: Scalars['String'];
  user?: Maybe<UserZio>;
  book?: Maybe<BookZio>;
};


export type UserZio = {
  __typename?: 'UserZIO';
  id: Scalars['ID'];
  username: Scalars['ID'];
  books?: Maybe<Array<BookZio>>;
  quotes?: Maybe<Array<QuoteZio>>;
  bookQuotes?: Maybe<Array<QuoteZio>>;
};


export type UserZioBookQuotesArgs = {
  value: Scalars['ID'];
};

export type GetUserIdsQueryVariables = Exact<{ [key: string]: never; }>;


export type GetUserIdsQuery = (
  { __typename?: 'QueriesZIO' }
  & { users: Array<(
    { __typename?: 'UserZIO' }
    & Pick<UserZio, 'id'>
  )> }
);


export const GetUserIds = gql`
    query GetUserIds {
  users {
    id
  }
}
    `;